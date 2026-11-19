package ajudantefamiliar.RAG.services;

import ajudantefamiliar.RAG.configuracoes.PropriedadesRag;
import ajudantefamiliar.RAG.services.excecoes.RegraNegocioException;
import ajudantefamiliar.RAG.services.ia.PeriodoIdentificado;
import ajudantefamiliar.RAG.services.ia.PromptsIa;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Define o período de busca de uma pergunta do chat, nesta ordem:
 * <ol>
 *   <li>datas enviadas pelo app (dataInicio e dataFim) → INFORMADO;</li>
 *   <li>a IA lê a pergunta ("ontem", "semana passada"...) → IDENTIFICADO_PELA_IA;</li>
 *   <li>sem pista, ou se a IA falhar → últimos {@code rag.periodo-padrao-dias} dias → PADRAO.</li>
 * </ol>
 * Períodos maiores que {@code rag.periodo-maximo-dias} são recusados (datas do app) ou encurtados (IA).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PeriodoService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final ChatClient chatClient;
    private final PropriedadesRag propriedades;

    public Periodo definir(String pergunta, String perguntaAnterior, LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio != null || dataFim != null) {
            return informado(dataInicio, dataFim);
        }
        LocalDate hoje = hoje();
        try {
            PeriodoIdentificado identificado = chatClient.prompt()
                    .system(PromptsIa.periodo(hoje.format(DateTimeFormatter.ISO_LOCAL_DATE),
                            hoje.getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR)))
                    .user(perguntaAnterior == null ? "Pergunta: " + pergunta
                            : "Pergunta anterior: " + perguntaAnterior + "\nPergunta atual: " + pergunta)
                    .call()
                    .entity(PeriodoIdentificado.class);
            if (identificado != null && identificado.encontrado()
                    && identificado.inicio() != null && identificado.fim() != null) {
                return ajustar(identificado.inicio(), identificado.fim());
            }
        } catch (RuntimeException e) {
            log.warn("Não foi possível identificar o período pela IA; usando o padrão: {}", e.getMessage());
        }
        return padrao();
    }

    /** Valida um período informado pelo usuário (app ou pedido de relatório). */
    public Periodo informado(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            throw new RegraNegocioException("Informe dataInicio e dataFim juntas");
        }
        if (fim.isBefore(inicio)) {
            throw new RegraNegocioException("dataFim não pode ser anterior a dataInicio");
        }
        Periodo periodo = new Periodo(inicio, fim, OrigemPeriodo.INFORMADO);
        if (periodo.dias() > propriedades.periodoMaximoDias()) {
            throw new RegraNegocioException("O período pode ter no máximo " + propriedades.periodoMaximoDias() + " dias");
        }
        return periodo;
    }

    public LocalDate hoje() {
        return LocalDate.now(propriedades.zona());
    }

    private Periodo ajustar(LocalDate inicio, LocalDate fim) {
        if (fim.isBefore(inicio)) {
            LocalDate troca = inicio;
            inicio = fim;
            fim = troca;
        }
        int maximo = propriedades.periodoMaximoDias();
        if (new Periodo(inicio, fim, OrigemPeriodo.IDENTIFICADO_PELA_IA).dias() > maximo) {
            inicio = fim.minusDays(maximo - 1L);
        }
        return new Periodo(inicio, fim, OrigemPeriodo.IDENTIFICADO_PELA_IA);
    }

    private Periodo padrao() {
        LocalDate hoje = hoje();
        return new Periodo(hoje.minusDays(propriedades.periodoPadraoDias() - 1L), hoje, OrigemPeriodo.PADRAO);
    }
}
