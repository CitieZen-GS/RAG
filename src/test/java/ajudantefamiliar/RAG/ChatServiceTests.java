package ajudantefamiliar.RAG;

import ajudantefamiliar.RAG.apoio.Dados;
import ajudantefamiliar.RAG.apoio.IaFalsa;
import ajudantefamiliar.RAG.dto.PerguntaRequisicao;
import ajudantefamiliar.RAG.dto.RespostaChat;
import ajudantefamiliar.RAG.models.Conversa;
import ajudantefamiliar.RAG.models.MensagemChat;
import ajudantefamiliar.RAG.models.enums.AutorMensagem;
import ajudantefamiliar.RAG.models.enums.TipoRelatorio;
import ajudantefamiliar.RAG.models.funcoes.Idoso;
import ajudantefamiliar.RAG.repositories.ConversaRepository;
import ajudantefamiliar.RAG.repositories.CuidadorRepository;
import ajudantefamiliar.RAG.repositories.MensagemChatRepository;
import ajudantefamiliar.RAG.services.*;
import ajudantefamiliar.RAG.services.excecoes.IaIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Fluxo do chat: contexto + memória → IA → grava pergunta e resposta. */
class ChatServiceTests {

    private final IaFalsa ia = new IaFalsa();
    private AcessoIdosoService acesso;
    private LinhaDoTempoService linhaDoTempo;
    private ConversaRepository conversas;
    private MensagemChatRepository mensagens;
    private ChatService servico;
    private final Idoso jose = Dados.idoso(10L, 1L);
    private final Conversa conversa = new Conversa();
    private final Jwt token = Jwt.withTokenValue("t").header("alg", "HS256").subject("1")
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();

    @BeforeEach
    void preparar() {
        acesso = mock(AcessoIdosoService.class);
        linhaDoTempo = mock(LinhaDoTempoService.class);
        conversas = mock(ConversaRepository.class);
        mensagens = mock(MensagemChatRepository.class);
        ChatClient chatClient = ChatClient.create(ia);
        servico = new ChatService(acesso, new PeriodoService(chatClient, Dados.propriedades()), linhaDoTempo,
                conversas, mensagens, mock(CuidadorRepository.class), chatClient, Dados.propriedades());

        conversa.setId(5L);
        when(acesso.buscarIdosoDoCuidador(10L, token)).thenReturn(jose);
        when(acesso.idCuidador(token)).thenReturn(1L);
        when(conversas.findByCuidadorIdAndIdosoId(1L, 10L)).thenReturn(Optional.of(conversa));
        when(linhaDoTempo.montar(eq(jose), any(), eq(TipoRelatorio.GERAL)))
                .thenReturn("- 17/11/2026 12:00 [Alimentação] Almoço: Sopa\n");
        when(mensagens.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
        // memória: uma troca anterior (o repository devolve da mais recente para a mais antiga)
        when(mensagens.findByConversaIdOrderByDataEnvioDescIdDesc(eq(5L), any())).thenReturn(List.of(
                mensagem(AutorMensagem.ASSISTENTE, "Ele tomou Metformina às 08:00."),
                mensagem(AutorMensagem.CUIDADOR, "Ele tomou o remédio ontem?")));
    }

    @Test
    void respondeComOContextoEAMemoriaEGravaPerguntaEResposta() {
        ia.responde("{\"encontrado\": true, \"inicio\": \"2026-11-17\", \"fim\": \"2026-11-17\"}")
                .responde("Ontem o José almoçou sopa às 12:00.");

        RespostaChat resposta = servico.perguntar(10L, token,
                new PerguntaRequisicao("E o que ele almoçou?", null, null));

        assertThat(resposta.resposta().conteudo()).isEqualTo("Ontem o José almoçou sopa às 12:00.");
        assertThat(resposta.periodoConsultado().origem()).isEqualTo(OrigemPeriodo.IDENTIFICADO_PELA_IA);
        assertThat(resposta.periodoConsultado().inicio()).isEqualTo(LocalDate.of(2026, 11, 17));

        // 1ª chamada: período (recebe a pergunta anterior); 2ª: resposta
        assertThat(ia.promptsRecebidos().getFirst().getContents()).contains("Pergunta anterior: Ele tomou o remédio ontem?");
        List<Message> enviado = ia.promptsRecebidos().get(1).getInstructions();
        assertThat(enviado.getFirst().getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(enviado.getFirst().getText()).contains("<contexto>", "[Alimentação] Almoço: Sopa", "Não faça diagnósticos");
        assertThat(enviado).extracting(Message::getMessageType).containsExactly(
                MessageType.SYSTEM, MessageType.USER, MessageType.ASSISTANT, MessageType.USER);
        assertThat(enviado.getLast().getText()).isEqualTo("E o que ele almoçou?");

        verify(mensagens, times(2)).save(any(MensagemChat.class));
        verify(conversas).save(conversa);
    }

    @Test
    void falhaDaIaNaoGravaNada() {
        ia.responde("{\"encontrado\": false}").falha(new RuntimeException("limite de uso da OpenAI"));

        assertThatThrownBy(() -> servico.perguntar(10L, token, new PerguntaRequisicao("Como ele está?", null, null)))
                .isInstanceOf(IaIndisponivelException.class);
        verify(mensagens, never()).save(any());
    }

    private MensagemChat mensagem(AutorMensagem autor, String texto) {
        MensagemChat m = new MensagemChat();
        m.setAutor(autor);
        m.setConteudo(texto);
        m.setDataEnvio(LocalDateTime.now());
        return m;
    }
}
