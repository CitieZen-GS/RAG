package ajudantefamiliar.RAG.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Pergunta do cuidador sobre o idoso")
public record PerguntaRequisicao(

        @Schema(example = "O que o José almoçou ontem e tomou os remédios da manhã?")
        @NotBlank @Size(max = 2000)
        String pergunta,

        @Schema(description = "Opcional. Com dataFim, define o período consultado. Sem as duas, a IA identifica "
                + "o período na pergunta (ou usa os últimos 7 dias).", example = "2026-11-18")
        LocalDate dataInicio,

        @Schema(description = "Opcional. Ver dataInicio.", example = "2026-11-18")
        LocalDate dataFim
) {
}
