-- SÓ PARA TESTES: cópia de M_AJ_V08__criar_tabela_alimentacao.sql da API de funções.
-- No banco real essa tabela é criada pela API de funções. Aqui ela existe para o Oracle
-- vazio do Testcontainers ter as tabelas que o RAG lê e das quais as M_RAG_V dependem.
-- Se a migration original mudar (numa nova M_AJ_V), atualize esta cópia.
CREATE TABLE T_Ajudante_familiar_Alimentacao (
    id_alimentacao INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nm_alimentacao VARCHAR(20) NOT NULL,
    tp_alimentacao VARCHAR(20) NOT NULL,
    dt_consumo DATE NOT NULL,
    hr_consumo DATE NOT NULL,
    vlr_caloria DECIMAL(6,2),
    ds_preparacao VARCHAR(100),
    id_idoso INTEGER NOT NULL,
    CONSTRAINT ck_alimentacao_tipo CHECK
        (tp_alimentacao IN ('Cafe da manha', 'Almoco', 'Lanche de tarde', 'Jantar')),
    CONSTRAINT fk_lembrente_familia_idoso FOREIGN KEY (id_idoso)
        REFERENCES T_Ajudante_familiar_Idoso (id_idoso)
);
