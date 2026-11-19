package ajudantefamiliar.RAG.services;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Intervalo de datas (inclusive nas duas pontas) usado para buscar os registros do idoso. */
public record Periodo(LocalDate inicio, LocalDate fim, OrigemPeriodo origem) {

    public long dias() {
        return ChronoUnit.DAYS.between(inicio, fim) + 1;
    }
}
