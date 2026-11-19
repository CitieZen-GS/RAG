package ajudantefamiliar.RAG.services;

import ajudantefamiliar.RAG.configuracoes.PropriedadesRag;
import ajudantefamiliar.RAG.models.Relatorio;
import ajudantefamiliar.RAG.models.enums.StatusRelatorio;
import ajudantefamiliar.RAG.repositories.RelatorioRepository;
import ajudantefamiliar.RAG.services.ia.PromptsIa;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Gera o texto do relatório com a IA (chamado pelo consumidor da fila rag.relatorio-solicitado).
 * <p>
 * Idempotente: se o relatório não existir mais ou não estiver GERANDO (mensagem repetida), não faz nada.
 * Falha da IA ou da montagem do contexto vira status ERRO com o motivo; não é tentada de novo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeradorRelatorioService {

    private static final int TAMANHO_ERRO = 400;

    private final RelatorioRepository relatorioRepository;
    private final LinhaDoTempoService linhaDoTempoService;
    private final ChatClient chatClient;
    private final PropriedadesRag propriedades;

    public void gerar(Long idRelatorio) {
        Relatorio relatorio = relatorioRepository.findComIdosoById(idRelatorio).orElse(null);
        if (relatorio == null || relatorio.getStatus() != StatusRelatorio.GERANDO) {
            log.info("Pedido de relatório {} ignorado (removido ou já processado)", idRelatorio);
            return;
        }
        try {
            Periodo periodo = new Periodo(relatorio.getInicioPeriodo(), relatorio.getFimPeriodo(),
                    OrigemPeriodo.INFORMADO);
            String contexto = linhaDoTempoService.montar(relatorio.getIdoso(), periodo, relatorio.getTipo());
            String conteudo = chatClient.prompt()
                    .system(PromptsIa.relatorio(contexto))
                    .user(pedido(relatorio))
                    .call()
                    .content();
            if (conteudo == null || conteudo.isBlank()) {
                throw new IllegalStateException("A IA devolveu um relatório vazio");
            }
            relatorio.setConteudo(conteudo);
            relatorio.setStatus(StatusRelatorio.CONCLUIDO);
            relatorio.setErro(null);
            log.info("Relatório {} concluído", idRelatorio);
        } catch (RuntimeException e) {
            log.error("Falha ao gerar o relatório {}", idRelatorio, e);
            relatorio.setStatus(StatusRelatorio.ERRO);
            relatorio.setErro(resumir("Não foi possível gerar o relatório: " + e.getMessage()));
        }
        relatorio.setDataConclusao(LocalDateTime.now(propriedades.zona()));
        relatorioRepository.save(relatorio);
    }

    private String pedido(Relatorio relatorio) {
        String pedido = "Escreva o relatório \"" + relatorio.getTitulo() + "\" (assunto: " + relatorio.getTipo()
                + ") com os registros do contexto.";
        if (relatorio.getSolicitacao() != null && !relatorio.getSolicitacao().isBlank()) {
            pedido += "\nPedido do cuidador: " + relatorio.getSolicitacao();
        }
        return pedido;
    }

    private String resumir(String texto) {
        return texto.length() <= TAMANHO_ERRO ? texto : texto.substring(0, TAMANHO_ERRO - 3) + "...";
    }
}
