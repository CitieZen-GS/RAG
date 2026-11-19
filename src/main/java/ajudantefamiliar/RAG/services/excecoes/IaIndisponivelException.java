package ajudantefamiliar.RAG.services.excecoes;

/** Lançada quando a IA (OpenAI) não respondeu: chave inválida, limite de uso, fora do ar (vira HTTP 503). */
public class IaIndisponivelException extends RuntimeException {

    public IaIndisponivelException(Throwable causa) {
        super("O serviço de IA não respondeu. Tente novamente em instantes.", causa);
    }
}
