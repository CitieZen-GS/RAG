package ajudantefamiliar.RAG.controller;

import ajudantefamiliar.RAG.dto.ErroResposta;
import ajudantefamiliar.RAG.dto.RelatorioRequisicao;
import ajudantefamiliar.RAG.dto.RelatorioResposta;
import ajudantefamiliar.RAG.services.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/idosos/{idIdoso}/relatorios")
@Tag(name = "Relatórios", description = "Relatórios do idoso gerados pela IA em segundo plano")
@RequiredArgsConstructor
public class RelatorioController {

    private final RelatorioService relatorioService;

    @Operation(summary = "Pede um relatório",
            description = "Responde na hora com status GERANDO. A IA gera em segundo plano: consulte "
                    + "GET /idosos/{idIdoso}/relatorios/{id} até o status virar CONCLUIDO (ou ERRO).")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Pedido aceito (status GERANDO)"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(responseCode = "401", description = "Sem token, token inválido ou revogado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Token de FAMILIAR ou idoso de outro cuidador",
                    content = @Content(schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Idoso não encontrado",
                    content = @Content(schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(responseCode = "422", description = "Período inválido (fim antes do início ou mais de 90 dias)",
                    content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @PostMapping
    public ResponseEntity<RelatorioResposta> solicitar(@PathVariable Long idIdoso, @AuthenticationPrincipal Jwt jwt,
                                                       @RequestBody @Valid RelatorioRequisicao requisicao,
                                                       UriComponentsBuilder uriBuilder) {
        RelatorioResposta resposta = relatorioService.solicitar(idIdoso, jwt, requisicao);
        var uri = uriBuilder.path("/idosos/{idIdoso}/relatorios/{id}").buildAndExpand(idIdoso, resposta.id()).toUri();
        return ResponseEntity.accepted().location(uri).body(resposta);
    }

    @Operation(summary = "Lista os relatórios do idoso", description = "Mais recentes primeiro, sem o conteúdo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista (vazia se não houver)"),
            @ApiResponse(responseCode = "403", description = "Token de FAMILIAR ou idoso de outro cuidador",
                    content = @Content(schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Idoso não encontrado",
                    content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @GetMapping
    public List<RelatorioResposta> listar(@PathVariable Long idIdoso, @AuthenticationPrincipal Jwt jwt) {
        return relatorioService.listar(idIdoso, jwt);
    }

    @Operation(summary = "Busca um relatório com o conteúdo (Markdown)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Relatório"),
            @ApiResponse(responseCode = "403", description = "Token de FAMILIAR ou idoso de outro cuidador",
                    content = @Content(schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Idoso ou relatório não encontrado",
                    content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @GetMapping("/{id}")
    public RelatorioResposta buscar(@PathVariable Long idIdoso, @PathVariable Long id,
                                    @AuthenticationPrincipal Jwt jwt) {
        return relatorioService.buscar(idIdoso, id, jwt);
    }

    @Operation(summary = "Remove um relatório")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Removido"),
            @ApiResponse(responseCode = "403", description = "Token de FAMILIAR ou idoso de outro cuidador",
                    content = @Content(schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(responseCode = "404", description = "Idoso ou relatório não encontrado",
                    content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long idIdoso, @PathVariable Long id,
                                        @AuthenticationPrincipal Jwt jwt) {
        relatorioService.remover(idIdoso, id, jwt);
        return ResponseEntity.noContent().build();
    }
}
