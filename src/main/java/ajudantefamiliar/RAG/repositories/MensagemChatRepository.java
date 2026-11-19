package ajudantefamiliar.RAG.repositories;

import ajudantefamiliar.RAG.models.MensagemChat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MensagemChatRepository extends JpaRepository<MensagemChat, Long> {

    /** Mensagens mais recentes primeiro; use Pageable para limitar a quantidade. */
    List<MensagemChat> findByConversaIdOrderByDataEnvioDescIdDesc(Long idConversa, Pageable limite);

    @Transactional
    @Modifying
    @Query("delete from MensagemChat m where m.conversa.id = :idConversa")
    int apagarDaConversa(@Param("idConversa") Long idConversa);
}
