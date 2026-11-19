package ajudantefamiliar.RAG.services;

import ajudantefamiliar.RAG.configuracoes.PropriedadesRag;
import ajudantefamiliar.RAG.dto.MensagemResposta;
import ajudantefamiliar.RAG.dto.PerguntaRequisicao;
import ajudantefamiliar.RAG.dto.PeriodoResposta;
import ajudantefamiliar.RAG.dto.RespostaChat;
import ajudantefamiliar.RAG.models.Conversa;
import ajudantefamiliar.RAG.models.MensagemChat;
import ajudantefamiliar.RAG.models.enums.AutorMensagem;
import ajudantefamiliar.RAG.models.enums.TipoRelatorio;
import ajudantefamiliar.RAG.models.funcoes.Idoso;
import ajudantefamiliar.RAG.repositories.ConversaRepository;
import ajudantefamiliar.RAG.repositories.CuidadorRepository;
import ajudantefamiliar.RAG.repositories.MensagemChatRepository;
import ajudantefamiliar.RAG.services.excecoes.IaIndisponivelException;
import ajudantefamiliar.RAG.services.ia.PromptsIa;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Chat do cuidador sobre o idoso (uma conversa contínua por cuidador e idoso).
 * <p>
 * Cada pergunta: confere o dono do idoso → define o período → monta a linha do tempo (contexto) →
 * envia à IA com as últimas mensagens (memória) → grava pergunta e resposta.
 * <p>
 * Sem @Transactional no método principal de propósito: a chamada à IA leva segundos e não deve segurar
 * uma conexão do banco aberta. Cada leitura/gravação usa a própria transação do repository.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final AcessoIdosoService acessoIdosoService;
    private final PeriodoService periodoService;
    private final LinhaDoTempoService linhaDoTempoService;
    private final ConversaRepository conversaRepository;
    private final MensagemChatRepository mensagemChatRepository;
    private final CuidadorRepository cuidadorRepository;
    private final ChatClient chatClient;
    private final PropriedadesRag propriedades;

    public RespostaChat perguntar(Long idIdoso, Jwt jwt,
                                  PerguntaRequisicao requisicao) {
        Idoso idoso = acessoIdosoService.buscarIdosoDoCuidador(idIdoso, jwt);
        Conversa conversa = obterConversa(acessoIdosoService.idCuidador(jwt), idoso);

        List<MensagemChat> anteriores = ultimasMensagens(conversa.getId(), propriedades.historicoMensagens());
        Periodo periodo = periodoService.definir(requisicao.pergunta(), ultimaPergunta(anteriores),
                requisicao.dataInicio(), requisicao.dataFim());
        String contexto = linhaDoTempoService.montar(idoso, periodo, TipoRelatorio.GERAL);

        String resposta;
        try {
            resposta = chatClient.prompt()
                    .system(PromptsIa.chat(contexto))
                    .messages(comoHistorico(anteriores))
                    .user(requisicao.pergunta())
                    .call()
                    .content();
        } catch (RuntimeException e) {
            throw new IaIndisponivelException(e);
        }
        if (resposta == null || resposta.isBlank()) {
            throw new IaIndisponivelException(null);
        }

        MensagemChat pergunta = gravar(conversa, AutorMensagem.CUIDADOR, requisicao.pergunta());
        MensagemChat respostaGravada = gravar(conversa, AutorMensagem.ASSISTENTE, resposta);
        conversa.setDataUltimaMensagem(respostaGravada.getDataEnvio());
        conversaRepository.save(conversa);

        return new RespostaChat(MensagemResposta.de(pergunta), MensagemResposta.de(respostaGravada),
                PeriodoResposta.de(periodo));
    }

    /** Histórico em ordem cronológica (as {@code limite} mensagens mais recentes). */
    public List<MensagemResposta> historico(Long idIdoso, Jwt jwt, int limite) {
        Idoso idoso = acessoIdosoService.buscarIdosoDoCuidador(idIdoso, jwt);
        return conversaRepository.findByCuidadorIdAndIdosoId(acessoIdosoService.idCuidador(jwt), idoso.getId())
                .map(conversa -> ultimasMensagens(conversa.getId(), limite).stream().map(MensagemResposta::de).toList())
                .orElse(List.of());
    }

    /** Apaga as mensagens da conversa (a conversa continua existindo, vazia). */
    public void limpar(Long idIdoso, Jwt jwt) {
        Idoso idoso = acessoIdosoService.buscarIdosoDoCuidador(idIdoso, jwt);
        conversaRepository.findByCuidadorIdAndIdosoId(acessoIdosoService.idCuidador(jwt), idoso.getId())
                .ifPresent(conversa -> {
                    mensagemChatRepository.apagarDaConversa(conversa.getId());
                    conversa.setDataUltimaMensagem(null);
                    conversaRepository.save(conversa);
                });
    }

    private Conversa obterConversa(Long idCuidador, Idoso idoso) {
        return conversaRepository.findByCuidadorIdAndIdosoId(idCuidador, idoso.getId()).orElseGet(() -> {
            Conversa nova = new Conversa();
            nova.setCuidador(cuidadorRepository.getReferenceById(idCuidador));
            nova.setIdoso(idoso);
            nova.setDataCriacao(agora());
            try {
                return conversaRepository.saveAndFlush(nova);
            } catch (DataIntegrityViolationException e) {
                // Duas primeiras perguntas ao mesmo tempo: a outra requisição criou a conversa
                return conversaRepository.findByCuidadorIdAndIdosoId(idCuidador, idoso.getId()).orElseThrow(() -> e);
            }
        });
    }

    private List<MensagemChat> ultimasMensagens(Long idConversa, int limite) {
        List<MensagemChat> recentes = new ArrayList<>(
                mensagemChatRepository.findByConversaIdOrderByDataEnvioDescIdDesc(idConversa, PageRequest.of(0, limite)));
        Collections.reverse(recentes);
        return recentes;
    }

    private String ultimaPergunta(List<MensagemChat> anteriores) {
        for (int i = anteriores.size() - 1; i >= 0; i--) {
            if (anteriores.get(i).getAutor() == AutorMensagem.CUIDADOR) {
                return anteriores.get(i).getConteudo();
            }
        }
        return null;
    }

    private List<Message> comoHistorico(List<MensagemChat> anteriores) {
        return anteriores.stream()
                .map(m -> m.getAutor() == AutorMensagem.CUIDADOR
                        ? (Message) new UserMessage(m.getConteudo())
                        : new AssistantMessage(m.getConteudo()))
                .toList();
    }

    private MensagemChat gravar(Conversa conversa, AutorMensagem autor, String conteudo) {
        MensagemChat mensagem = new MensagemChat();
        mensagem.setConversa(conversa);
        mensagem.setAutor(autor);
        mensagem.setConteudo(conteudo);
        mensagem.setDataEnvio(agora());
        return mensagemChatRepository.save(mensagem);
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(propriedades.zona());
    }
}
