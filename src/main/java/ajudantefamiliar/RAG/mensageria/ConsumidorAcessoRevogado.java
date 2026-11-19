package ajudantefamiliar.RAG.mensageria;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ajudantefamiliar.RAG.services.TokensRevogadosService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Recebe da API de autenticação os tokens revogados (logout) e passa a recusá-los.
 * Se der erro, o Spring tenta de novo (3 vezes, ver application.yml) e depois manda a mensagem
 * para a fila rag.acesso-revogado.dlq.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsumidorAcessoRevogado {

    private final TokensRevogadosService tokensRevogadosService;

    @RabbitListener(queues = ConfiguracaoMensageria.FILA_ACESSO_REVOGADO)
    public void receber(AcessoRevogadoEvento evento) {
        if (evento.jti() == null || evento.expiraEm() == null) {
            throw new IllegalArgumentException("Evento de acesso revogado sem jti ou expiraEm: " + evento);
        }
        tokensRevogadosService.revogar(evento.jti(), evento.expiraEm());
        log.info("Token revogado ({}): jti={}, usuario={} {}", evento.motivo(), evento.jti(),
                evento.tipoUsuario(), evento.idUsuario());
    }
}
