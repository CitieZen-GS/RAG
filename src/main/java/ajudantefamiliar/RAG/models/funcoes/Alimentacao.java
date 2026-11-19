package ajudantefamiliar.RAG.models.funcoes;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** T_Ajudante_familiar_Alimentacao (M_AJ_V08), só leitura. */
@Entity
@Immutable
@Table(name = "T_Ajudante_familiar_Alimentacao")
@Getter
@NoArgsConstructor
public class Alimentacao {

    @Id
    @Column(name = "id_alimentacao")
    private Long id;

    @Column(name = "nm_alimentacao")
    private String nome;

    /** Como gravado no banco: 'Cafe da manha', 'Almoco', 'Lanche de tarde' ou 'Jantar'. */
    @Column(name = "tp_alimentacao")
    private String tipo;

    @Column(name = "dt_consumo")
    private LocalDate dataConsumo;

    @Column(name = "hr_consumo")
    private LocalDateTime horaConsumo;

    @Column(name = "vlr_caloria")
    private BigDecimal calorias;

    @Column(name = "ds_preparacao")
    private String preparacao;

    @Column(name = "id_idoso")
    private Long idIdoso;
}
