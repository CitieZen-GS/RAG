package ajudantefamiliar.RAG.services.excecoes;

/** Lançada quando o usuário logado não pode acessar aquele registro (vira HTTP 403). */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
