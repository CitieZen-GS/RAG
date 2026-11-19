-- SÓ PARA TESTES: cópia de M_AJ_V03__criar_tabela_cuidador.sql da API de funções.
-- No banco real essa tabela é criada pela API de funções. Aqui ela existe para o Oracle
-- vazio do Testcontainers ter as tabelas que o RAG lê e das quais as M_RAG_V dependem.
-- Se a migration original mudar (numa nova M_AJ_V), atualize esta cópia.
CREATE TABLE T_Ajudante_familiar_Cuidador (
    id_cuidador INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nm_cuidador VARCHAR(20) NOT NULL,
    cpf_cuidador VARCHAR(14) UNIQUE,
    ds_email VARCHAR(100) UNIQUE,
    ds_senha VARCHAR(100),
    id_perfil INTEGER NOT NULL,
    CONSTRAINT fk_cuidador_perfil FOREIGN KEY (id_perfil)
        REFERENCES T_Ajudante_familiar_Perfil (id_perfil)
);
