package ajudantefamiliar.RAG.configuracoes;

import ajudantefamiliar.RAG.services.TokensRevogadosService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Valida o JWT emitido pela API de autenticação (HS256, segredo compartilhado), igual à API de funções.
 * Tokens revogados no logout (recebidos via RabbitMQ) são recusados com 401.
 * <p>
 * Chat e relatórios são só do CUIDADOR: token de FAMILIAR recebe 403 em qualquer rota.
 * Além disso, cada rota confere se o idoso pertence ao cuidador do token (AcessoIdosoService).
 */
@Configuration
public class ConfiguracaoSeguranca {

    @Value("${jwt.segredo}")
    private String segredo;

    @Bean
    public SecurityFilterChain filtroDeSeguranca(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(regras -> regras
                        .requestMatchers("/error").permitAll()
                        // Documentação Swagger é pública; as rotas da API continuam exigindo token
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().hasRole("CUIDADOR"))
                .oauth2ResourceServer(servidor -> servidor
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDePerfis())));
        return http.build();
    }

    @Bean
    public JwtDecoder decodificadorJwt(TokensRevogadosService tokensRevogadosService) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.segredo precisa ter pelo menos 32 caracteres (256 bits) para HS256");
        }
        SecretKeySpec chave = new SecretKeySpec(bytes, "HmacSHA256");
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
        // Validações padrão (exp, nbf) + recusar tokens revogados no logout (avisados via RabbitMQ)
        decodificador.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), validadorTokenRevogado(tokensRevogadosService)));
        return decodificador;
    }

    private OAuth2TokenValidator<Jwt> validadorTokenRevogado(TokensRevogadosService tokensRevogadosService) {
        OAuth2Error erro = new OAuth2Error("invalid_token", "Token revogado (logout)", null);
        return jwt -> tokensRevogadosService.estaRevogado(jwt.getId())
                ? OAuth2TokenValidatorResult.failure(erro)
                : OAuth2TokenValidatorResult.success();
    }

    /** Converte a claim "roles" em ROLE_CUIDADOR / ROLE_FAMILIAR. */
    private JwtAuthenticationConverter conversorDePerfis() {
        JwtGrantedAuthoritiesConverter perfis = new JwtGrantedAuthoritiesConverter();
        perfis.setAuthoritiesClaimName("roles");
        perfis.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(perfis);
        return conversor;
    }
}
