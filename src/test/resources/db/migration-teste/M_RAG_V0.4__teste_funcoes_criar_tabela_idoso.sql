-- SÓ PARA TESTES: cópia de M_AJ_V04__criar_tabela_idoso.sql da API de funções.
-- No banco real essa tabela é criada pela API de funções. Aqui ela existe para o Oracle
-- vazio do Testcontainers ter as tabelas que o RAG lê e das quais as M_RAG_V dependem.
-- Se a migration original mudar (numa nova M_AJ_V), atualize esta cópia.
CREATE TABLE T_Ajudante_familiar_Idoso (
    id_idoso INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nm_idoso VARCHAR(20) NOT NULL,
    cpf_idoso VARCHAR(14),
    vlr_altura DECIMAL(3,2),
    vlr_peso DECIMAL(5,2),
    dt_nascimento DATE NOT NULL,
    ds_comportamento VARCHAR(100),
    id_cuidador INTEGER NOT NULL,
    CONSTRAINT fk_idoso_cuidador FOREIGN KEY (id_cuidador)
        REFERENCES T_Ajudante_familiar_Cuidador (id_cuidador)
);
