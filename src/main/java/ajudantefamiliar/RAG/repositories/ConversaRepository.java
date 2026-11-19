package ajudantefamiliar.RAG.repositories;

import ajudantefamiliar.RAG.models.Conversa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversaRepository extends JpaRepository<Conversa, Long> {

    /** Uma única conversa por cuidador e idoso (uk_conversa_cuidador_idoso). */
    Optional<Conversa> findByCuidadorIdAndIdosoId(Long idCuidador, Long idIdoso);
}
