package ajudantefamiliar.RAG.models;

import ajudantefamiliar.RAG.models.enums.AutorMensagem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Mensagem da conversa: pergunta do cuidador ou resposta da IA. */
@Entity
@Table(name = "T_Ajudante_familiar_Mensagem_Chat")
@Getter
@Setter
@NoArgsConstructor
public class MensagemChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensagem")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_conversa", nullable = false)
    private Conversa conversa;

    @Enumerated(EnumType.STRING)
    @Column(name = "tp_autor", length = 10, nullable = false)
    private AutorMensagem autor;

    /** CLOB: respostas da IA podem passar de 4.000 caracteres. */
    @Lob
    @Column(name = "ds_conteudo", nullable = false)
    private String conteudo;

    @Column(name = "dt_envio", nullable = false, updatable = false)
    private LocalDateTime dataEnvio;

    /** O service preenche no fuso do Brasil (rag.fuso-horario); aqui só completa se faltar. */
    @PrePersist
    void aoCriar() {
        if (dataEnvio == null) {
            dataEnvio = LocalDateTime.now();
        }
    }
}
