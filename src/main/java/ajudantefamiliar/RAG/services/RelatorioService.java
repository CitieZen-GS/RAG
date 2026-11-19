package ajudantefamiliar.RAG.services;

import ajudantefamiliar.RAG.configuracoes.PropriedadesRag;
import ajudantefamiliar.RAG.dto.RelatorioRequisicao;
import ajudantefamiliar.RAG.dto.RelatorioResposta;
import ajudantefamiliar.RAG.mensageria.RelatorioSolicitadoEvento;
import ajudantefamiliar.RAG.models.Relatorio;
import ajudantefamiliar.RAG.models.enums.StatusRelatorio;
import ajudantefamiliar.RAG.models.funcoes.Idoso;
import ajudantefamiliar.RAG.repositories.CuidadorRepository;
import ajudantefamiliar.RAG.repositories.RelatorioRepository;
import ajudantefamiliar.RAG.services.excecoes.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Pedidos e consultas de relatórios. A geração em si é feita em segundo plano pelo GeradorRelatorioService,
 * acionado pela fila rag.relatorio-solicitado.
 */
@Service
@RequiredArgsConstructor
public class RelatorioService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AcessoIdosoService acessoIdosoService;
    private final PeriodoService periodoService;
    private final RelatorioRepository relatorioRepository;
    private final CuidadorRepository cuidadorRepository;
    private final ApplicationEventPublisher publicadorDeEventos;
    private final PropriedadesRag propriedades;

    /** Grava o relatório como GERANDO e pede a geração (o evento só sai depois do commit). */
    @Transactional
    public RelatorioResposta solicitar(Long idIdoso, Jwt jwt, RelatorioRequisicao requisicao) {
        Idoso idoso = acessoIdosoService.buscarIdosoDoCuidador(idIdoso, jwt);
        Periodo periodo = periodoService.informado(requisicao.dataInicio(), requisicao.dataFim());

        Relatorio relatorio = new Relatorio();
        relatorio.setCuidador(cuidadorRepository.getReferenceById(acessoIdosoService.idCuidador(jwt)));
        relatorio.setIdoso(idoso);
        relatorio.setTipo(requisicao.tipo());
        relatorio.setTitulo(requisicao.titulo() != null && !requisicao.titulo().isBlank()
                ? requisicao.titulo().trim() : tituloPadrao(idoso, requisicao, periodo));
        relatorio.setSolicitacao(requisicao.solicitacao());
        relatorio.setInicioPeriodo(periodo.inicio());
        relatorio.setFimPeriodo(periodo.fim());
        relatorio.setStatus(StatusRelatorio.GERANDO);
        relatorio.setDataCriacao(LocalDateTime.now(propriedades.zona()));
        relatorio = relatorioRepository.save(relatorio);

        publicadorDeEventos.publishEvent(new RelatorioSolicitadoEvento(relatorio.getId()));
        return RelatorioResposta.de(relatorio);
    }

    /** Relatórios do idoso, mais recentes primeiro, sem o conteúdo. */
    @Transactional(readOnly = true)
    public List<RelatorioResposta> listar(Long idIdoso, Jwt jwt) {
        Idoso idoso = acessoIdosoService.buscarIdosoDoCuidador(idIdoso, jwt);
        return relatorioRepository.findByIdosoIdOrderByDataCriacaoDesc(idoso.getId()).stream()
                .map(RelatorioResposta::resumo).toList();
    }

    @Transactional(readOnly = true)
    public RelatorioResposta buscar(Long idIdoso, Long idRelatorio, Jwt jwt) {
        Idoso idoso = acessoIdosoService.buscarIdosoDoCuidador(idIdoso, jwt);
        return relatorioRepository.findByIdAndIdosoId(idRelatorio, idoso.getId())
                .map(RelatorioResposta::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Relatório", idRelatorio));
    }

    @Transactional
    public void remover(Long idIdoso, Long idRelatorio, Jwt jwt) {
        Idoso idoso = acessoIdosoService.buscarIdosoDoCuidador(idIdoso, jwt);
        Relatorio relatorio = relatorioRepository.findByIdAndIdosoId(idRelatorio, idoso.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Relatório", idRelatorio));
        relatorioRepository.delete(relatorio);
    }

    private String tituloPadrao(Idoso idoso, RelatorioRequisicao requisicao, Periodo periodo) {
        String assunto = switch (requisicao.tipo()) {
            case GERAL -> "Relatório geral";
            case MEDICAMENTOS -> "Medicamentos";
            case ALIMENTACAO -> "Alimentação";
            case ATIVIDADES -> "Atividades";
            case CONSULTAS -> "Consultas";
        };
        return assunto + " de " + idoso.getNome() + " (" + periodo.inicio().format(DATA) + " a "
                + periodo.fim().format(DATA) + ")";
    }
}
