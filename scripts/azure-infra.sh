#!/bin/bash
#
# Ajudante Familiar - infraestrutura completa na Azure (Azure CLI)
#
# Este mesmo script fica nos 3 repositórios (API-Funcoes, API-autentificador e RAG).
# Alterou esta cópia? Replique nos outros com scripts/sincronizar-infra.sh (veja o uso lá).
#
# Rode-o UMA vez, de qualquer um deles, para criar tudo:
#   - Grupo de recursos, Application Insights e plano do App Service (Linux, B1)
#   - RabbitMQ no Azure Container Instance (mensageria entre as APIs)
#   - Os 3 Web Apps (funções, autenticação e RAG), ligados ao Application Insights
# Recursos que já existem são mantidos: pode rodar de novo sem duplicar nada.
# Qualquer erro interrompe o script na hora, mostrando o comando que falhou.
#
# VERIFIQUE SE ESTA COM A ASSINATURA CORRETA
#
# az account show
# az account list -o table
# az account set --subscription "nome-da-assinatura-correta"
#
# Uso: bash scripts/azure-infra.sh

# -E: o trap de erro vale também dentro de funções | -e: para no primeiro erro
# -u: variável não definida é erro | pipefail: erro no meio de um pipe também conta
set -Eeuo pipefail
trap 'echo "ERRO na linha $LINENO, o comando falhou: $BASH_COMMAND" >&2' ERR

falhar() {
  echo "ERRO: $1" >&2
  exit 1
}


# Variáveis
RESOURCE_GROUP_NAME="gs-ajudantefamiliar"
APP_SERVICE_PLAN="ajudante-familiar-SP"
LOCATION="brazilsouth"
RUNTIME="JAVA:21-java21"
APP_INSIGHTS_NAME="ai-ajudante-familiar"

# Web Apps: cada nome é igual ao WEB_APP_NAME do azure-pipeline.yml do respectivo repositório
WEB_APPS=(
  "ajudante-familiar-funcoes"        # API de funções (porta local 8080)
  "ajudante-familiar-autenticacao"   # API de autenticação (porta local 8081)
  "ajudante-familiar-rag"            # API de RAG (porta local 8082)
)

# RabbitMQ (Azure Container Instance), usado pela mensageria entre as APIs
RABBITMQ_CONTAINER_NAME="rabbitmq-ajudante-familiar"
RABBITMQ_DNS_LABEL="rabbitmq-ajudante-familiar"   # vira <label>.brazilsouth.azurecontainer.io; precisa ser único na região
RABBITMQ_IMAGE="rabbitmq:4-management"            # inclui o painel web de administração (porta 15672)
RABBITMQ_USER="ajudante"

# Conferir o login antes de tudo: sem ele, as checagens abaixo achariam que nada existe
ASSINATURA=$(az account show --query name --output tsv 2>/dev/null) \
  || falhar "Azure CLI sem login. Rode 'az login' e tente de novo."
echo "Assinatura em uso: $ASSINATURA"

# Senha do RabbitMQ: digitada aqui, não aparece na tela e não fica salva neste arquivo.
# Só é pedida se o container ainda não existir.
# Guarde-a: ela vai na variável secreta SPRING_RABBITMQ_PASSWORD das 3 pipelines.
RABBITMQ_PASSWORD=""
RABBITMQ_EXISTE=false
if az container show --name $RABBITMQ_CONTAINER_NAME --resource-group $RESOURCE_GROUP_NAME --output none 2>/dev/null; then
  RABBITMQ_EXISTE=true
else
  # -r: mantém as barras invertidas (\) da senha como foram digitadas
  read -r -s -p "Defina a senha do RabbitMQ (mínimo 12 caracteres): " RABBITMQ_PASSWORD \
    || falhar "Não foi possível ler a senha. Rode o script num terminal interativo."
  echo
  if [ ${#RABBITMQ_PASSWORD} -lt 12 ]; then
    falhar "Senha muito curta. A porta do RabbitMQ fica aberta para a internet: use 12 caracteres ou mais."
  fi
fi

# Registrar os Serviços utilizados
az provider register --namespace Microsoft.Web
az provider register --namespace Microsoft.Insights
az provider register --namespace Microsoft.OperationalInsights
az provider register --namespace Microsoft.ServiceLinker
az provider register --namespace Microsoft.ContainerInstance

# Registrar a extensão utilizada (--upgrade: não falha se ela já estiver instalada)
az extension add --name application-insights --upgrade --only-show-errors

# Criação do Grupo de Recursos
az group create \
  --name $RESOURCE_GROUP_NAME \
  --location "$LOCATION"

# Criar Application Insights (um só para as 3 APIs)
az monitor app-insights component create \
  --app $APP_INSIGHTS_NAME \
  --location "$LOCATION" \
  --resource-group $RESOURCE_GROUP_NAME \
  --application-type web

# Criar o Plano de Serviço (as 3 APIs rodam no mesmo plano B1)
az appservice plan create \
  --name $APP_SERVICE_PLAN \
  --resource-group $RESOURCE_GROUP_NAME \
  --location "$LOCATION" \
  --sku B1 \
  --is-linux

# Recuperar a String de Conexão do Application Insights
CONNECTION_STRING=$(az monitor app-insights component show \
  --app $APP_INSIGHTS_NAME \
  --resource-group $RESOURCE_GROUP_NAME \
  --query connectionString \
  --output tsv)
[ -n "$CONNECTION_STRING" ] \
  || falhar "A connection string do Application Insights $APP_INSIGHTS_NAME veio vazia."

# Criar e configurar os 3 Web Apps
for WEB_APP_NAME in "${WEB_APPS[@]}"; do
  echo
  echo "==> Web App $WEB_APP_NAME"

  # Criar o Serviço de Aplicativo, se ainda não existir
  if az webapp show --name $WEB_APP_NAME --resource-group $RESOURCE_GROUP_NAME --output none 2>/dev/null; then
    echo "Web App $WEB_APP_NAME já existe: mantido."
  else
    az webapp create \
      --name $WEB_APP_NAME \
      --resource-group $RESOURCE_GROUP_NAME \
      --plan $APP_SERVICE_PLAN \
      --runtime "$RUNTIME" \
      || falhar "Não foi possível criar o Web App $WEB_APP_NAME. O nome precisa ser único em toda a Azure: se já estiver em uso por outra pessoa, troque-o aqui e no WEB_APP_NAME do azure-pipeline.yml do repositório."
  fi

  # Manter o app sempre ligado (disponível no B1): evita a demora de subir o Spring Boot a cada acesso
  az webapp config set \
    --name "$WEB_APP_NAME" \
    --resource-group "$RESOURCE_GROUP_NAME" \
    --always-on true \
    --output none

  # Application Insights: para Java no Linux bastam estas 2 variáveis. Com elas, o App Service
  # liga o agente Java (~3), que envia a telemetria para a connection string (sem SDK no pom.xml).
  # Alterar as variáveis já reinicia o Web App, então não é preciso um "az webapp restart" à parte.
  az webapp config appsettings set \
    --name "$WEB_APP_NAME" \
    --resource-group "$RESOURCE_GROUP_NAME" \
    --settings \
      APPLICATIONINSIGHTS_CONNECTION_STRING="$CONNECTION_STRING" \
      ApplicationInsightsAgent_EXTENSION_VERSION="~3" \
    --output none
  # Banco, RabbitMQ, JWT_SEGREDO (e OPENAI_API_KEY, no RAG) NÃO ficam aqui: cada pipeline
  # (azure-pipeline.yml) grava essas variáveis no seu Web App a cada deploy, a partir das
  # Variables secretas do Azure DevOps. O JWT_SEGREDO precisa ser o mesmo nas 3 pipelines.

  echo "Web App $WEB_APP_NAME configurado (Always On e Application Insights)."
done

# Criar o RabbitMQ no Azure Container Instance
# 5672: porta AMQP usada pelas APIs. 15672: painel web (http://<host>:15672), mesmo usuário e senha.
# Sem volume: filas e mensagens se perdem se o container for recriado (suficiente para o projeto).
echo
if [ "$RABBITMQ_EXISTE" = true ]; then
  echo "==> RabbitMQ $RABBITMQ_CONTAINER_NAME já existe: mantido (a senha continua a mesma)."
else
  echo "==> RabbitMQ $RABBITMQ_CONTAINER_NAME"
  az container create \
    --name $RABBITMQ_CONTAINER_NAME \
    --resource-group $RESOURCE_GROUP_NAME \
    --location "$LOCATION" \
    --image $RABBITMQ_IMAGE \
    --os-type Linux \
    --cpu 1 \
    --memory 1.5 \
    --ip-address Public \
    --dns-name-label $RABBITMQ_DNS_LABEL \
    --ports 5672 15672 \
    --restart-policy Always \
    --environment-variables RABBITMQ_DEFAULT_USER="$RABBITMQ_USER" \
    --secure-environment-variables RABBITMQ_DEFAULT_PASS="$RABBITMQ_PASSWORD"
fi

# Endereço do RabbitMQ (valor de SPRING_RABBITMQ_HOST)
RABBITMQ_HOST=$(az container show \
  --name $RABBITMQ_CONTAINER_NAME \
  --resource-group $RESOURCE_GROUP_NAME \
  --query ipAddress.fqdn \
  --output tsv)
[ -n "$RABBITMQ_HOST" ] \
  || falhar "O container $RABBITMQ_CONTAINER_NAME existe, mas está sem endereço público (ipAddress.fqdn)."

echo
echo "=================================================================="
echo " Web Apps criados (confira o endereço em Domínio padrão no portal):"
for WEB_APP_NAME in "${WEB_APPS[@]}"; do
  HOST=$(az webapp show --name $WEB_APP_NAME --resource-group $RESOURCE_GROUP_NAME --query defaultHostName --output tsv)
  echo "   $WEB_APP_NAME -> https://$HOST/swagger-ui.html"
done
echo
echo " Valores do RabbitMQ para as Variables das 3 pipelines:"
echo "   SPRING_RABBITMQ_HOST     = $RABBITMQ_HOST"
echo "   SPRING_RABBITMQ_PORT     = 5672"
echo "   SPRING_RABBITMQ_USERNAME = $RABBITMQ_USER"
echo "   SPRING_RABBITMQ_PASSWORD = (a senha definida na criação; marque como secreta)"
echo " Painel de administração: http://$RABBITMQ_HOST:15672"
echo "=================================================================="
