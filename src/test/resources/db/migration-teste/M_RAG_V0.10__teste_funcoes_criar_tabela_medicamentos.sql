-- SÓ PARA TESTES: cópia de M_AJ_V10__criar_tabela_medicamentos.sql da API de funções.
-- No banco real essa tabela é criada pela API de funções. Aqui ela existe para o Oracle
-- vazio do Testcontainers ter as tabelas que o RAG lê e das quais as M_RAG_V dependem.
-- Se a migration original mudar (numa nova M_AJ_V), atualize esta cópia.
CREATE TABLE T_Ajudante_familiar_Medicamentos (
    id_medicamentos INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nm_medicamentos VARCHAR(20),
    via_administracao VARCHAR(20),
    vl_frquencia VARCHAR(50),
    ds_instrucao_uso VARCHAR(100),
    dt_medicamento DATE,
    hr_medicamento DATE NOT NULL,
    id_idoso INTEGER NOT NULL,
    CONSTRAINT fk_medicamento_idoso FOREIGN KEY (id_idoso)
        REFERENCES T_Ajudante_familiar_Idoso (id_idoso)
);
