-- Mensagens da conversa, em ordem de envio: perguntas do cuidador e respostas da IA.
CREATE TABLE T_Ajudante_familiar_Mensagem_Chat (
    id_mensagem INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_conversa INTEGER NOT NULL,
    tp_autor VARCHAR(10) NOT NULL,
    ds_conteudo CLOB NOT NULL,
    dt_envio TIMESTAMP NOT NULL,
    CONSTRAINT ck_mensagem_autor CHECK (tp_autor IN ('CUIDADOR', 'ASSISTENTE')),
    CONSTRAINT fk_mensagem_conversa FOREIGN KEY (id_conversa)
        REFERENCES T_Ajudante_familiar_Conversa (id_conversa)
);

-- Carregar o histórico de uma conversa em ordem cronológica
CREATE INDEX ix_mensagem_conversa_envio ON T_Ajudante_familiar_Mensagem_Chat (id_conversa, dt_envio);
