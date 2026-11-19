# Ajudante Familiar

App para auxiliar nos cuidados de idosos. Projeto de estudo.

## Perfis de acesso

### Cuidador
- Cadastrar medicamentos e informações do idoso
- Registrar atividades a serem feitas com o idoso
- Marcar lembretes no calendário do app para futuras consultas
- Registrar a alimentação do idoso

### Familiar
- Somente visualização de tudo que o cuidador registrar: medicamentos, alimentação, futuras consultas e atividades diárias

## Arquitetura

Microsserviços (3 APIs) comunicando-se via RabbitMQ:

1. **API de funções**: funções HTTP do cuidador e do paciente
2. **API de autenticação**: validação de acessos e permissões via JWT
3. **API de RAG**: RAG básico para um chat que busca informações do paciente e gera relatórios conforme o cuidador pedir

Migrations de cada API via Flyway.

## Convenções

- Código, comentários, nomes de classes, métodos, variáveis e tabelas sempre em **português**.
- Exceção: os nomes dos packages listados abaixo permanecem em inglês.

## Organização das APIs (mesmos packages em todas)

| Package | Responsabilidade |
|---|---|
| `models` | Entidades e relacionamentos de tabelas |
| `repositories` | Acesso ao banco de dados, consultas e alterações |
| `services` | Regras de negócio |
| `dto` | Objetos que definem o que entra e o que sai da API |
| `controller` | Recebe as requisições HTTP |
| `mensageria` | RabbitMQ: eventos (records), configuração de exchanges/filas, publicadores e consumidores |

## Regras de camadas

- `controller` conversa apenas com `services` e usa `dto` na entrada e saída.
- `services` concentra as regras de negócio e acessa o banco somente via `repositories`.
- Entidades (`models`) nunca são expostas diretamente nas respostas da API.

## Banco e migrations

- Nunca edite uma migration já mergeada: crie uma nova.
- O schema é só do Flyway.

## Padrão de ambiente (vale para todas as APIs)

### Docker Compose

- Toda API tem um `compose.yaml` **idêntico** ao da API de funções, com `name: api-funcoes`. O mesmo nome de projeto faz todas usarem os mesmos containers (um Oracle e um RabbitMQ) em vez de subir cópias que disputariam as portas.
- Ao alterar o `compose.yaml` de uma API, replique a mudança nas outras.
- Dependência `spring-boot-docker-compose` (`runtime`, `optional`): o `./mvnw spring-boot:run` sobe o compose e configura banco e RabbitMQ a partir dele. **Não** declare `spring.datasource` nem `spring.rabbitmq` no `application.yaml`.
- O Spring só para os containers que ele mesmo subiu. Se outra API já estava rodando, os containers são reaproveitados.
- Ordem de subida: **API de funções primeiro**. As migrations dela criam as tabelas base (cargo, perfil, cuidador, idoso, parente...) das quais as outras dependem.

### Portas

| Serviço | Porta |
|---|---|
| Oracle Free (serviço `FREEPDB1`) | 1521 |
| RabbitMQ (AMQP) | 5672 |
| API de funções | 8080 |
| API de autenticação | 8081 |
| API de RAG | 8082 |

A porta da API fica fixa em `server.port` no `application.yaml`. As portas do Oracle e do RabbitMQ vêm só do `compose.yaml`.

### Credenciais de desenvolvimento

| Serviço | Usuário / senha |
|---|---|
| Oracle (schema da aplicação `AJUDANTE`) | `ajudante` / `secret` |
| Oracle (admin) | `system` / `secret` |
| RabbitMQ | `myuser` / `secret` |

### Variáveis de ambiente

Em desenvolvimento nenhuma é obrigatória. Fora do Docker Compose (servidor, `java -jar`), defina:

| Variável | Descrição |
|---|---|
| `JWT_SEGREDO` | Segredo HS256 dos tokens (mínimo 32 caracteres, **igual em todas as APIs**) |
| `SPRING_DATASOURCE_URL` | Ex.: `jdbc:oracle:thin:@//host:1521/FREEPDB1` |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Usuário e senha do Oracle |
| `SPRING_RABBITMQ_HOST` / `SPRING_RABBITMQ_PORT` | Endereço e porta do RabbitMQ |
| `SPRING_RABBITMQ_USERNAME` / `SPRING_RABBITMQ_PASSWORD` | Usuário e senha do RabbitMQ |
| `SERVER_PORT` | Opcional: troca a porta da API |
| `OPENAI_API_KEY` | Só no RAG (secreta): chave da OpenAI usada pelo Spring AI |
| `JWT_EXPIRACAO_ACESSO_MINUTOS` | Só na autenticação: validade do token de acesso (padrão 60) |
| `JWT_EXPIRACAO_ATUALIZACAO_DIAS` | Só na autenticação: validade do token de atualização (padrão 7) |

Regras para novas variáveis:

- Para o que o Spring já cobre, use os nomes padrão dele (`SPRING_...`, `SERVER_...`). Não invente nomes próprios.
- Configuração própria da aplicação: chave em português no YAML e variável em MAIÚSCULAS com `_`, com valor padrão de dev, ex.: `jwt.segredo: "${JWT_SEGREDO:valor-de-dev}"`.
- O valor padrão serve só para desenvolvimento. Em produção, a variável deve ser sempre definida.

### Flyway

Todas as APIs usam o mesmo schema `AJUDANTE`. Cada uma tem prefixo e tabela de histórico próprios:

| API | Prefixo | Tabela de histórico |
|---|---|---|
| Funções | `M_AJ_V` | `flyway_schema_history` |
| Autenticação | `M_AUT_V` | `flyway_historico_autenticacao` |
| RAG | `M_RAG_V` | `flyway_historico_rag` |

APIs que não são a de funções usam `baseline-on-migrate: true` e `baseline-version: 0`, porque o schema já existe quando elas sobem.

Testes (Testcontainers) começam com o Oracle vazio. Por isso as APIs que dependem das tabelas da API de funções têm cópias **só de teste** das migrations dela, em `src/test/resources/db/migration-teste`, com versões abaixo de 1 (ex.: `M_AUT_V0.1__...`). Elas são ativadas por `src/test/resources/config/application.yaml`. Quando a API de funções criar uma nova migration que mude essas tabelas, atualize as cópias.

## Deploy (Azure)

- Cada API tem `azure-pipeline.yaml` na raiz e `scripts/azure-infra.sh`.
- `API-Funcoes/scripts/azure-infra.sh` cria o grupo `gs-ajudantefamiliar`, o plano B1 `ajudante-familiar-SP`, o Application Insights `ai-ajudante-familiar`, o RabbitMQ no Azure Container Instance (`rabbitmq-ajudante-familiar.brazilsouth.azurecontainer.io`, usuário `ajudante`, senha digitada ao rodar) e o Web App da API de funções. Rode primeiro.
- O script das outras APIs só cria o Web App `ajudante-familiar-<api>` nesse plano e o liga ao Application Insights.
- O RAG precisa também da variável secreta `OPENAI_API_KEY` na pipeline.
- Credenciais (banco, RabbitMQ, `JWT_SEGREDO`) nunca vão nos scripts: a pipeline grava no Web App a cada deploy.

## Contrato do JWT (autenticação → outras APIs)

A API de autenticação emite e as outras só validam. Assinatura HS256 com `jwt.segredo` (`JWT_SEGREDO`), igual em todas.

| Claim | Cuidador | Parente |
|---|---|---|
| `sub` | `id_cuidador` | `id_parente` |
| `roles` | `["CUIDADOR"]` | `["FAMILIAR"]` |
| `permissoes` | `["LEITURA", "ESCRITA"]` | `["LEITURA"]` |
| `id_idoso` | não existe | id do idoso vinculado ao parente |
| `nome`, `email` | dados do cuidador | dados do parente |
| `jti` | id único do token (UUID) | idem |
| `iss`, `iat`, `exp` | `ajudante-familiar-autenticacao`, emissão, expiração | idem |

- O tipo vem da tabela onde o e-mail foi encontrado: `T_Ajudante_familiar_Cuidador` dá CUIDADOR, `T_Ajudante_familiar_Parente` dá FAMILIAR.
- A autorização nas outras APIs usa `roles`: CUIDADOR lê e escreve, FAMILIAR só faz GET.
- `sub` só é único junto com `roles`: um cuidador e um parente podem ter o mesmo id.
- Senhas ficam em `ds_senha` como hash BCrypt. Tokens de atualização ficam só como hash SHA-256.
- Mudou alguma claim? Atualize esta tabela e a `ConfiguracaoSeguranca` das APIs que validam o token.

### Rotas da API de autenticação (porta 8081)

| Método | Rota | Corpo | Resposta |
|---|---|---|---|
| POST | `/autenticacao/login` | `{"email", "senha"}` | `tokenAcesso`, `tokenAtualizacao`, `expiraEm`, `usuario` |
| POST | `/autenticacao/atualizar` | `{"tokenAtualizacao"}` | novo par de tokens; o antigo é revogado |
| POST | `/autenticacao/sair` | `{"tokenAtualizacao", "tokenAcesso"?}` | 204, revoga o token de atualização; com `tokenAcesso`, as outras APIs passam a recusá-lo na hora (RabbitMQ) |
| GET | `/autenticacao/eu` | header `Authorization: Bearer` | usuário do token (o app usa ao reabrir) |
| POST | `/autenticacao/cadastro/cuidador` | `{"nome", "cpf"?, "email", "senha"}` | 201 + tokens (já sai logado). Público |
| POST | `/autenticacao/cadastro/parente` | `{"nome", "telefone"?, "email", "senha", "idIdoso"}` | 201 + usuário. Só CUIDADOR, e só para um idoso dele |

Regras do cadastro: e-mail único entre cuidadores **e** parentes, salvo em minúsculas. CPF salvo como `000.000.000-00`. Senha de 6 a 72 caracteres, salva em BCrypt. A autenticação lê `T_Ajudante_familiar_Idoso` só para conferir o dono, e nunca grava nela.

Swagger: http://localhost:8081/swagger-ui.html. Toda rota nova precisa de `@Operation`, `@ApiResponses` com os códigos de erro e `@Schema` com exemplo nos DTOs.

## Mensageria (RabbitMQ)

As APIs compartilham o banco, então mensagens **não** servem para copiar dados: servem para uma API avisar a outra que algo aconteceu.

- Exchange único, tipo topic: `ajudante-familiar.eventos`. Falhas vão para `ajudante-familiar.eventos.dlx`.
- Routing key: `<api que publica>.<o que aconteceu>`, ex.: `autenticacao.acesso-revogado`.
- Fila: `<api que consome>.<evento>`, ex.: `funcoes.acesso-revogado`, com a fila de falhas `<fila>.dlq`.
- Mensagens em JSON (`JacksonJsonMessageConverter`). Cada API tem o próprio record do evento, com os mesmos campos: mudou um, mude nos dois lados e nesta tabela.
- Quem publica declara o exchange. Quem consome declara exchange, fila, DLQ e bindings (`mensageria/ConfiguracaoMensageria`).
- Publicação sempre **depois do commit**: o service chama `ApplicationEventPublisher.publishEvent(...)` e o `PublicadorEventos` (`@TransactionalEventListener`) envia ao RabbitMQ. Falha no RabbitMQ é só registrada em log e não desfaz a operação.
- Consumidores: 3 tentativas (1s de intervalo); depois a mensagem vai para a `.dlq`.

| Evento | Routing key | Publica | Consome | Campos | Efeito |
|---|---|---|---|---|---|
| `AcessoRevogadoEvento` | `autenticacao.acesso-revogado` | autenticação, no `/sair` com `tokenAcesso` | funções (`funcoes.acesso-revogado`) e RAG (`rag.acesso-revogado`) | `jti`, `idUsuario`, `tipoUsuario`, `expiraEm`, `revogadoEm`, `motivo` | o JWT com esse `jti` é recusado (401) até expirar; lista em memória |
| `RelatorioSolicitadoEvento` | `rag.relatorio-solicitado` | RAG, após gravar o relatório `GERANDO` | RAG (`rag.relatorio-solicitado`) | `idRelatorio` | gera o relatório com a IA e marca `CONCLUIDO`/`ERRO`; RabbitMQ fora do ar no pedido → `ERRO` na hora |

## API de RAG (porta 8082)

Chat do cuidador sobre a linha do tempo do idoso e geração de relatórios, com IA (Spring AI + OpenAI).

- **Busca dos dados (sem vetores):** consultas SQL por idoso e período nas tabelas da API de funções. As entidades delas ficam em `models/funcoes`, todas `@Immutable` (só leitura; o RAG nunca grava nelas). Colunas com valores de banco (tipo de refeição, status da consulta) ficam como texto. Mudou uma tabela na API de funções? Atualize a entidade correspondente.
- **Tabelas próprias** (migrations `M_RAG_V`):

| Tabela | Para que serve | Regras |
|---|---|---|
| `T_Ajudante_familiar_Conversa` | conversa do chat | **uma** por cuidador e idoso (`uk_conversa_cuidador_idoso`) |
| `T_Ajudante_familiar_Mensagem_Chat` | mensagens da conversa | `tp_autor` CUIDADOR ou ASSISTENTE; conteúdo em CLOB; ordem por `dt_envio` |
| `T_Ajudante_familiar_Relatorio` | relatórios gerados | `tp_relatorio` GERAL, MEDICAMENTOS, ALIMENTACAO, ATIVIDADES ou CONSULTAS; `st_relatorio` GERANDO → CONCLUIDO ou ERRO; período com `dt_fim_periodo >= dt_inicio_periodo` |

- **Acesso:** só CUIDADOR (FAMILIAR recebe 403 em tudo), e só aos idosos dele: toda rota chama `AcessoIdosoService.buscarIdosoDoCuidador` antes de ler dados do idoso (outro cuidador recebe 403; idoso inexistente, 404).
- Enums gravados pelo nome (`@Enumerated(EnumType.STRING)`), iguais aos valores das constraints.
- Datas de criação/envio preenchidas em `@PrePersist`.
- Variável de ambiente própria: `OPENAI_API_KEY` (secreta). Sem ela a aplicação sobe, mas as chamadas à IA falham.

### Rotas da API de RAG (porta 8082, só CUIDADOR)

| Método | Rota | O que faz |
|---|---|---|
| POST | `/idosos/{idIdoso}/chat` | `{"pergunta", "dataInicio"?, "dataFim"?}` → resposta da IA + `periodoConsultado`. Grava pergunta e resposta |
| GET | `/idosos/{idIdoso}/chat?limite=50` | histórico em ordem cronológica (1 a 200 mensagens) |
| DELETE | `/idosos/{idIdoso}/chat` | apaga as mensagens (a memória recomeça) |
| POST | `/idosos/{idIdoso}/relatorios` | `{"tipo", "dataInicio", "dataFim", "titulo"?, "solicitacao"?}` → **202** com status `GERANDO` |
| GET | `/idosos/{idIdoso}/relatorios` | lista sem o conteúdo, mais recentes primeiro |
| GET | `/idosos/{idIdoso}/relatorios/{id}` | relatório com o conteúdo em Markdown (consultar até `CONCLUIDO` ou `ERRO`) |
| DELETE | `/idosos/{idIdoso}/relatorios/{id}` | remove o relatório |

Erros: 400 dados inválidos, 401 token ausente/revogado, 403 familiar ou idoso de outro cuidador, 404 não encontrado, 422 período inválido, 503 IA não respondeu (nada é gravado).

### Como uma pergunta é respondida

1. `AcessoIdosoService` confere o dono do idoso.
2. `PeriodoService` define o período: datas do app (`INFORMADO`) → IA lê a pergunta (`IDENTIFICADO_PELA_IA`, resposta estruturada `PeriodoIdentificado`) → últimos 7 dias (`PADRAO`). A IA recebe a pergunta anterior para entender continuações ("e no jantar?").
3. `LinhaDoTempoService` monta o contexto: dados do idoso, doenças e registros do período em ordem cronológica.
4. `ChatService` envia à IA: prompt de sistema (`services/ia/PromptsIa`, com o contexto) + últimas mensagens (memória) + pergunta.
5. Pergunta e resposta são gravadas. A chamada à IA fica fora de transação (leva segundos).

Prompts ficam todos em `PromptsIa`: responder só com o contexto, citar datas, dizer quando não há registro, nunca diagnosticar nem sugerir mudar medicação.

### Configurações (`rag.*` no application.yaml, classe `PropriedadesRag`)

`fuso-horario` (America/Sao_Paulo: "hoje" e datas gravadas no horário do Brasil, o Azure roda em UTC), `periodo-padrao-dias` (7), `periodo-maximo-dias` (90), `historico-mensagens` (10), `limite-registros` (400). Modelo da IA: `OPENAI_MODELO` (padrão `gpt-5-mini`).

### Cuidados

- `pom.xml`: o `spring-ai-starter-model-openai` exclui `io.swagger.core.v3:swagger-annotations` (versão antiga trazida pelo Spring AI). Sem a exclusão, o `/v3/api-docs` quebra com `NoSuchMethodError`.
- Gravações dentro de `@TransactionalEventListener` (depois do commit) precisam de transação nova (`PROPAGATION_REQUIRES_NEW`), senão são ignoradas sem erro. Ver `PublicadorEventos.marcarErro`.
- Testes não chamam a OpenAI: use `apoio/IaFalsa` com `ChatClient.create(iaFalsa)`.
