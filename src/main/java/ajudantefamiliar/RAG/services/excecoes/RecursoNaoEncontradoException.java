package ajudantefamiliar.RAG.services.excecoes;

/** Lançada quando um registro buscado pelo id não existe (vira HTTP 404). */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, Long id) {
        super(recurso + " com id " + id + " não encontrado(a)");
    }
}
