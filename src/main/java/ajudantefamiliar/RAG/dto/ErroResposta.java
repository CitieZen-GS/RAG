package ajudantefamiliar.RAG.dto;

import java.time.Instant;
import java.util.List;

public record ErroResposta(
        Instant momento,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<CampoInvalido> campos
) {

    public record CampoInvalido(String campo, String mensagem) {
    }
}
