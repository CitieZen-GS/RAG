package ajudantefamiliar.RAG.models.funcoes;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** T_Ajudante_familiar_Medicamentos (M_AJ_V10), só leitura. */
@Entity
@Immutable
@Table(name = "T_Ajudante_familiar_Medicamentos")
@Getter
@NoArgsConstructor
public class Medicamento {

    @Id
    @Column(name = "id_medicamentos")
    private Long id;

    @Column(name = "nm_medicamentos")
    private String nome;

    @Column(name = "via_administracao")
    private String viaAdministracao;

    @Column(name = "vl_frquencia")
    private String frequencia;

    @Column(name = "ds_instrucao_uso")
    private String instrucaoUso;

    @Column(name = "dt_medicamento")
    private LocalDate data;

    @Column(name = "hr_medicamento")
    private LocalDateTime hora;

    @Column(name = "id_idoso")
    private Long idIdoso;
}
