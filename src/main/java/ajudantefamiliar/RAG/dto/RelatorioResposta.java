package ajudantefamiliar.RAG.dto;

import ajudantefamiliar.RAG.models.Relatorio;
import ajudantefamiliar.RAG.models.enums.StatusRelatorio;
import ajudantefamiliar.RAG.models.enums.TipoRelatorio;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RelatorioResposta(
        Long id,
        Long idIdoso,
        TipoRelatorio tipo,
        String titulo,
        String solicitacao,
        LocalDate dataInicio,
        LocalDate dataFim,
        @Schema(description = "GERANDO (aguarde e consulte de novo), CONCLUIDO ou ERRO")
        StatusRelatorio status,
        @Schema(description = "Texto em Markdown. Nulo enquanto GERANDO; nas listagens não é enviado.")
        String conteudo,
        @Schema(description = "Motivo, quando status = ERRO")
        String erro,
        LocalDateTime dataCriacao,
        LocalDateTime dataConclusao
) {

    public static RelatorioResposta de(Relatorio r) {
        return new RelatorioResposta(r.getId(), r.getIdoso().getId(), r.getTipo(), r.getTitulo(), r.getSolicitacao(),
                r.getInicioPeriodo(), r.getFimPeriodo(), r.getStatus(), r.getConteudo(), r.getErro(),
                r.getDataCriacao(), r.getDataConclusao());
    }

    /** Para listagens: sem o conteúdo (pode ser grande). */
    public static RelatorioResposta resumo(Relatorio r) {
        return new RelatorioResposta(r.getId(), r.getIdoso().getId(), r.getTipo(), r.getTitulo(), r.getSolicitacao(),
                r.getInicioPeriodo(), r.getFimPeriodo(), r.getStatus(), null, r.getErro(),
                r.getDataCriacao(), r.getDataConclusao());
    }
}
