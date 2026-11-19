package ajudantefamiliar.RAG.models;

import ajudantefamiliar.RAG.models.enums.StatusRelatorio;
import ajudantefamiliar.RAG.models.enums.TipoRelatorio;
import ajudantefamiliar.RAG.models.funcoes.Cuidador;
import ajudantefamiliar.RAG.models.funcoes.Idoso;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Relatório gerado pela IA com os dados do idoso em um período. Fica salvo para o cuidador rever
 * sem gerar (e pagar a IA) de novo. Começa GERANDO e termina CONCLUIDO (com conteúdo) ou ERRO.
 */
@Entity
@Table(name = "T_Ajudante_familiar_Relatorio")
@Getter
@Setter
@NoArgsConstructor
public class Relatorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_relatorio")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cuidador", nullable = false)
    private Cuidador cuidador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_idoso", nullable = false)
    private Idoso idoso;

    @Enumerated(EnumType.STRING)
    @Column(name = "tp_relatorio", length = 15, nullable = false)
    private TipoRelatorio tipo;

    @Column(name = "ds_titulo", length = 100, nullable = false)
    private String titulo;

    /** O que o cuidador pediu (instrução livre para a IA). */
    @Column(name = "ds_solicitacao", length = 500)
    private String solicitacao;

    @Column(name = "dt_inicio_periodo", nullable = false)
    private LocalDate inicioPeriodo;

    @Column(name = "dt_fim_periodo", nullable = false)
    private LocalDate fimPeriodo;

    /** Texto do relatório (Markdown). Nulo enquanto GERANDO. */
    @Lob
    @Column(name = "ds_conteudo")
    private String conteudo;

    /**
     * Inicia como GERANDO: o DEFAULT do banco só vale quando a coluna é omitida
     * no INSERT, e o Hibernate sempre envia a coluna.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "st_relatorio", length = 10, nullable = false)
    private StatusRelatorio status = StatusRelatorio.GERANDO;

    @Column(name = "ds_erro", length = 400)
    private String erro;

    @Column(name = "dt_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "dt_conclusao")
    private LocalDateTime dataConclusao;

    /** O service preenche no fuso do Brasil (rag.fuso-horario); aqui só completa se faltar. */
    @PrePersist
    void aoCriar() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
    }
}
