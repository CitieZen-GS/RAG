-- SÓ PARA TESTES: cópia de M_AJ_V06__criar_tabela_doencas.sql da API de funções.
-- No banco real essa tabela é criada pela API de funções. Aqui ela existe para o Oracle
-- vazio do Testcontainers ter as tabelas que o RAG lê e das quais as M_RAG_V dependem.
-- Se a migration original mudar (numa nova M_AJ_V), atualize esta cópia.
CREATE TABLE T_Ajudante_familiar_Doencas (
    id_doenca INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nm_doenca VARCHAR(20),
    ds_doenca VARCHAR(20),
    id_idoso INTEGER NOT NULL,
    CONSTRAINT fk_doenca_idoso FOREIGN KEY (id_idoso)
        REFERENCES T_Ajudante_familiar_Idoso (id_idoso)
);
