package ajudantefamiliar.RAG.mensageria;

import java.time.Instant;

/**
 * Mensagem recebida da API de autenticação quando um token de acesso deixa de valer antes de expirar
 * (ex.: logout). Contrato igual ao record de mesmo nome na API de autenticação.
 *
 * @param jti         id único do JWT (claim "jti")
 * @param idUsuario   claim "sub": id_cuidador ou id_parente
 * @param tipoUsuario CUIDADOR ou FAMILIAR
 * @param expiraEm    claim "exp": depois disso o token já é recusado de qualquer forma
 * @param revogadoEm  quando o acesso foi revogado
 * @param motivo      por enquanto só LOGOUT
 */
public record AcessoRevogadoEvento(String jti, Long idUsuario, String tipoUsuario, Instant expiraEm,
                                   Instant revogadoEm, String motivo) {
}
