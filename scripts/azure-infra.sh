#
# VERIFIQUE SE ESTA COM A ASSINATURA CORRRETA
#
# az account show
# az account list -o table
# az account set --subscription "nome-da-assinatura-correta"


# Variáveis
# Usa o grupo de recursos, o plano B1 e o Application Insights criados pelo script
# API-Funcoes/scripts/azure-infra.sh: rode aquele primeiro. Aqui só é criado o Web App.
RESOURCE_GROUP_NAME="gs-ajudantefamiliar"
WEB_APP_NAME="ajudante-familiar-rag"   # igual ao WEB_APP_NAME do azure-pipeline.yml
APP_SERVICE_PLAN="ajudante-familiar-SP"
RUNTIME="JAVA:21-java21"
APP_INSIGHTS_NAME="ai-ajudante-familiar"


# Conferir se a infraestrutura compartilhada já existe
if ! az appservice plan show --name $APP_SERVICE_PLAN --resource-group $RESOURCE_GROUP_NAME --output none 2>/dev/null; then
  echo "Plano $APP_SERVICE_PLAN não encontrado no grupo $RESOURCE_GROUP_NAME."
  echo "Rode antes o script API-Funcoes/scripts/azure-infra.sh."
  exit 1
fi

# Registrar a extensão utilizada
az extension add --name application-insights

# Criar o Serviço de Aplicativo no mesmo plano da API de funções
az webapp create \
  --name $WEB_APP_NAME \
  --resource-group $RESOURCE_GROUP_NAME \
  --plan $APP_SERVICE_PLAN \
  --runtime "$RUNTIME"

# Recuperar a String de Conexão do Application Insights (o mesmo das outras APIs)
CONNECTION_STRING=$(az monitor app-insights component show \
  --app $APP_INSIGHTS_NAME \
  --resource-group $RESOURCE_GROUP_NAME \
  --query connectionString \
  --output tsv)

# Configurar as Variáveis de Ambiente do Application Insights
az webapp config appsettings set \
  --name "$WEB_APP_NAME" \
  --resource-group "$RESOURCE_GROUP_NAME" \
  --settings \
    APPLICATIONINSIGHTS_CONNECTION_STRING="$CONNECTION_STRING" \
    ApplicationInsightsAgent_EXTENSION_VERSION="~3" \
    XDT_MicrosoftApplicationInsights_Mode="Recommended" \
    XDT_MicrosoftApplicationInsights_PreemptSdk="1"
# Banco, RabbitMQ e JWT_SEGREDO NÃO ficam aqui: a pipeline grava essas variáveis no Web App
# a cada deploy (e a OPENAI_API_KEY, só no RAG). O JWT_SEGREDO precisa ser o mesmo das outras APIs.

# Manter o app sempre ligado (disponível no B1)
az webapp config set \
  --name "$WEB_APP_NAME" \
  --resource-group "$RESOURCE_GROUP_NAME" \
  --always-on true

# Reiniciar o Web App
az webapp restart --name $WEB_APP_NAME --resource-group $RESOURCE_GROUP_NAME

# Criar a conexão do nosso Web App com o Application Insights
az monitor app-insights component connect-webapp \
    --app $APP_INSIGHTS_NAME \
    --web-app $WEB_APP_NAME \
    --resource-group $RESOURCE_GROUP_NAME
