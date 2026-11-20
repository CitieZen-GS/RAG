#!/bin/bash
#
# Ajudante Familiar - infraestrutura completa na Azure (Azure CLI)
#
# Este mesmo script fica nos 3 repositórios (API-Funcoes, API-autentificador e RAG).
# Alterou esta cópia? Replique nos outros com scripts/sincronizar-infra.sh (veja o uso lá).
#
# Rode-o UMA vez, de qualquer um deles, para criar tudo:
#   - Grupo de recursos, Application Insights e plano do App Service (Linux, B3)
#   - RabbitMQ no Azure Container Instance (mensageria entre as APIs)
#   - Banco Oracle Free no Azure Container Instance (compartilhado pelas 3 APIs)
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

# Usa a variável global RESOURCE_GROUP_NAME, definida logo abaixo
container_existe() {
  az container show --name "$1" --resource-group "$RESOURCE_GROUP_NAME" --output none 2>/dev/null
}

# Lê uma senha nova sem mostrar na tela e guarda na variável cujo nome vem em $1.
# As portas do RabbitMQ e do Oracle ficam abertas na internet: exige 12+ caracteres.
# Aspas e espaços são recusados porque quebrariam o comando que define a senha no Oracle.
pedir_senha_nova() {
  local destino="$1" descricao="$2" senha
  # -r: mantém as barras invertidas (\) da senha como foram digitadas
  read -r -s -p "Defina a senha do $descricao (mínimo 12 caracteres): " senha \
    || falhar "Não foi possível ler a senha. Rode o script num terminal interativo."
  echo
  [ ${#senha} -ge 12 ] \
    || falhar "Senha do $descricao muito curta: use 12 caracteres ou mais (a porta fica aberta na internet)."
  case "$senha" in
    *[\"\'[:space:]]*) falhar "A senha do $descricao não pode ter aspas nem espaços." ;;
  esac
  printf -v "$destino" '%s' "$senha"
}


# Variáveis
RESOURCE_GROUP_NAME="gs-ajudantefamiliar"
APP_SERVICE_PLAN="ajudante-familiar-SP"
LOCATION="brazilsouth"
RUNTIME="JAVA:21-java21"
APP_INSIGHTS_NAME="ai-ajudante-familiar"
APP_SERVICE_SKU="B3"   # 4 vCPUs e 7 GB, divididos pelas 3 APIs (o B1, com 1 vCPU, não dava conta das 3)

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

# Oracle Free (Azure Container Instance): o mesmo banco do dockerfiles/compose.yaml, com versão fixa.
# "slim-faststart": imagem enxuta com o banco já inicializado, que sobe em segundos.
ORACLE_CONTAINER_NAME="oracle-ajudante-familiar"
ORACLE_DNS_LABEL="oracle-ajudante-familiar"       # vira <label>.brazilsouth.azurecontainer.io; precisa ser único na região
ORACLE_IMAGE="gvenzl/oracle-free:23-slim-faststart"
ORACLE_APP_USER="ajudante"                        # usuário/schema AJUDANTE, criado no FREEPDB1 (igual ao compose)
ORACLE_SERVICO="FREEPDB1"

# Conferir o login antes de tudo: sem ele, as checagens abaixo achariam que nada existe
ASSINATURA=$(az account show --query name --output tsv 2>/dev/null) \
  || falhar "Azure CLI sem login. Rode 'az login' e tente de novo."
echo "Assinatura em uso: $ASSINATURA"

# Senhas: digitadas aqui, não aparecem na tela e não ficam salvas neste arquivo.
# Só são pedidas para o container que ainda não existe. Guarde-as: vão nas variáveis
# secretas SPRING_RABBITMQ_PASSWORD e SPRING_DATASOURCE_PASSWORD das 3 pipelines.
RABBITMQ_PASSWORD=""
RABBITMQ_EXISTE=false
if container_existe "$RABBITMQ_CONTAINER_NAME"; then
  RABBITMQ_EXISTE=true
else
  pedir_senha_nova RABBITMQ_PASSWORD "RabbitMQ"
fi

ORACLE_APP_PASSWORD=""
ORACLE_ADMIN_PASSWORD=""
ORACLE_EXISTE=false
if container_existe "$ORACLE_CONTAINER_NAME"; then
  ORACLE_EXISTE=true
else
  pedir_senha_nova ORACLE_APP_PASSWORD "usuário $ORACLE_APP_USER do Oracle (usada pelas APIs)"
  pedir_senha_nova ORACLE_ADMIN_PASSWORD "administrador do Oracle (SYS e SYSTEM)"
  [ "$ORACLE_APP_PASSWORD" != "$ORACLE_ADMIN_PASSWORD" ] \
    || falhar "Use senhas diferentes para o usuário da aplicação e para o administrador."
fi

# Registrar os Serviços utilizados
az provider register --namespace Microsoft.Web
az provider register --namespace Microsoft.Insights
az provider register --namespace Microsoft.OperationalInsights
az provider register --namespace Microsoft.ServiceLinker
az provider register --namespace Microsoft.ContainerInstance
# AlertsManagement: o Application Insights cria junto a regra de alerta "Failure Anomalies";
# sem este registro, essa parte falha e aparece como deployment com erro no grupo de recursos
az provider register --namespace Microsoft.AlertsManagement

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

# Criar o Plano de Serviço (as 3 APIs rodam no mesmo plano)
az appservice plan create \
  --name $APP_SERVICE_PLAN \
  --resource-group $RESOURCE_GROUP_NAME \
  --location "$LOCATION" \
  --sku "$APP_SERVICE_SKU" \
  --is-linux

# Garantir o tamanho do plano também quando ele já existia (ex.: criado antes como B1).
# Trocar o tamanho reinicia os apps do plano, sem perder deploy nem configuração.
az appservice plan update \
  --name $APP_SERVICE_PLAN \
  --resource-group $RESOURCE_GROUP_NAME \
  --sku "$APP_SERVICE_SKU" \
  --output none

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

  # Manter o app sempre ligado (disponível a partir do B1): evita a demora de subir o Spring Boot a cada acesso
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

# Criar o Oracle no Azure Container Instance
# 1521: porta do banco (serviço FREEPDB1). 2 vCPUs e 4 GB: o mínimo confortável para o Oracle Free.
# Sem volume: os dados se perdem se o container for recriado. Nesse caso, rode de novo a pipeline
# da API de funções primeiro (o Flyway recria as tabelas base) e depois as outras duas.
echo
if [ "$ORACLE_EXISTE" = true ]; then
  echo "==> Oracle $ORACLE_CONTAINER_NAME já existe: mantido (as senhas continuam as mesmas)."
else
  echo "==> Oracle $ORACLE_CONTAINER_NAME"
  az container create \
    --name "$ORACLE_CONTAINER_NAME" \
    --resource-group "$RESOURCE_GROUP_NAME" \
    --location "$LOCATION" \
    --image "$ORACLE_IMAGE" \
    --os-type Linux \
    --cpu 2 \
    --memory 4 \
    --ip-address Public \
    --dns-name-label "$ORACLE_DNS_LABEL" \
    --ports 1521 \
    --restart-policy Always \
    --environment-variables APP_USER="$ORACLE_APP_USER" \
    --secure-environment-variables \
      ORACLE_PASSWORD="$ORACLE_ADMIN_PASSWORD" \
      APP_USER_PASSWORD="$ORACLE_APP_PASSWORD" \
    --output none

  # O container sobe antes do banco: espera a mensagem de pronto da imagem (até 20 minutos).
  # Os logs vão para uma variável antes do grep: com pipefail, "az ... | grep -q" falharia
  # justamente quando acha a mensagem (o grep fecha o pipe e o az recebe SIGPIPE).
  echo "Aguardando o Oracle ficar pronto..."
  ORACLE_LIMITE=$((SECONDS + 1200))
  until LOGS_ORACLE=$(az container logs --name "$ORACLE_CONTAINER_NAME" --resource-group "$RESOURCE_GROUP_NAME" 2>/dev/null || true); \
        grep -qF "DATABASE IS READY TO USE" <<<"$LOGS_ORACLE"; do
    [ $SECONDS -lt $ORACLE_LIMITE ] \
      || falhar "O Oracle não ficou pronto em 20 minutos. Veja os logs: az container logs --name $ORACLE_CONTAINER_NAME --resource-group $RESOURCE_GROUP_NAME"
    sleep 20
  done
  echo "Oracle pronto."
fi

# Endereço do Oracle (vai no SPRING_DATASOURCE_URL)
ORACLE_HOST=$(az container show \
  --name "$ORACLE_CONTAINER_NAME" \
  --resource-group "$RESOURCE_GROUP_NAME" \
  --query ipAddress.fqdn \
  --output tsv)
[ -n "$ORACLE_HOST" ] \
  || falhar "O container $ORACLE_CONTAINER_NAME existe, mas está sem endereço público (ipAddress.fqdn)."

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
echo
echo " Valores do Oracle para as Variables das 3 pipelines:"
echo "   SPRING_DATASOURCE_URL      = jdbc:oracle:thin:@//$ORACLE_HOST:1521/$ORACLE_SERVICO"
echo "   SPRING_DATASOURCE_USERNAME = $ORACLE_APP_USER"
echo "   SPRING_DATASOURCE_PASSWORD = (a senha do usuário $ORACLE_APP_USER; marque como secreta)"
echo " SQL Developer: host $ORACLE_HOST, porta 1521, nome do serviço $ORACLE_SERVICO, usuário $ORACLE_APP_USER"
if [ "$ORACLE_EXISTE" = false ]; then
  echo
  echo " Banco novo e vazio: atualize as Variables e rode PRIMEIRO a pipeline da API de funções"
  echo " (as migrations dela criam as tabelas base); depois, as da autenticação e do RAG."
fi
echo "=================================================================="
