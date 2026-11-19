-- =====================================================================
-- Ajudante Familiar - API de RAG
-- script-bd.sql: objetos de banco (Oracle) desta API.
-- Depende das tabelas da API de funções (rode API-Funcoes/scripts/script-bd.sql antes).
-- Gerado a partir das migrations do Flyway em src/main/resources/db/migration,
-- na ordem em que são aplicadas. Na aplicação, o Flyway executa esses mesmos
-- arquivos sozinho; este script serve para criar o schema manualmente.
-- =====================================================================

-- ---------------------------------------------------------------------
-- M_RAG_V01__criar_tabela_conversa.sql
-- ---------------------------------------------------------------------
-- Conversa do chat: uma conversa contínua por cuidador e idoso.
-- Depende de T_Ajudante_familiar_Cuidador e T_Ajudante_familiar_Idoso (migrations da API de funções).
CREATE TABLE T_Ajudante_familiar_Conversa (
    id_conversa INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cuidador INTEGER NOT NULL,
    id_idoso INTEGER NOT NULL,
    dt_criacao TIMESTAMP NOT NULL,
    dt_ultima_mensagem TIMESTAMP,
    CONSTRAINT uk_conversa_cuidador_idoso UNIQUE (id_cuidador, id_idoso),
    CONSTRAINT fk_conversa_cuidador FOREIGN KEY (id_cuidador)
        REFERENCES T_Ajudante_familiar_Cuidador (id_cuidador),
    CONSTRAINT fk_conversa_idoso FOREIGN KEY (id_idoso)
        REFERENCES T_Ajudante_familiar_Idoso (id_idoso)
);

-- O índice da UNIQUE já cobre buscas por id_cuidador; este cobre a FK de id_idoso
CREATE INDEX ix_conversa_idoso ON T_Ajudante_familiar_Conversa (id_idoso);

-- ---------------------------------------------------------------------
-- M_RAG_V02__criar_tabela_mensagem_chat.sql
-- ---------------------------------------------------------------------
-- Mensagens da conversa, em ordem de envio: perguntas do cuidador e respostas da IA.
CREATE TABLE T_Ajudante_familiar_Mensagem_Chat (
    id_mensagem INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_conversa INTEGER NOT NULL,
    tp_autor VARCHAR(10) NOT NULL,
    ds_conteudo CLOB NOT NULL,
    dt_envio TIMESTAMP NOT NULL,
    CONSTRAINT ck_mensagem_autor CHECK (tp_autor IN ('CUIDADOR', 'ASSISTENTE')),
    CONSTRAINT fk_mensagem_conversa FOREIGN KEY (id_conversa)
        REFERENCES T_Ajudante_familiar_Conversa (id_conversa)
);

-- Carregar o histórico de uma conversa em ordem cronológica
CREATE INDEX ix_mensagem_conversa_envio ON T_Ajudante_familiar_Mensagem_Chat (id_conversa, dt_envio);

-- ---------------------------------------------------------------------
-- M_RAG_V03__criar_tabela_relatorio.sql
-- ---------------------------------------------------------------------
-- Relatórios gerados pela IA a partir dos dados do idoso em um período.
-- st_relatorio permite gerar em segundo plano (ex.: via RabbitMQ): GERANDO -> CONCLUIDO ou ERRO.
CREATE TABLE T_Ajudante_familiar_Relatorio (
    id_relatorio INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cuidador INTEGER NOT NULL,
    id_idoso INTEGER NOT NULL,
    tp_relatorio VARCHAR(15) NOT NULL,
    ds_titulo VARCHAR(100) NOT NULL,
    ds_solicitacao VARCHAR(500),
    dt_inicio_periodo DATE NOT NULL,
    dt_fim_periodo DATE NOT NULL,
    ds_conteudo CLOB,
    st_relatorio VARCHAR(10) DEFAULT 'GERANDO' NOT NULL,
    ds_erro VARCHAR(400),
    dt_criacao TIMESTAMP NOT NULL,
    dt_conclusao TIMESTAMP,
    CONSTRAINT ck_relatorio_tipo CHECK
        (tp_relatorio IN ('GERAL', 'MEDICAMENTOS', 'ALIMENTACAO', 'ATIVIDADES', 'CONSULTAS')),
    CONSTRAINT ck_relatorio_status CHECK (st_relatorio IN ('GERANDO', 'CONCLUIDO', 'ERRO')),
    CONSTRAINT ck_relatorio_periodo CHECK (dt_fim_periodo >= dt_inicio_periodo),
    CONSTRAINT fk_relatorio_cuidador FOREIGN KEY (id_cuidador)
        REFERENCES T_Ajudante_familiar_Cuidador (id_cuidador),
    CONSTRAINT fk_relatorio_idoso FOREIGN KEY (id_idoso)
        REFERENCES T_Ajudante_familiar_Idoso (id_idoso)
);

CREATE INDEX ix_relatorio_cuidador ON T_Ajudante_familiar_Relatorio (id_cuidador);
CREATE INDEX ix_relatorio_idoso ON T_Ajudante_familiar_Relatorio (id_idoso);
