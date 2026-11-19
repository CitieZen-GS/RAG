package ajudantefamiliar.RAG.apoio;

import ajudantefamiliar.RAG.configuracoes.PropriedadesRag;
import ajudantefamiliar.RAG.models.funcoes.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Fábrica de dados para testes. As entidades da API de funções são só leitura (sem setters). */
public final class Dados {

    private Dados() {
    }

    public static PropriedadesRag propriedades() {
        return new PropriedadesRag("America/Sao_Paulo", 7, 90, 10, 400);
    }

    public static Idoso idoso(Long id, Long idCuidador) {
        Idoso idoso = new Idoso();
        ReflectionTestUtils.setField(idoso, "id", id);
        ReflectionTestUtils.setField(idoso, "nome", "Jose");
        ReflectionTestUtils.setField(idoso, "dataNascimento", LocalDate.of(1940, 5, 10));
        ReflectionTestUtils.setField(idoso, "altura", new BigDecimal("1.70"));
        ReflectionTestUtils.setField(idoso, "peso", new BigDecimal("70.50"));
        ReflectionTestUtils.setField(idoso, "comportamento", "calmo");
        ReflectionTestUtils.setField(idoso, "idCuidador", idCuidador);
        return idoso;
    }

    public static Medicamento medicamento(String nome, LocalDateTime hora) {
        Medicamento m = new Medicamento();
        ReflectionTestUtils.setField(m, "nome", nome);
        ReflectionTestUtils.setField(m, "viaAdministracao", "Oral");
        ReflectionTestUtils.setField(m, "frequencia", "12h");
        ReflectionTestUtils.setField(m, "hora", hora);
        return m;
    }

    public static Alimentacao alimentacao(String nome, String tipo, LocalDateTime hora) {
        Alimentacao a = new Alimentacao();
        ReflectionTestUtils.setField(a, "nome", nome);
        ReflectionTestUtils.setField(a, "tipo", tipo);
        ReflectionTestUtils.setField(a, "dataConsumo", hora.toLocalDate());
        ReflectionTestUtils.setField(a, "horaConsumo", hora);
        ReflectionTestUtils.setField(a, "calorias", new BigDecimal("350.50"));
        return a;
    }

    public static Atividade atividade(String nome, LocalDate data) {
        Atividade a = new Atividade();
        ReflectionTestUtils.setField(a, "nome", nome);
        ReflectionTestUtils.setField(a, "data", data);
        return a;
    }

    public static LembreteConsulta consulta(String titulo, LocalDateTime hora, Medicamento... medicamentos) {
        LembreteConsulta c = new LembreteConsulta();
        ReflectionTestUtils.setField(c, "titulo", titulo);
        ReflectionTestUtils.setField(c, "nomeMedico", "Dr Silva");
        ReflectionTestUtils.setField(c, "dataConsulta", hora.toLocalDate());
        ReflectionTestUtils.setField(c, "horaConsulta", hora);
        ReflectionTestUtils.setField(c, "status", "Pendente");
        ReflectionTestUtils.setField(c, "medicamentos", List.of(medicamentos));
        return c;
    }

    public static Doenca doenca(String nome) {
        Doenca d = new Doenca();
        ReflectionTestUtils.setField(d, "nome", nome);
        ReflectionTestUtils.setField(d, "descricao", "tipo 2");
        return d;
    }
}
