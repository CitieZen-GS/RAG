package ajudantefamiliar.RAG.models.funcoes;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** T_Ajudante_familiar_Cuidador (M_AJ_V03), só leitura. Senha e e-mail não são mapeados. */
@Entity
@Immutable
@Table(name = "T_Ajudante_familiar_Cuidador")
@Getter
@NoArgsConstructor
public class Cuidador {

    @Id
    @Column(name = "id_cuidador")
    private Long id;

    @Column(name = "nm_cuidador")
    private String nome;
}
