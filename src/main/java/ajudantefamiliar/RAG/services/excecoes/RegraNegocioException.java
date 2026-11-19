package ajudantefamiliar.RAG.services.excecoes;

/** Lançada quando uma regra de negócio é violada, ex.: período inválido (vira HTTP 422). */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
