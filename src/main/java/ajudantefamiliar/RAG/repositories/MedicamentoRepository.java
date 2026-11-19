package ajudantefamiliar.RAG.repositories;

import ajudantefamiliar.RAG.models.funcoes.Medicamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Só leitura. Filtra por hr_medicamento, que sempre tem data e hora
 * (dt_medicamento pode ser nula; a API de funções usa o dia do cadastro).
 */
public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {

    List<Medicamento> findByIdIdosoAndHoraBetweenOrderByHoraAsc(Long idIdoso, LocalDateTime inicio, LocalDateTime fim);
}
