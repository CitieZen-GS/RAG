package ajudantefamiliar.RAG.dto;

import ajudantefamiliar.RAG.models.enums.TipoRelatorio;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Pedido de relatório sobre o idoso")
public record RelatorioRequisicao(

        @Schema(description = "GERAL (tudo) ou um assunto: MEDICAMENTOS, ALIMENTACAO, ATIVIDADES, CONSULTAS",
                example = "GERAL")
        @NotNull
        TipoRelatorio tipo,

        @Schema(example = "2026-11-01")
        @NotNull
        LocalDate dataInicio,

        @Schema(description = "No máximo 90 dias depois de dataInicio", example = "2026-11-18")
        @NotNull
        LocalDate dataFim,

        @Schema(description = "Opcional. Sem título, é gerado um automático.", example = "Relatório para a consulta")
        @Size(max = 100)
        String titulo,

        @Schema(description = "Opcional. Instrução livre para a IA.",
                example = "Destaque se houve dias sem registro de medicamentos")
        @Size(max = 500)
        String solicitacao
) {
}
