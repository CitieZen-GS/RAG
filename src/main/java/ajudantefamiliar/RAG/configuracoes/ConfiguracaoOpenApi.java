package ajudantefamiliar.RAG.configuracoes;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentação Swagger / OpenAPI.
 * <ul>
 *   <li>Swagger UI: /swagger-ui.html</li>
 *   <li>JSON OpenAPI: /v3/api-docs</li>
 * </ul>
 * O botão "Authorize" recebe o JWT do cuidador (sem o prefixo "Bearer").
 */
@Configuration
public class ConfiguracaoOpenApi {

    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI documentacaoApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ajudante Familiar: API de RAG")
                        .version("v1")
                        .description("""
                                Chat com IA sobre a linha do tempo do idoso e geração de relatórios.

                                **Acesso:** só o **CUIDADOR** (token da API de autenticação), e só para os idosos dele.
                                Token de FAMILIAR recebe 403.

                                **Chat** (`/idosos/{idIdoso}/chat`): uma conversa contínua por idoso. A IA responde
                                só com os registros do período consultado: datas enviadas pelo app, período
                                identificado na pergunta ("ontem", "semana passada") ou os últimos 7 dias.

                                **Relatórios** (`/idosos/{idIdoso}/relatorios`): o POST responde na hora com
                                `GERANDO`; a IA gera em segundo plano (RabbitMQ). Consulte o relatório até o status
                                virar `CONCLUIDO` (texto em Markdown) ou `ERRO`.

                                Períodos de até 90 dias. Datas no formato `aaaa-mm-dd`."""))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("tokenAcesso de um CUIDADOR, emitido pela API de autenticação")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }
}
