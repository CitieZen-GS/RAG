package ajudantefamiliar.RAG.repositories;

import ajudantefamiliar.RAG.models.funcoes.Alimentacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/** Só leitura. */
public interface AlimentacaoRepository extends JpaRepository<Alimentacao, Long> {

    List<Alimentacao> findByIdIdosoAndDataConsumoBetweenOrderByHoraConsumoAsc(Long idIdoso, LocalDate inicio,
                                                                              LocalDate fim);
}
