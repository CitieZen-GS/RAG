-- SÓ PARA TESTES: cópia de M_AJ_V01__criar_tabela_cargo.sql da API de funções.
-- No banco real essa tabela é criada pela API de funções. Aqui ela existe para o Oracle
-- vazio do Testcontainers ter as tabelas que o RAG lê e das quais as M_RAG_V dependem.
-- Se a migration original mudar (numa nova M_AJ_V), atualize esta cópia.
CREATE TABLE T_Ajudante_familiar_Cargo (
    id_cargo INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nm_cargo VARCHAR(20),
    fg_permissao CHAR(1),
    CONSTRAINT ck_cargo CHECK (fg_permissao IN ('S', 'N'))
);
