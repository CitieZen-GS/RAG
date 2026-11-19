package ajudantefamiliar.RAG.configuracoes;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cliente da IA. O modelo (OpenAI), a chave e o nome do modelo vêm do application.yaml
 * (spring.ai.openai.*), configurados pelo Spring AI.
 */
@Configuration
public class ConfiguracaoIa {

    @Bean
    public ChatClient chatClient(ChatClient.Builder construtor) {
        return construtor.build();
    }
}
