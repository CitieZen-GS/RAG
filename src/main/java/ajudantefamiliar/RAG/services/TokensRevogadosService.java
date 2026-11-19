package ajudantefamiliar.RAG.services;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lista dos tokens de acesso revogados antes de expirar (jti -> expiração), alimentada pelo RabbitMQ.
 * <p>
 * Fica em memória: um token só precisa ficar aqui até expirar (no máximo a validade do token de acesso,
 * 60 minutos por padrão). Se a API reiniciar, a lista começa vazia e esses tokens voltam a valer até
 * expirar, o mesmo comportamento de antes da mensageria.
 */
@Service
public class TokensRevogadosService {

    private final Map<String, Instant> revogados = new ConcurrentHashMap<>();

    public void revogar(String jti, Instant expiraEm) {
        removerExpirados();
        if (expiraEm.isAfter(Instant.now())) {
            revogados.put(jti, expiraEm);
        }
    }

    public boolean estaRevogado(String jti) {
        return jti != null && revogados.containsKey(jti);
    }

    /** Tokens já expirados são recusados pela validação de "exp": não precisam mais ocupar memória. */
    private void removerExpirados() {
        Instant agora = Instant.now();
        revogados.values().removeIf(expiraEm -> expiraEm.isBefore(agora));
    }
}
