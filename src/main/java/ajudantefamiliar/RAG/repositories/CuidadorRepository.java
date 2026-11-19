package ajudantefamiliar.RAG.repositories;

import ajudantefamiliar.RAG.models.funcoes.Cuidador;
import org.springframework.data.jpa.repository.JpaRepository;

/** Só leitura: a entidade é @Immutable e pertence à API de funções. */
public interface CuidadorRepository extends JpaRepository<Cuidador, Long> {
}
