package processing.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;
import processing.infrastructure.enumerate.JobType;

/**
 * Configuração de processamento.
 * @param enabledJobTypes tipos de job que o upload pode criar (habilite quando o processador existir)
 */
@Validated
@ConfigurationProperties(prefix = "processing")
public record ProcessingProperties(@DefaultValue("FILE_METADATA") List<JobType> enabledJobTypes) {}
