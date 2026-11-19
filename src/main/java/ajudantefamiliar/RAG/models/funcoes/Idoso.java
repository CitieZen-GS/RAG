package ajudantefamiliar.RAG.models.funcoes;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;

/** T_Ajudante_familiar_Idoso (M_AJ_V04), só leitura. idCuidador indica o dono (quem pode usar o chat). */
@Entity
@Immutable
@Table(name = "T_Ajudante_familiar_Idoso")
@Getter
@NoArgsConstructor
public class Idoso {

    @Id
    @Column(name = "id_idoso")
    private Long id;

    @Column(name = "nm_idoso")
    private String nome;

    @Column(name = "cpf_idoso")
    private String cpf;

    @Column(name = "vlr_altura")
    private BigDecimal altura;

    @Column(name = "vlr_peso")
    private BigDecimal peso;

    @Column(name = "dt_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "ds_comportamento")
    private String comportamento;

    @Column(name = "id_cuidador")
    private Long idCuidador;
}
