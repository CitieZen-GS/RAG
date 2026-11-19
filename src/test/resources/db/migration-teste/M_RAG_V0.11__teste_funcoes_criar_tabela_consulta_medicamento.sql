-- SÓ PARA TESTES: cópia de M_AJ_V11__criar_tabela_consulta_medicamento.sql da API de funções.
-- No banco real essa tabela é criada pela API de funções. Aqui ela existe para o Oracle
-- vazio do Testcontainers ter as tabelas que o RAG lê e das quais as M_RAG_V dependem.
-- Se a migration original mudar (numa nova M_AJ_V), atualize esta cópia.
CREATE TABLE T_Ajudante_familiar_Consulta_Medicamento (
    id_consulta_medicamento INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_lembrete_consulta INTEGER NOT NULL,
    id_medicamentos INTEGER NOT NULL,
    CONSTRAINT fk_cm_lembrente_consulta FOREIGN KEY (id_lembrete_consulta)
        REFERENCES T_Ajudante_familiar_Lembrete_Consulta (id_lembrete_consulta),
    CONSTRAINT fk_cm_medicamentos FOREIGN KEY (id_medicamentos)
        REFERENCES T_Ajudante_familiar_Medicamentos (id_medicamentos)
);
