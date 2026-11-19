package ajudantefamiliar.RAG.services;

import ajudantefamiliar.RAG.configuracoes.PropriedadesRag;
import ajudantefamiliar.RAG.models.enums.TipoRelatorio;
import ajudantefamiliar.RAG.models.funcoes.*;
import ajudantefamiliar.RAG.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parte "R" (retrieval) do RAG: busca os registros do idoso no período, direto nas tabelas da API de funções,
 * e monta um texto em ordem cronológica que vai como contexto para a IA.
 * <p>
 * Exemplo de linha: {@code - 10/11/2026 12:00 [Alimentação] Almoço: Sopa (350,5 kcal); preparo: legumes}
 */
@Service
@RequiredArgsConstructor
public class LinhaDoTempoService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    /** Valores gravados em tp_alimentacao (sem acento) -> texto para a IA. */
    private static final Map<String, String> REFEICOES = Map.of(
            "Cafe da manha", "Café da manhã",
            "Almoco", "Almoço",
            "Lanche de tarde", "Lanche da tarde",
            "Jantar", "Jantar");

    private final DoencaRepository doencaRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final AlimentacaoRepository alimentacaoRepository;
    private final AtividadeRepository atividadeRepository;
    private final LembreteConsultaRepository lembreteConsultaRepository;
    private final PropriedadesRag propriedades;

    /** Um item da linha do tempo; {@code temHora=false} quando só a data é conhecida. */
    record Evento(LocalDateTime momento, boolean temHora, String texto) {
    }

    /**
     * @param escopo GERAL traz tudo; os demais tipos trazem só aquele assunto (o cabeçalho do idoso vem sempre)
     */
    @Transactional(readOnly = true)
    public String montar(Idoso idoso, Periodo periodo, TipoRelatorio escopo) {
        LocalDate hoje = LocalDate.now(propriedades.zona());
        StringBuilder texto = new StringBuilder();

        texto.append("## Dados do idoso\n").append(dadosDoIdoso(idoso, hoje)).append('\n');
        texto.append("## Período consultado: ").append(periodo.inicio().format(DATA))
                .append(" a ").append(periodo.fim().format(DATA))
                .append(" (hoje é ").append(hoje.format(DATA)).append(")\n\n");

        List<Evento> eventos = buscarEventos(idoso.getId(), periodo, escopo);
        texto.append("## Registros em ordem cronológica");
        if (escopo != TipoRelatorio.GERAL) {
            texto.append(" (somente ").append(escopo.name().toLowerCase()).append(")");
        }
        texto.append('\n');

        if (eventos.isEmpty()) {
            return texto.append("Nenhum registro no período.\n").toString();
        }
        int limite = propriedades.limiteRegistros();
        if (eventos.size() > limite) {
            texto.append("(").append(eventos.size() - limite)
                    .append(" registros mais antigos foram omitidos por limite de tamanho)\n");
            eventos = eventos.subList(eventos.size() - limite, eventos.size());
        }
        eventos.forEach(evento -> texto.append("- ").append(evento.texto()).append('\n'));
        return texto.toString();
    }

    List<Evento> buscarEventos(Long idIdoso, Periodo periodo, TipoRelatorio escopo) {
        List<Evento> eventos = new ArrayList<>();
        if (inclui(escopo, TipoRelatorio.MEDICAMENTOS)) {
            medicamentoRepository.findByIdIdosoAndHoraBetweenOrderByHoraAsc(idIdoso,
                            periodo.inicio().atStartOfDay(), periodo.fim().atTime(LocalTime.MAX))
                    .forEach(m -> eventos.add(medicamento(m)));
        }
        if (inclui(escopo, TipoRelatorio.ALIMENTACAO)) {
            alimentacaoRepository.findByIdIdosoAndDataConsumoBetweenOrderByHoraConsumoAsc(idIdoso,
                            periodo.inicio(), periodo.fim())
                    .forEach(a -> eventos.add(alimentacao(a)));
        }
        if (inclui(escopo, TipoRelatorio.ATIVIDADES)) {
            atividadeRepository.findByIdIdosoAndDataBetweenOrderByDataAscHoraInicioAsc(idIdoso,
                            periodo.inicio(), periodo.fim())
                    .forEach(a -> eventos.add(atividade(a)));
        }
        if (inclui(escopo, TipoRelatorio.CONSULTAS)) {
            lembreteConsultaRepository.findByIdIdosoAndDataConsultaBetweenOrderByDataConsultaAscHoraConsultaAsc(
                            idIdoso, periodo.inicio(), periodo.fim())
                    .forEach(c -> eventos.add(consulta(c)));
        }
        eventos.sort(Comparator.comparing(Evento::momento));
        return eventos;
    }

    private boolean inclui(TipoRelatorio escopo, TipoRelatorio assunto) {
        return escopo == TipoRelatorio.GERAL || escopo == assunto;
    }

    private String dadosDoIdoso(Idoso idoso, LocalDate hoje) {
        StringBuilder dados = new StringBuilder("Nome: ").append(idoso.getNome());
        if (idoso.getDataNascimento() != null) {
            dados.append(" | Idade: ").append(Period.between(idoso.getDataNascimento(), hoje).getYears())
                    .append(" anos (nascimento ").append(idoso.getDataNascimento().format(DATA)).append(")");
        }
        if (idoso.getAltura() != null) {
            dados.append(" | Altura: ").append(numero(idoso.getAltura())).append(" m");
        }
        if (idoso.getPeso() != null) {
            dados.append(" | Peso: ").append(numero(idoso.getPeso())).append(" kg");
        }
        dados.append('\n');
        if (temTexto(idoso.getComportamento())) {
            dados.append("Comportamento: ").append(idoso.getComportamento()).append('\n');
        }
        List<Doenca> doencas = doencaRepository.findByIdIdosoOrderByNomeAsc(idoso.getId());
        dados.append("Doenças: ").append(doencas.isEmpty() ? "nenhuma registrada" : doencas.stream()
                .map(d -> d.getNome() + (temTexto(d.getDescricao()) ? " (" + d.getDescricao() + ")" : ""))
                .collect(Collectors.joining("; "))).append('\n');
        return dados.toString();
    }

    private Evento medicamento(Medicamento m) {
        StringBuilder t = new StringBuilder(m.getHora().format(DATA_HORA)).append(" [Medicamento] ").append(m.getNome());
        complemento(t, "via", m.getViaAdministracao());
        complemento(t, "frequência", m.getFrequencia());
        complemento(t, "instrução", m.getInstrucaoUso());
        return new Evento(m.getHora(), true, t.toString());
    }

    private Evento alimentacao(Alimentacao a) {
        LocalDateTime momento = a.getHoraConsumo() != null ? a.getHoraConsumo() : a.getDataConsumo().atStartOfDay();
        StringBuilder t = new StringBuilder(quando(a.getHoraConsumo(), a.getDataConsumo()))
                .append(" [Alimentação] ").append(REFEICOES.getOrDefault(a.getTipo(), a.getTipo()))
                .append(": ").append(a.getNome());
        if (a.getCalorias() != null) {
            t.append(" (").append(numero(a.getCalorias())).append(" kcal)");
        }
        complemento(t, "preparo", a.getPreparacao());
        return new Evento(momento, a.getHoraConsumo() != null, t.toString());
    }

    private Evento atividade(Atividade a) {
        LocalDateTime momento = a.getHoraInicio() != null ? a.getHoraInicio() : a.getData().atStartOfDay();
        StringBuilder t = new StringBuilder(quando(a.getHoraInicio(), a.getData()));
        if (a.getHoraInicio() != null && a.getHoraFim() != null) {
            t.append("–").append(a.getHoraFim().format(HORA));
        }
        t.append(" [Atividade] ").append(a.getNome());
        complemento(t, "descrição", a.getDescricao());
        return new Evento(momento, a.getHoraInicio() != null, t.toString());
    }

    private Evento consulta(LembreteConsulta c) {
        LocalDateTime momento = c.getHoraConsulta() != null ? c.getHoraConsulta() : c.getDataConsulta().atStartOfDay();
        StringBuilder t = new StringBuilder(quando(c.getHoraConsulta(), c.getDataConsulta()))
                .append(" [Consulta] ").append(temTexto(c.getTitulo()) ? c.getTitulo() : "Consulta");
        if (temTexto(c.getNomeMedico())) {
            t.append(" com ").append(c.getNomeMedico());
        }
        t.append(" (").append(c.getStatus()).append(")");
        complemento(t, "descrição", c.getDescricao());
        if (!c.getMedicamentos().isEmpty()) {
            complemento(t, "medicamentos ligados", c.getMedicamentos().stream()
                    .map(Medicamento::getNome).collect(Collectors.joining(", ")));
        }
        return new Evento(momento, c.getHoraConsulta() != null, t.toString());
    }

    private String quando(LocalDateTime dataHora, LocalDate data) {
        return dataHora != null ? dataHora.format(DATA_HORA) : data.format(DATA) + " (sem horário)";
    }

    private void complemento(StringBuilder texto, String rotulo, String valor) {
        if (temTexto(valor)) {
            texto.append("; ").append(rotulo).append(": ").append(valor);
        }
    }

    private boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String numero(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
