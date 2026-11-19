package ajudantefamiliar.RAG.repositories;

import ajudantefamiliar.RAG.models.Relatorio;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RelatorioRepository extends JpaRepository<Relatorio, Long> {

    List<Relatorio> findByIdosoIdOrderByDataCriacaoDesc(Long idIdoso);

    /** Garante que o relatório é do idoso da rota (o dono do idoso já foi conferido). */
    Optional<Relatorio> findByIdAndIdosoId(Long id, Long idIdoso);

    /** Usado pelo consumidor da fila: já traz o idoso para montar a linha do tempo. */
    @EntityGraph(attributePaths = "idoso")
    Optional<Relatorio> findComIdosoById(Long id);
}
