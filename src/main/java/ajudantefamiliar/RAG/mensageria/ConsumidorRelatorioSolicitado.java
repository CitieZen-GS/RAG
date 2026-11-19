package ajudantefamiliar.RAG.mensageria;

import ajudantefamiliar.RAG.services.GeradorRelatorioService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Recebe os pedidos de relatório e gera em segundo plano, um por vez.
 * Falhas da IA viram status ERRO no relatório; erros inesperados (ex.: banco fora do ar) são tentados
 * de novo 3 vezes e depois a mensagem vai para rag.relatorio-solicitado.dlq.
 */
@Component
@RequiredArgsConstructor
public class ConsumidorRelatorioSolicitado {

    private final GeradorRelatorioService geradorRelatorioService;

    @RabbitListener(queues = ConfiguracaoMensageria.FILA_RELATORIO_SOLICITADO)
    public void receber(RelatorioSolicitadoEvento evento) {
        if (evento.idRelatorio() == null) {
            throw new IllegalArgumentException("Pedido de relatório sem idRelatorio");
        }
        geradorRelatorioService.gerar(evento.idRelatorio());
    }
}
