package ajudantefamiliar.RAG.mensageria;

/**
 * Pedido de geração de relatório. O RAG publica e ele mesmo consome (fila rag.relatorio-solicitado):
 * assim a rota responde na hora e a IA trabalha em segundo plano.
 *
 * @param idRelatorio relatório já gravado com status GERANDO
 */
public record RelatorioSolicitadoEvento(Long idRelatorio) {
}
