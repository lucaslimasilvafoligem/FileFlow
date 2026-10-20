package storage.config;

import storage.domain.S3StorageAdapter;
import storage.infrastructure.contract.StoragePort;
import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/** Beans do AWS SDK. Trocar MinIO por S3 é só configuração (endpoint vazio + path-style false). */
@Configuration
public class StorageConfig {
    @Bean(destroyMethod = "close")
    public S3Client s3Client(StorageProperties props) {
        var builder = S3Client.builder()
                .region(Region.of(props.region()))
                .credentialsProvider(credentials(props))
                .forcePathStyle(props.forcePathStyle())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .httpClientBuilder(ApacheHttpClient.builder()
                        .connectionTimeout(props.connectTimeout())
                        .socketTimeout(props.readTimeout()))
                .overrideConfiguration(c -> c.apiCallTimeout(props.apiCallTimeout()));
        if (StringUtils.hasText(props.endpoint())) {
            builder.endpointOverride(URI.create(props.endpoint()));
        }
        return builder.build();
    }

    /** O presigner usa o endpoint PÚBLICO: o host entra na assinatura e o navegador precisa alcançá-lo. */
    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner(StorageProperties props) {
        var builder = S3Presigner.builder()
                .region(Region.of(props.region()))
                .credentialsProvider(credentials(props))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(props.forcePathStyle())
                        .build());
        String publicEndpoint = props.effectivePublicEndpoint();
        if (StringUtils.hasText(publicEndpoint)) {
            builder.endpointOverride(URI.create(publicEndpoint));
        }
        return builder.build();
    }

    @Bean
    public StoragePort storagePort(S3Client s3Client, S3Presigner presigner, StorageProperties props) {
        return new S3StorageAdapter(s3Client, presigner, props);
    }

    private static AwsCredentialsProvider credentials(StorageProperties props) {
        if (props.hasStaticCredentials()) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(props.accessKey(), props.secretKey()));
        }
        return DefaultCredentialsProvider.create();
    }
}
