package ajudantefamiliar.RAG.repositories;

import ajudantefamiliar.RAG.models.funcoes.Doenca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Só leitura. Doenças não têm data: entram sempre no contexto do idoso. */
public interface DoencaRepository extends JpaRepository<Doenca, Long> {

    List<Doenca> findByIdIdosoOrderByNomeAsc(Long idIdoso);
}
