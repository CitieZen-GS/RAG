package ajudantefamiliar.RAG.repositories;

import ajudantefamiliar.RAG.models.funcoes.LembreteConsulta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/** Só leitura. Já traz os medicamentos ligados a cada consulta (evita consultas extras e erro de lazy). */
public interface LembreteConsultaRepository extends JpaRepository<LembreteConsulta, Long> {

    @EntityGraph(attributePaths = "medicamentos")
    List<LembreteConsulta> findByIdIdosoAndDataConsultaBetweenOrderByDataConsultaAscHoraConsultaAsc(
            Long idIdoso, LocalDate inicio, LocalDate fim);
}
