package ajudantefamiliar.RAG.controller;

import ajudantefamiliar.RAG.dto.ErroResposta;
import ajudantefamiliar.RAG.dto.MensagemResposta;
import ajudantefamiliar.RAG.dto.PerguntaRequisicao;
import ajudantefamiliar.RAG.dto.RespostaChat;
import ajudantefamiliar.RAG.services.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/idosos/{idIdoso}/chat")
@Tag(name = "Chat", description = "Perguntas sobre a linha do tempo do idoso (uma conversa contínua por idoso)")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "Envia uma pergunta e recebe a resposta da IA",
            description = "A IA responde só com os registros do idoso no período consultado (devolvido em "
                    + "periodoConsultado). Pode levar alguns segundos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resposta gerada; pergunta e resposta ficam no histórico"),
            @ApiResponse(responseCode = "400", description = "Pergunta vazia ou dados inválidos",
                    content = @Content(schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(responseCode = "401", description = "Sem token, token inválido ou revogado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Token de FAMILIAR ou idoso de outro cuidador",
                    content = @Content(schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Idoso não encontrado",
                    content = @Content(schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(responseCode = "422", description = "Período inválido (fim antes do início ou mais de 90 dias)",
                    content = @Content(schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(responseCode = "503", description = "A IA não respondeu; nada foi gravado",
                    content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @PostMapping
    public RespostaChat perguntar(@PathVariable Long idIdoso, @AuthenticationPrincipal Jwt jwt,
                                  @RequestBody @Valid PerguntaRequisicao requisicao) {
        return chatService.perguntar(idIdoso, jwt, requisicao);
    }

    @Operation(summary = "Histórico da conversa, em ordem cronológica",
            description = "Devolve as mensagens mais recentes (limite padrão 50).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mensagens (lista vazia se ainda não houve conversa)"),
            @ApiResponse(responseCode = "403", description = "Token de FAMILIAR ou idoso de outro cuidador",
                    content = @Content(schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Idoso não encontrado",
                    content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @GetMapping
    public List<MensagemResposta> historico(@PathVariable Long idIdoso, @AuthenticationPrincipal Jwt jwt,
                                            @Parameter(description = "Quantas mensagens recentes trazer (1 a 200)")
                                            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limite) {
        return chatService.historico(idIdoso, jwt, limite);
    }

    @Operation(summary = "Apaga as mensagens da conversa", description = "A memória da IA recomeça do zero.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Mensagens apagadas"),
            @ApiResponse(responseCode = "403", description = "Token de FAMILIAR ou idoso de outro cuidador",
                    content = @Content(schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Idoso não encontrado",
                    content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @DeleteMapping
    public ResponseEntity<Void> limpar(@PathVariable Long idIdoso, @AuthenticationPrincipal Jwt jwt) {
        chatService.limpar(idIdoso, jwt);
        return ResponseEntity.noContent().build();
    }
}
