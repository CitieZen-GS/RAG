package ajudantefamiliar.RAG;

import ajudantefamiliar.RAG.apoio.Dados;
import ajudantefamiliar.RAG.apoio.IaFalsa;
import ajudantefamiliar.RAG.models.Relatorio;
import ajudantefamiliar.RAG.models.enums.StatusRelatorio;
import ajudantefamiliar.RAG.models.enums.TipoRelatorio;
import ajudantefamiliar.RAG.repositories.RelatorioRepository;
import ajudantefamiliar.RAG.services.GeradorRelatorioService;
import ajudantefamiliar.RAG.services.LinhaDoTempoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Geração do relatório pelo consumidor da fila. */
class GeradorRelatorioServiceTests {

    private final IaFalsa ia = new IaFalsa();
    private RelatorioRepository relatorios;
    private LinhaDoTempoService linhaDoTempo;
    private GeradorRelatorioService gerador;
    private Relatorio relatorio;

    @BeforeEach
    void preparar() {
        relatorios = mock(RelatorioRepository.class);
        linhaDoTempo = mock(LinhaDoTempoService.class);
        gerador = new GeradorRelatorioService(relatorios, linhaDoTempo, ChatClient.create(ia), Dados.propriedades());

        relatorio = new Relatorio();
        relatorio.setId(3L);
        relatorio.setIdoso(Dados.idoso(10L, 1L));
        relatorio.setTipo(TipoRelatorio.MEDICAMENTOS);
        relatorio.setTitulo("Medicamentos de novembro");
        relatorio.setSolicitacao("Destaque dias sem registro");
        relatorio.setInicioPeriodo(LocalDate.of(2026, 11, 1));
        relatorio.setFimPeriodo(LocalDate.of(2026, 11, 18));
        when(relatorios.findComIdosoById(3L)).thenReturn(Optional.of(relatorio));
        when(linhaDoTempo.montar(any(), any(), eq(TipoRelatorio.MEDICAMENTOS))).thenReturn("- registros\n");
    }

    @Test
    void geraOConteudoEMarcaConcluido() {
        ia.responde("# Medicamentos de novembro\n\nResumo...");

        gerador.gerar(3L);

        assertThat(relatorio.getStatus()).isEqualTo(StatusRelatorio.CONCLUIDO);
        assertThat(relatorio.getConteudo()).startsWith("# Medicamentos de novembro");
        assertThat(relatorio.getDataConclusao()).isNotNull();
        assertThat(ia.promptsRecebidos().getFirst().getContents()).contains("Pontos de atenção", "Destaque dias sem registro");
        verify(relatorios).save(relatorio);
    }

    @Test
    void falhaDaIaMarcaErroComOMotivo() {
        ia.falha(new RuntimeException("chave inválida"));

        gerador.gerar(3L);

        assertThat(relatorio.getStatus()).isEqualTo(StatusRelatorio.ERRO);
        assertThat(relatorio.getErro()).contains("chave inválida");
        verify(relatorios).save(relatorio);
    }

    @Test
    void mensagemRepetidaDeRelatorioJaConcluidoEhIgnorada() {
        relatorio.setStatus(StatusRelatorio.CONCLUIDO);

        gerador.gerar(3L);

        assertThat(ia.promptsRecebidos()).isEmpty();
        verify(relatorios, never()).save(any());
    }
}
