package ajudantefamiliar.RAG.services.ia;

import java.time.LocalDate;

/**
 * Resposta estruturada (JSON) da IA ao identificar o período de uma pergunta.
 *
 * @param encontrado false quando a pergunta não indica período nenhum
 */
public record PeriodoIdentificado(boolean encontrado, LocalDate inicio, LocalDate fim) {
}
