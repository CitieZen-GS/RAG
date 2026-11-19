-- Conversa do chat: uma conversa contínua por cuidador e idoso.
-- Depende de T_Ajudante_familiar_Cuidador e T_Ajudante_familiar_Idoso (migrations da API de funções).
CREATE TABLE T_Ajudante_familiar_Conversa (
    id_conversa INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cuidador INTEGER NOT NULL,
    id_idoso INTEGER NOT NULL,
    dt_criacao TIMESTAMP NOT NULL,
    dt_ultima_mensagem TIMESTAMP,
    CONSTRAINT uk_conversa_cuidador_idoso UNIQUE (id_cuidador, id_idoso),
    CONSTRAINT fk_conversa_cuidador FOREIGN KEY (id_cuidador)
        REFERENCES T_Ajudante_familiar_Cuidador (id_cuidador),
    CONSTRAINT fk_conversa_idoso FOREIGN KEY (id_idoso)
        REFERENCES T_Ajudante_familiar_Idoso (id_idoso)
);

-- O índice da UNIQUE já cobre buscas por id_cuidador; este cobre a FK de id_idoso
CREATE INDEX ix_conversa_idoso ON T_Ajudante_familiar_Conversa (id_idoso);
