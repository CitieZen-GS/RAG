package ajudantefamiliar.RAG.dto;

import ajudantefamiliar.RAG.services.OrigemPeriodo;
import ajudantefamiliar.RAG.services.Periodo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Período cujos registros foram usados na resposta")
public record PeriodoResposta(

        LocalDate inicio,

        LocalDate fim,

        @Schema(description = "INFORMADO (datas do app), IDENTIFICADO_PELA_IA ou PADRAO (últimos 7 dias)")
        OrigemPeriodo origem
) {

    public static PeriodoResposta de(Periodo periodo) {
        return new PeriodoResposta(periodo.inicio(), periodo.fim(), periodo.origem());
    }
}
