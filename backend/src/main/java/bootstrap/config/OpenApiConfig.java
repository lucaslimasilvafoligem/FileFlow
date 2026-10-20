package bootstrap.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {
    @Bean
    OpenAPI fileFlowOpenApi() {
        return new OpenAPI().info(new Info()
                .title("FileFlow API")
                .version("v1")
                .description("Ingestão, armazenamento e processamento assíncrono de arquivos."));
    }
}
