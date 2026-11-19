package ajudantefamiliar.RAG.mensageria;

import ajudantefamiliar.RAG.configuracoes.PropriedadesRag;
import ajudantefamiliar.RAG.models.enums.StatusRelatorio;
import ajudantefamiliar.RAG.repositories.RelatorioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

/**
 * Envia os eventos do RAG para o RabbitMQ só DEPOIS do commit no banco (mesmo padrão da autenticação):
 * o relatório já existe com status GERANDO quando o consumidor recebe o pedido.
 * <p>
 * Se o RabbitMQ estiver fora do ar, o relatório é marcado como ERRO na hora, em vez de ficar GERANDO para sempre.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PublicadorEventos {

    private final RabbitTemplate rabbitTemplate;
    private final RelatorioRepository relatorioRepository;
    private final PropriedadesRag propriedades;
    private final PlatformTransactionManager gerenciadorDeTransacoes;

    @TransactionalEventListener
    public void publicar(RelatorioSolicitadoEvento evento) {
        try {
            rabbitTemplate.convertAndSend(ConfiguracaoMensageria.EXCHANGE_EVENTOS,
                    ConfiguracaoMensageria.ROTA_RELATORIO_SOLICITADO, evento);
            log.info("Evento {} publicado: relatorio={}", ConfiguracaoMensageria.ROTA_RELATORIO_SOLICITADO,
                    evento.idRelatorio());
        } catch (AmqpException e) {
            log.error("Falha ao publicar {} (relatorio={})", ConfiguracaoMensageria.ROTA_RELATORIO_SOLICITADO,
                    evento.idRelatorio(), e);
            marcarErro(evento.idRelatorio());
        }
    }

    /**
     * Precisa de uma transação NOVA: este código roda depois do commit, mas ainda "dentro" da transação
     * que acabou de terminar; uma gravação normal aqui seria ignorada sem erro.
     */
    private void marcarErro(Long idRelatorio) {
        TransactionTemplate novaTransacao = new TransactionTemplate(gerenciadorDeTransacoes);
        novaTransacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        novaTransacao.executeWithoutResult(status -> relatorioRepository.findById(idRelatorio).ifPresent(relatorio -> {
            relatorio.setStatus(StatusRelatorio.ERRO);
            relatorio.setErro("Fila de geração indisponível no momento. Solicite o relatório novamente.");
            relatorio.setDataConclusao(LocalDateTime.now(propriedades.zona()));
        }));
    }
}
