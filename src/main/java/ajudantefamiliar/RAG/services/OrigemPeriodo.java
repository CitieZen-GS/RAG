package ajudantefamiliar.RAG.services;

/** De onde veio o período consultado numa pergunta do chat. */
public enum OrigemPeriodo {
    /** O app mandou dataInicio e dataFim. */
    INFORMADO,
    /** A IA identificou na pergunta ("ontem", "semana passada"...). */
    IDENTIFICADO_PELA_IA,
    /** Nenhuma pista: últimos N dias (rag.periodo-padrao-dias). */
    PADRAO
}
