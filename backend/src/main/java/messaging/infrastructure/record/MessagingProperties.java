package messaging.infrastructure.record;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração de mensageria.
 * @param retry política de retry
 * @param outbox configuração do relay do outbox
 */
@Validated
@ConfigurationProperties(prefix = "messaging")
public record MessagingProperties(@Valid @DefaultValue Retry retry, @Valid @DefaultValue Outbox outbox) {
    /**
     * Política de retry.
     * @param delays espera antes de cada nova tentativa; o total de tentativas é delays.size() + 1
     */
    public record Retry(@DefaultValue({"5s", "30s", "2m"}) @NotEmpty List<Duration> delays) {
    }

    /**
     * Relay do outbox.
     * @param batchSize eventos por lote
     */
    public record Outbox(@DefaultValue("100") @Min(1) int batchSize) {
    }
}
