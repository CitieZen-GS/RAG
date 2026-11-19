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
