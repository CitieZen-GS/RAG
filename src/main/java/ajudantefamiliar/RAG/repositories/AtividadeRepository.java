package ajudantefamiliar.RAG.repositories;

import ajudantefamiliar.RAG.models.funcoes.Atividade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/** Só leitura. */
public interface AtividadeRepository extends JpaRepository<Atividade, Long> {

    List<Atividade> findByIdIdosoAndDataBetweenOrderByDataAscHoraInicioAsc(Long idIdoso, LocalDate inicio,
                                                                           LocalDate fim);
}
