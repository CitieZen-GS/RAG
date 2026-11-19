package ajudantefamiliar.RAG.models.funcoes;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** T_Ajudante_familiar_Doencas (M_AJ_V06), só leitura. Não tem data: entra sempre no contexto do idoso. */
@Entity
@Immutable
@Table(name = "T_Ajudante_familiar_Doencas")
@Getter
@NoArgsConstructor
public class Doenca {

    @Id
    @Column(name = "id_doenca")
    private Long id;

    @Column(name = "nm_doenca")
    private String nome;

    @Column(name = "ds_doenca")
    private String descricao;

    @Column(name = "id_idoso")
    private Long idIdoso;
}
