package ajudantefamiliar.RAG.controller;

import ajudantefamiliar.RAG.dto.ErroResposta;
import ajudantefamiliar.RAG.services.excecoes.AcessoNegadoException;
import ajudantefamiliar.RAG.services.excecoes.IaIndisponivelException;
import ajudantefamiliar.RAG.services.excecoes.RecursoNaoEncontradoException;
import ajudantefamiliar.RAG.services.excecoes.RegraNegocioException;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

/**
 * Converte exceções em respostas JSON padronizadas (mesmo formato das outras APIs).
 */
@Slf4j
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> naoEncontrado(RecursoNaoEncontradoException e, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage(), req, List.of());
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResposta> acessoNegado(AcessoNegadoException e, HttpServletRequest req) {
        return resposta(HttpStatus.FORBIDDEN, e.getMessage(), req, List.of());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResposta> regraNegocio(RegraNegocioException e, HttpServletRequest req) {
        return resposta(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage(), req, List.of());
    }

    @ExceptionHandler(IaIndisponivelException.class)
    public ResponseEntity<ErroResposta> iaIndisponivel(IaIndisponivelException e, HttpServletRequest req) {
        log.error("Falha na chamada à IA", e.getCause());
        return resposta(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), req, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> integridade(DataIntegrityViolationException e, HttpServletRequest req) {
        return resposta(HttpStatus.CONFLICT,
                "Operação viola uma restrição do banco (registro duplicado ou com dados vinculados)", req, List.of());
    }

    /** Parâmetros de URL/query fora das regras (ex.: limite maior que 200). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErroResposta> parametroForaDasRegras(HandlerMethodValidationException e,
                                                               HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro inválido: verifique os limites informados", req, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> validacao(MethodArgumentNotValidException e, HttpServletRequest req) {
        List<ErroResposta.CampoInvalido> campos = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroResposta.CampoInvalido(erro.getField(), erro.getDefaultMessage()))
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos", req, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> corpoInvalido(HttpMessageNotReadableException e, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido: verifique o JSON, datas (aaaa-mm-dd) e enums", req, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> parametroInvalido(MethodArgumentTypeMismatchException e,
                                                          HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + e.getName() + "'",
                req, List.of());
    }

    private ResponseEntity<ErroResposta> resposta(HttpStatus status, String mensagem, HttpServletRequest req,
                                                  List<ErroResposta.CampoInvalido> campos) {
        ErroResposta corpo = new ErroResposta(Instant.now(), status.value(), status.getReasonPhrase(),
                mensagem, req.getRequestURI(), campos);
        return ResponseEntity.status(status).body(corpo);
    }
}
