package ajudantefamiliar.RAG.configuracoes;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.ZoneId;

/**
 * Configurações do RAG (prefixo "rag" no application.yaml).
 *
 * @param fusoHorario         fuso usado para "hoje", "ontem" etc. (o servidor no Azure roda em UTC)
 * @param periodoPadraoDias   período usado quando a pergunta não indica nenhum (ex.: últimos 7 dias)
 * @param periodoMaximoDias   maior período aceito numa pergunta ou relatório (limite de texto da IA)
 * @param historicoMensagens  quantas mensagens anteriores da conversa vão junto, como memória
 * @param limiteRegistros     máximo de registros da linha do tempo enviados à IA (os mais recentes)
 */
@ConfigurationProperties("rag")
public record PropriedadesRag(
        String fusoHorario,
        int periodoPadraoDias,
        int periodoMaximoDias,
        int historicoMensagens,
        int limiteRegistros
) {

    public ZoneId zona() {
        return ZoneId.of(fusoHorario);
    }
}
