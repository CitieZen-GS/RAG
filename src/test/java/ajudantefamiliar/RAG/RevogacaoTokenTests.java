package ajudantefamiliar.RAG;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import ajudantefamiliar.RAG.configuracoes.ConfiguracaoSeguranca;
import ajudantefamiliar.RAG.mensageria.AcessoRevogadoEvento;
import ajudantefamiliar.RAG.mensageria.ConsumidorAcessoRevogado;
import ajudantefamiliar.RAG.services.TokensRevogadosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Revogação de token (logout) recebida pelo RabbitMQ: depois do evento, o mesmo JWT passa a ser recusado.
 * Não sobe Spring nem containers: roda rápido e também no CI.
 */
class RevogacaoTokenTests {

    private static final String SEGREDO = "segredo-de-teste-com-pelo-menos-32-caracteres";

    private TokensRevogadosService tokensRevogadosService;
    private ConsumidorAcessoRevogado consumidor;
    private JwtDecoder decodificador;

    @BeforeEach
    void preparar() {
        tokensRevogadosService = new TokensRevogadosService();
        consumidor = new ConsumidorAcessoRevogado(tokensRevogadosService);
        ConfiguracaoSeguranca configuracao = new ConfiguracaoSeguranca();
        ReflectionTestUtils.setField(configuracao, "segredo", SEGREDO);
        decodificador = configuracao.decodificadorJwt(tokensRevogadosService);
    }

    @Test
    void tokenValidoEhAceito() {
        String token = gerarToken("jti-1", Instant.now().plus(10, ChronoUnit.MINUTES));
        assertThat(decodificador.decode(token).getId()).isEqualTo("jti-1");
    }

    @Test
    void tokenRevogadoNoLogoutEhRecusado() {
        Instant expiraEm = Instant.now().plus(10, ChronoUnit.MINUTES);
        String token = gerarToken("jti-2", expiraEm);

        consumidor.receber(new AcessoRevogadoEvento("jti-2", 1L, "CUIDADOR", expiraEm, Instant.now(), "LOGOUT"));

        assertThatThrownBy(() -> decodificador.decode(token))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("Token revogado");
    }

    @Test
    void revogarUmTokenNaoAfetaOutros() {
        Instant expiraEm = Instant.now().plus(10, ChronoUnit.MINUTES);
        String outroToken = gerarToken("jti-outro", expiraEm);

        consumidor.receber(new AcessoRevogadoEvento("jti-3", 1L, "CUIDADOR", expiraEm, Instant.now(), "LOGOUT"));

        assertThat(decodificador.decode(outroToken).getId()).isEqualTo("jti-outro");
    }

    @Test
    void tokenSemJtiContinuaAceito() {
        String token = gerarToken(null, Instant.now().plus(10, ChronoUnit.MINUTES));
        assertThat(decodificador.decode(token).getSubject()).isEqualTo("1");
    }

    @Test
    void eventoDeTokenJaExpiradoNaoOcupaMemoria() {
        tokensRevogadosService.revogar("jti-velho", Instant.now().minus(1, ChronoUnit.MINUTES));
        assertThat(tokensRevogadosService.estaRevogado("jti-velho")).isFalse();
    }

    @Test
    void eventoSemJtiEhRejeitadoParaIrParaFilaDeFalhas() {
        assertThatThrownBy(() -> consumidor.receber(
                new AcessoRevogadoEvento(null, 1L, "CUIDADOR", Instant.now(), Instant.now(), "LOGOUT")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Gera um JWT igual ao da API de autenticação (HS256, claims sub, roles, jti, exp). */
    private String gerarToken(String jti, Instant expiraEm) {
        SecretKeySpec chave = new SecretKeySpec(SEGREDO.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject("1")
                .claim("roles", List.of("CUIDADOR"))
                .issuedAt(Instant.now())
                .expiresAt(expiraEm);
        if (jti != null) {
            claims.id(jti);
        }
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
                .getTokenValue();
    }
}
