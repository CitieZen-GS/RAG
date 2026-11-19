package ajudantefamiliar.RAG.models.funcoes;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * T_Ajudante_familiar_Lembrete_Consulta (M_AJ_V09), só leitura, com os medicamentos ligados à consulta
 * pela tabela associativa T_Ajudante_familiar_Consulta_Medicamento (M_AJ_V11).
 */
@Entity
@Immutable
@Table(name = "T_Ajudante_familiar_Lembrete_Consulta")
@Getter
@NoArgsConstructor
public class LembreteConsulta {

    @Id
    @Column(name = "id_lembrete_consulta")
    private Long id;

    @Column(name = "ds_titulo_lembrente_consulta")
    private String titulo;

    @Column(name = "nm_consulta_medico")
    private String nomeMedico;

    @Column(name = "dt_consulta")
    private LocalDate dataConsulta;

    @Column(name = "hr_consulta")
    private LocalDateTime horaConsulta;

    @Column(name = "ds_consulta")
    private String descricao;

    /** Como gravado no banco: 'Pendente' ou 'Realizado'. */
    @Column(name = "st_consulta")
    private String status;

    @Column(name = "id_idoso")
    private Long idIdoso;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "T_Ajudante_familiar_Consulta_Medicamento",
            joinColumns = @JoinColumn(name = "id_lembrete_consulta"),
            inverseJoinColumns = @JoinColumn(name = "id_medicamentos"))
    private List<Medicamento> medicamentos = new ArrayList<>();
}
