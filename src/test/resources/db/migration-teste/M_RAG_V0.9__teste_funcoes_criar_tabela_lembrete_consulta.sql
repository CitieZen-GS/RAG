-- SÓ PARA TESTES: cópia de M_AJ_V09__criar_tabela_lembrete_consulta.sql da API de funções.
-- No banco real essa tabela é criada pela API de funções. Aqui ela existe para o Oracle
-- vazio do Testcontainers ter as tabelas que o RAG lê e das quais as M_RAG_V dependem.
-- Se a migration original mudar (numa nova M_AJ_V), atualize esta cópia.
CREATE TABLE T_Ajudante_familiar_Lembrete_Consulta (
    id_lembrete_consulta INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ds_titulo_lembrente_consulta VARCHAR(20),
    nm_consulta_medico VARCHAR(20),
    dt_consulta DATE,
    hr_consulta DATE,
    ds_consulta VARCHAR(100),
    st_consulta VARCHAR(10) DEFAULT 'Pendente' NOT NULL,
    id_idoso INTEGER NOT NULL,
    CONSTRAINT ck_consulta_status CHECK (st_consulta IN ('Pendente', 'Realizado')),
    CONSTRAINT fk_lembrente_consulta_idoso FOREIGN KEY (id_idoso)
        REFERENCES T_Ajudante_familiar_Idoso (id_idoso)
);
