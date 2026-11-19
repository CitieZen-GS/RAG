package ajudantefamiliar.RAG.apoio;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

/**
 * Modelo de IA falso para testes: devolve respostas pré-definidas, em ordem, e guarda os prompts recebidos.
 * Use com {@code ChatClient.create(iaFalsa)}. Não chama a OpenAI.
 */
public class IaFalsa implements ChatModel {

    private final Deque<Supplier<String>> respostas = new ArrayDeque<>();
    private final List<Prompt> promptsRecebidos = new ArrayList<>();

    public IaFalsa responde(String texto) {
        respostas.add(() -> texto);
        return this;
    }

    public IaFalsa falha(RuntimeException erro) {
        respostas.add(() -> {
            throw erro;
        });
        return this;
    }

    public List<Prompt> promptsRecebidos() {
        return promptsRecebidos;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        promptsRecebidos.add(prompt);
        if (respostas.isEmpty()) {
            throw new IllegalStateException("IaFalsa sem resposta configurada");
        }
        return new ChatResponse(List.of(new Generation(new AssistantMessage(respostas.poll().get()))));
    }
}
