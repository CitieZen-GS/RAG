package ajudantefamiliar.RAG.repositories;

import ajudantefamiliar.RAG.models.funcoes.Idoso;
import org.springframework.data.jpa.repository.JpaRepository;

/** Só leitura: a entidade é @Immutable e pertence à API de funções. */
public interface IdosoRepository extends JpaRepository<Idoso, Long> {
}
