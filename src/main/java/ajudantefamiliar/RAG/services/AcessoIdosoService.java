package ajudantefamiliar.RAG.services;

import ajudantefamiliar.RAG.models.funcoes.Idoso;
import ajudantefamiliar.RAG.repositories.IdosoRepository;
import ajudantefamiliar.RAG.services.excecoes.AcessoNegadoException;
import ajudantefamiliar.RAG.services.excecoes.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confere se o idoso pertence ao cuidador do token. Toda rota do chat e dos relatórios
 * chama este service antes de ler qualquer dado do idoso.
 * <p>
 * O papel (só CUIDADOR) já é exigido pela ConfiguracaoSeguranca; aqui é a regra de dono:
 * o "sub" do JWT de um cuidador é o id_cuidador.
 */
@Service
@RequiredArgsConstructor
public class AcessoIdosoService {

    private final IdosoRepository idosoRepository;

    @Transactional(readOnly = true)
    public Idoso buscarIdosoDoCuidador(Long idIdoso, Jwt jwt) {
        Idoso idoso = idosoRepository.findById(idIdoso)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Idoso", idIdoso));
        if (!idoso.getIdCuidador().equals(idCuidador(jwt))) {
            throw new AcessoNegadoException("Este idoso não é acompanhado pelo cuidador logado");
        }
        return idoso;
    }

    /** id_cuidador do token (claim "sub"). */
    public Long idCuidador(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
