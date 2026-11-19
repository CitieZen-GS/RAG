package ajudantefamiliar.RAG.mensageria;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Estrutura do RabbitMQ usada pela API de RAG (consome o logout e publica/consome os pedidos de relatório).
 * <pre>
 * [autenticação] --"autenticacao.acesso-revogado"--> ajudante-familiar.eventos (topic)
 *                                                       └─> rag.acesso-revogado  (fila do RAG)
 * [RAG]          --"rag.relatorio-solicitado"-------> ajudante-familiar.eventos
 *                                                       └─> rag.relatorio-solicitado (gera o relatório)
 *                                     falhou 3 vezes ──> ajudante-familiar.eventos.dlx
 *                                                       └─> <fila>.dlq (para análise)
 * </pre>
 * O Spring declara tudo isso no RabbitMQ ao conectar (se já existir, nada muda).
 */
@Configuration
public class ConfiguracaoMensageria {

    /** Exchange compartilhado por todas as APIs. */
    public static final String EXCHANGE_EVENTOS = "ajudante-familiar.eventos";

    /** Para onde vão as mensagens que falharam em todas as tentativas (dead letter exchange). */
    public static final String EXCHANGE_FALHAS = "ajudante-familiar.eventos.dlx";

    public static final String ROTA_ACESSO_REVOGADO = "autenticacao.acesso-revogado";

    /** Publicada e consumida pelo próprio RAG: gerar relatório em segundo plano. */
    public static final String ROTA_RELATORIO_SOLICITADO = "rag.relatorio-solicitado";

    /** Filas desta API: "<api que consome>.<evento>". */
    public static final String FILA_ACESSO_REVOGADO = "rag.acesso-revogado";
    public static final String FILA_ACESSO_REVOGADO_FALHAS = FILA_ACESSO_REVOGADO + ".dlq";
    public static final String FILA_RELATORIO_SOLICITADO = "rag.relatorio-solicitado";
    public static final String FILA_RELATORIO_SOLICITADO_FALHAS = FILA_RELATORIO_SOLICITADO + ".dlq";

    @Bean
    public TopicExchange exchangeEventos() {
        return new TopicExchange(EXCHANGE_EVENTOS, true, false);
    }

    @Bean
    public TopicExchange exchangeFalhas() {
        return new TopicExchange(EXCHANGE_FALHAS, true, false);
    }

    /** Fila durável: se esta API estiver fora do ar, as mensagens esperam aqui. */
    @Bean
    public Queue filaAcessoRevogado() {
        return QueueBuilder.durable(FILA_ACESSO_REVOGADO)
                .deadLetterExchange(EXCHANGE_FALHAS)
                .build();
    }

    @Bean
    public Queue filaAcessoRevogadoFalhas() {
        return QueueBuilder.durable(FILA_ACESSO_REVOGADO_FALHAS).build();
    }

    @Bean
    public Binding ligacaoAcessoRevogado() {
        return BindingBuilder.bind(filaAcessoRevogado()).to(exchangeEventos()).with(ROTA_ACESSO_REVOGADO);
    }

    @Bean
    public Binding ligacaoAcessoRevogadoFalhas() {
        return BindingBuilder.bind(filaAcessoRevogadoFalhas()).to(exchangeFalhas()).with(ROTA_ACESSO_REVOGADO);
    }

    @Bean
    public Queue filaRelatorioSolicitado() {
        return QueueBuilder.durable(FILA_RELATORIO_SOLICITADO)
                .deadLetterExchange(EXCHANGE_FALHAS)
                .build();
    }

    @Bean
    public Queue filaRelatorioSolicitadoFalhas() {
        return QueueBuilder.durable(FILA_RELATORIO_SOLICITADO_FALHAS).build();
    }

    @Bean
    public Binding ligacaoRelatorioSolicitado() {
        return BindingBuilder.bind(filaRelatorioSolicitado()).to(exchangeEventos()).with(ROTA_RELATORIO_SOLICITADO);
    }

    @Bean
    public Binding ligacaoRelatorioSolicitadoFalhas() {
        return BindingBuilder.bind(filaRelatorioSolicitadoFalhas()).to(exchangeFalhas())
                .with(ROTA_RELATORIO_SOLICITADO);
    }

    /** Mensagens em JSON; o tipo é deduzido pelo parâmetro do método @RabbitListener. */
    @Bean
    public MessageConverter conversorJson() {
        return new JacksonJsonMessageConverter();
    }
}
