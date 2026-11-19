package ajudantefamiliar.RAG.models.funcoes;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** T_Ajudante_familiar_Atividades (M_AJ_V07), só leitura. Horas em Oracle DATE (data + hora). */
@Entity
@Immutable
@Table(name = "T_Ajudante_familiar_Atividades")
@Getter
@NoArgsConstructor
public class Atividade {

    @Id
    @Column(name = "id_atividade")
    private Long id;

    @Column(name = "nm_atividade")
    private String nome;

    @Column(name = "ds_atividade")
    private String descricao;

    @Column(name = "dt_atividade")
    private LocalDate data;

    @Column(name = "hr_atividade_inicio")
    private LocalDateTime horaInicio;

    @Column(name = "hr_atividade_fim")
    private LocalDateTime horaFim;

    @Column(name = "id_idoso")
    private Long idIdoso;
}
