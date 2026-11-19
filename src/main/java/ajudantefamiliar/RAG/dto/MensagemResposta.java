package ajudantefamiliar.RAG.dto;

import ajudantefamiliar.RAG.models.MensagemChat;
import ajudantefamiliar.RAG.models.enums.AutorMensagem;

import java.time.LocalDateTime;

public record MensagemResposta(Long id, AutorMensagem autor, String conteudo, LocalDateTime dataEnvio) {

    public static MensagemResposta de(MensagemChat mensagem) {
        return new MensagemResposta(mensagem.getId(), mensagem.getAutor(), mensagem.getConteudo(),
                mensagem.getDataEnvio());
    }
}
