package ajudantefamiliar.RAG;

import ajudantefamiliar.RAG.apoio.Dados;
import ajudantefamiliar.RAG.models.enums.TipoRelatorio;
import ajudantefamiliar.RAG.models.funcoes.Idoso;
import ajudantefamiliar.RAG.repositories.*;
import ajudantefamiliar.RAG.services.LinhaDoTempoService;
import ajudantefamiliar.RAG.services.OrigemPeriodo;
import ajudantefamiliar.RAG.services.Periodo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/** Montagem do contexto (linha do tempo) enviado à IA. */
class LinhaDoTempoServiceTests {

    private static final LocalDate DIA = LocalDate.of(2026, 11, 10);
    private static final Periodo NOVEMBRO = new Periodo(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
            OrigemPeriodo.INFORMADO);

    private DoencaRepository doencas;
    private MedicamentoRepository medicamentos;
    private AlimentacaoRepository alimentacoes;
    private AtividadeRepository atividades;
    private LembreteConsultaRepository consultas;
    private LinhaDoTempoService servico;
    private final Idoso jose = Dados.idoso(1L, 1L);

    @BeforeEach
    void preparar() {
        doencas = mock(DoencaRepository.class);
        medicamentos = mock(MedicamentoRepository.class);
        alimentacoes = mock(AlimentacaoRepository.class);
        atividades = mock(AtividadeRepository.class);
        consultas = mock(LembreteConsultaRepository.class);
        servico = new LinhaDoTempoService(doencas, medicamentos, alimentacoes, atividades, consultas,
                Dados.propriedades());

        when(doencas.findByIdIdosoOrderByNomeAsc(1L)).thenReturn(List.of(Dados.doenca("Diabetes")));
        var metformina = Dados.medicamento("Metformina", DIA.atTime(8, 0));
        when(medicamentos.findByIdIdosoAndHoraBetweenOrderByHoraAsc(anyLong(), any(), any()))
                .thenReturn(List.of(metformina));
        when(alimentacoes.findByIdIdosoAndDataConsumoBetweenOrderByHoraConsumoAsc(anyLong(), any(), any()))
                .thenReturn(List.of(Dados.alimentacao("Sopa", "Almoco", DIA.atTime(12, 0))));
        when(atividades.findByIdIdosoAndDataBetweenOrderByDataAscHoraInicioAsc(anyLong(), any(), any()))
                .thenReturn(List.of(Dados.atividade("Caminhada", DIA)));
        when(consultas.findByIdIdosoAndDataConsultaBetweenOrderByDataConsultaAscHoraConsultaAsc(anyLong(), any(), any()))
                .thenReturn(List.of(Dados.consulta("Retorno", LocalDateTime.of(2026, 11, 20, 14, 0), metformina)));
    }

    @Test
    void contextoGeralTrazDadosDoIdosoERegistrosEmOrdemCronologica() {
        String texto = servico.montar(jose, NOVEMBRO, TipoRelatorio.GERAL);

        assertThat(texto).contains("Nome: Jose", "Altura: 1,7 m", "Peso: 70,5 kg", "Diabetes (tipo 2)",
                "Período consultado: 01/11/2026 a 30/11/2026");
        assertThat(texto).contains("10/11/2026 12:00 [Alimentação] Almoço: Sopa (350,5 kcal)",
                "10/11/2026 (sem horário) [Atividade] Caminhada",
                "[Consulta] Retorno com Dr Silva (Pendente); medicamentos ligados: Metformina");
        // atividade sem horário (00:00) < medicamento 08:00 < almoço 12:00 < consulta dia 20
        assertThat(texto.indexOf("[Atividade]")).isLessThan(texto.indexOf("[Medicamento]"));
        assertThat(texto.indexOf("[Medicamento]")).isLessThan(texto.indexOf("[Alimentação]"));
        assertThat(texto.indexOf("[Alimentação]")).isLessThan(texto.indexOf("[Consulta]"));
    }

    @Test
    void escopoDeAssuntoBuscaSoAquelaTabela() {
        String texto = servico.montar(jose, NOVEMBRO, TipoRelatorio.MEDICAMENTOS);

        assertThat(texto).contains("[Medicamento] Metformina").doesNotContain("[Alimentação]", "[Atividade]");
        verify(alimentacoes, never()).findByIdIdosoAndDataConsumoBetweenOrderByHoraConsumoAsc(anyLong(), any(), any());
        verify(consultas, never())
                .findByIdIdosoAndDataConsultaBetweenOrderByDataConsultaAscHoraConsultaAsc(anyLong(), any(), any());
    }

    @Test
    void medicamentosSaoBuscadosDoInicioAoFimDoPeriodoComHorario() {
        servico.montar(jose, NOVEMBRO, TipoRelatorio.MEDICAMENTOS);

        verify(medicamentos).findByIdIdosoAndHoraBetweenOrderByHoraAsc(eq(1L),
                eq(LocalDate.of(2026, 11, 1).atStartOfDay()),
                argThat(fim -> fim.toLocalDate().equals(LocalDate.of(2026, 11, 30)) && fim.getHour() == 23));
    }

    @Test
    void periodoSemRegistrosAvisaAIa() {
        when(medicamentos.findByIdIdosoAndHoraBetweenOrderByHoraAsc(anyLong(), any(), any())).thenReturn(List.of());

        assertThat(servico.montar(jose, NOVEMBRO, TipoRelatorio.MEDICAMENTOS)).contains("Nenhum registro no período.");
    }
}
