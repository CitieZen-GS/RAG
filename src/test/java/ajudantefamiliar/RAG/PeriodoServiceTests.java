package ajudantefamiliar.RAG;

import ajudantefamiliar.RAG.apoio.Dados;
import ajudantefamiliar.RAG.apoio.IaFalsa;
import ajudantefamiliar.RAG.services.OrigemPeriodo;
import ajudantefamiliar.RAG.services.Periodo;
import ajudantefamiliar.RAG.services.PeriodoService;
import ajudantefamiliar.RAG.services.excecoes.RegraNegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Definição do período de uma pergunta: datas do app, IA ou padrão. */
class PeriodoServiceTests {

    private final IaFalsa ia = new IaFalsa();
    private final PeriodoService servico = new PeriodoService(ChatClient.create(ia), Dados.propriedades());

    @Test
    void datasDoAppTemPrioridadeENaoChamamAIa() {
        Periodo periodo = servico.definir("e ontem?", null, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10));

        assertThat(periodo).isEqualTo(new Periodo(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10),
                OrigemPeriodo.INFORMADO));
        assertThat(ia.promptsRecebidos()).isEmpty();
    }

    @Test
    void periodoIdentificadoPelaIa() {
        ia.responde("{\"encontrado\": true, \"inicio\": \"2026-11-17\", \"fim\": \"2026-11-17\"}");

        Periodo periodo = servico.definir("O que ele comeu ontem?", null, null, null);

        assertThat(periodo).isEqualTo(new Periodo(LocalDate.of(2026, 11, 17), LocalDate.of(2026, 11, 17),
                OrigemPeriodo.IDENTIFICADO_PELA_IA));
        assertThat(ia.promptsRecebidos().getFirst().getContents()).contains("Hoje é", "O que ele comeu ontem?");
    }

    @Test
    void perguntaSemPeriodoUsaOsUltimos7Dias() {
        ia.responde("{\"encontrado\": false}");

        Periodo periodo = servico.definir("Como ele está?", null, null, null);

        assertThat(periodo.origem()).isEqualTo(OrigemPeriodo.PADRAO);
        assertThat(periodo.fim()).isEqualTo(servico.hoje());
        assertThat(periodo.dias()).isEqualTo(7);
    }

    @Test
    void falhaDaIaUsaOPadraoEmVezDeQuebrarOChat() {
        ia.falha(new RuntimeException("OpenAI fora do ar"));

        assertThat(servico.definir("ontem?", null, null, null).origem()).isEqualTo(OrigemPeriodo.PADRAO);
    }

    @Test
    void periodoDaIaMaiorQue90DiasEhEncurtado() {
        ia.responde("{\"encontrado\": true, \"inicio\": \"2025-01-01\", \"fim\": \"2026-11-18\"}");

        Periodo periodo = servico.definir("desde o ano passado", null, null, null);

        assertThat(periodo.dias()).isEqualTo(90);
        assertThat(periodo.fim()).isEqualTo(LocalDate.of(2026, 11, 18));
    }

    @Test
    void datasDoAppInvalidasSaoRecusadas() {
        assertThatThrownBy(() -> servico.informado(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1)))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> servico.informado(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 11, 1)))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("90 dias");
        assertThatThrownBy(() -> servico.definir("x", null, LocalDate.of(2026, 1, 1), null))
                .isInstanceOf(RegraNegocioException.class);
    }
}
