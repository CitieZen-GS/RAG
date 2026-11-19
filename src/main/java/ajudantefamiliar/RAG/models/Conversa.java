package ajudantefamiliar.RAG.models;

import ajudantefamiliar.RAG.models.funcoes.Cuidador;
import ajudantefamiliar.RAG.models.funcoes.Idoso;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Conversa do chat: uma única conversa contínua por cuidador e idoso (uk_conversa_cuidador_idoso).
 */
@Entity
@Table(name = "T_Ajudante_familiar_Conversa")
@Getter
@Setter
@NoArgsConstructor
public class Conversa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conversa")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cuidador", nullable = false)
    private Cuidador cuidador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_idoso", nullable = false)
    private Idoso idoso;

    @Column(name = "dt_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "dt_ultima_mensagem")
    private LocalDateTime dataUltimaMensagem;

    @OneToMany(mappedBy = "conversa")
    @OrderBy("dataEnvio ASC")
    private List<MensagemChat> mensagens = new ArrayList<>();

    /** O service preenche no fuso do Brasil (rag.fuso-horario); aqui só completa se faltar. */
    @PrePersist
    void aoCriar() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
    }
}
