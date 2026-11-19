package ajudantefamiliar.RAG;

import ajudantefamiliar.RAG.models.funcoes.Idoso;
import ajudantefamiliar.RAG.repositories.IdosoRepository;
import ajudantefamiliar.RAG.services.AcessoIdosoService;
import ajudantefamiliar.RAG.services.excecoes.AcessoNegadoException;
import ajudantefamiliar.RAG.services.excecoes.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regra de dono: o cuidador só usa o chat e os relatórios dos idosos que acompanha.
 * Não sobe Spring nem containers.
 */
class AcessoIdosoServiceTests {

    private IdosoRepository idosoRepository;
    private AcessoIdosoService acessoIdosoService;

    @BeforeEach
    void preparar() {
        idosoRepository = mock(IdosoRepository.class);
        acessoIdosoService = new AcessoIdosoService(idosoRepository);

        Idoso jose = mock(Idoso.class);
        when(jose.getId()).thenReturn(10L);
        when(jose.getIdCuidador()).thenReturn(1L);
        when(idosoRepository.findById(10L)).thenReturn(Optional.of(jose));
        when(idosoRepository.findById(99L)).thenReturn(Optional.empty());
    }

    @Test
    void cuidadorDoIdosoTemAcesso() {
        assertThat(acessoIdosoService.buscarIdosoDoCuidador(10L, tokenDoCuidador(1L)).getId()).isEqualTo(10L);
    }

    @Test
    void outroCuidadorRecebeAcessoNegado() {
        assertThatThrownBy(() -> acessoIdosoService.buscarIdosoDoCuidador(10L, tokenDoCuidador(2L)))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void idosoInexistenteRecebeNaoEncontrado() {
        assertThatThrownBy(() -> acessoIdosoService.buscarIdosoDoCuidador(99L, tokenDoCuidador(1L)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private Jwt tokenDoCuidador(Long idCuidador) {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(String.valueOf(idCuidador))
                .claim("roles", List.of("CUIDADOR"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
    }
}
