-- SÓ PARA TESTES: cópia de M_AJ_V07__criar_tabela_atividades.sql da API de funções.
-- No banco real essa tabela é criada pela API de funções. Aqui ela existe para o Oracle
-- vazio do Testcontainers ter as tabelas que o RAG lê e das quais as M_RAG_V dependem.
-- Se a migration original mudar (numa nova M_AJ_V), atualize esta cópia.
CREATE TABLE T_Ajudante_familiar_Atividades (
    id_atividade INTEGER GENERATED ALWAYS AS IDENTITY,
    nm_atividade VARCHAR(20) NOT NULL,
    ds_atividade VARCHAR(80),
    dt_atividade DATE NOT NULL,
    hr_atividade_inicio DATE,
    hr_atividade_fim DATE,
    id_idoso INTEGER NOT NULL,
    CONSTRAINT pk_atividades PRIMARY KEY (id_atividade),
    CONSTRAINT fk_atividade_idoso FOREIGN KEY (id_idoso)
        REFERENCES T_Ajudante_familiar_Idoso (id_idoso)
);
