package storage.config;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração do storage (MinIO local / Amazon S3).
 * @param endpoint endpoint usado pelo backend; vazio em S3 real
 * @param publicEndpoint endpoint usado para assinar presigned URLs (alcançável pelo navegador)
 * @param region região
 * @param accessKey access key; vazio = cadeia padrão de credenciais da AWS (IAM role)
 * @param secretKey secret key
 * @param bucket nome do bucket
 * @param forcePathStyle true para MinIO
 * @param presignedUrlExpiration validade padrão de presigned URLs
 * @param connectTimeout timeout de conexão
 * @param readTimeout timeout de leitura de socket
 * @param apiCallTimeout timeout total de uma chamada (incluindo retries do SDK)
 */
@Validated
@ConfigurationProperties(prefix = "storage")
public record StorageProperties(
        String endpoint,
        String publicEndpoint,
        @NotBlank String region,
        String accessKey,
        String secretKey,
        @NotBlank String bucket,
        @DefaultValue("true") boolean forcePathStyle,
        @DefaultValue("15m") Duration presignedUrlExpiration,
        @DefaultValue("5s") Duration connectTimeout,
        @DefaultValue("30s") Duration readTimeout,
        @DefaultValue("60s") Duration apiCallTimeout) {

    public String effectivePublicEndpoint() {
        return StringUtils.hasText(publicEndpoint) ? publicEndpoint : endpoint;
    }

    public boolean hasStaticCredentials() {
        return StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey);
    }

    @Override
    public String toString() {
        return "StorageProperties[endpoint=%s, publicEndpoint=%s, region=%s, bucket=%s, forcePathStyle=%s, credentials=***]"
                .formatted(endpoint, publicEndpoint, region, bucket, forcePathStyle);
    }
}
