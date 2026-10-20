package storage.domain;

import storage.config.StorageProperties;
import storage.infrastructure.contract.StoragePort;
import storage.infrastructure.record.PageResult;
import storage.infrastructure.record.StorageObject;
import storage.infrastructure.record.StorageObjectMetadata;
import storage.infrastructure.record.UploadObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import exception.storage.StorageAuthenticationException;
import exception.storage.StorageException;
import exception.storage.StorageObjectNotFoundException;
import exception.storage.StorageOperationException;
import exception.storage.StorageUnavailableException;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * Adapter S3/MinIO sobre o AWS SDK v2. Retry de rede fica a cargo do próprio SDK (política padrão);
 * não há retry adicional aqui para não multiplicar tentativas.
 */
public class S3StorageAdapter implements StoragePort {
    private static final Logger LOG = LoggerFactory.getLogger(S3StorageAdapter.class);
    private static final int MAX_PAGE_SIZE = 1000;
    private static final Set<String> AUTH_ERROR_CODES = Set.of(
            "InvalidAccessKeyId",
            "SignatureDoesNotMatch",
            "AccessDenied",
            "ExpiredToken",
            "InvalidToken"
        );

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;

    public S3StorageAdapter(S3Client s3, S3Presigner presigner, StorageProperties properties) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = properties.bucket();
    }

    @Override
    public StorageObject upload(UploadObject input) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(input.key())
                    .contentType(input.contentType())
                    .contentLength(input.contentLength())
                    .build();
            PutObjectResponse response = s3.putObject(
                    request, RequestBody.fromInputStream(
                        input.content(),
                        input.contentLength()
                    )
                );
            LOG.debug("storage.upload ok key={} size={}", input.key(), input.contentLength());
            return new StorageObject(input.key(), stripQuotes(response.eTag()), input.contentLength());
        } catch (SdkException e) {
            throw translate("upload", input.key(), e);
        }
    }

    @Override
    public StorageObjectMetadata getMetadata(String key) {
        try {
            HeadObjectResponse head = s3.headObject(b -> b.bucket(bucket).key(key));
            return new StorageObjectMetadata(
                    key, head.contentLength(),
                    head.contentType(),
                    stripQuotes(head.eTag()),
                    head.lastModified()
                );
        } catch (SdkException e) {
            throw translate("getMetadata", key, e);
        }
    }

    @Override
    public InputStream download(String key) {
        try {
            return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException e) {
            throw translate("download", key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3.deleteObject(b -> b.bucket(bucket).key(key));
            LOG.debug("storage.delete ok key={}", key);
        } catch (SdkException e) {
            throw translate("delete", key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            s3.headObject(b -> b.bucket(bucket).key(key));
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (SdkException e) {
            StorageException translated = translate("exists", key, e);
            if (translated instanceof StorageObjectNotFoundException) {
                return false;
            }
            throw translated;
        }
    }

    @Override
    public PageResult<StorageObjectMetadata> list(String prefix, String continuationToken, int limit) {
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_PAGE_SIZE);
        }
        try {
            ListObjectsV2Request.Builder request = ListObjectsV2Request.builder().bucket(bucket).maxKeys(limit);
            if (prefix != null && !prefix.isBlank()) {
                request.prefix(prefix);
            }
            if (continuationToken != null && !continuationToken.isBlank()) {
                request.continuationToken(continuationToken);
            }
            ListObjectsV2Response response = s3.listObjectsV2(request.build());
            List<StorageObjectMetadata> items = response.contents().stream()
                    .map(o -> new StorageObjectMetadata(
                            o.key(), o.size(), null, stripQuotes(o.eTag()), o.lastModified()))
                    .toList();
            String next = Boolean.TRUE.equals(response.isTruncated()) ? response.nextContinuationToken() : null;
            return new PageResult<>(items, next);
        } catch (SdkException e) {
            throw translate("list", prefix, e);
        }
    }

    @Override
    public String generatePresignedUrl(String key, Duration expiration, String downloadFileName) {
        try {
            GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(b -> {
                        b.bucket(bucket).key(key);
                        if (downloadFileName != null && !downloadFileName.isBlank()) {
                            b.responseContentDisposition(attachment(downloadFileName));
                        }
                    })
                    .build();
            return presigner.presignGetObject(request).url().toString();
        } catch (SdkException e) {
            throw translate("presign", key, e);
        }
    }

    private static String attachment(String fileName) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename*=UTF-8''" + encoded;
    }

    @Override
    public void ensureBucket() {
        if (bucketExists()) {
            LOG.info("storage.bucket already exists bucket={}", bucket);
            return;
        }
        try {
            s3.createBucket(b -> b.bucket(bucket));
            LOG.info("storage.bucket created bucket={}", bucket);
        } catch (BucketAlreadyOwnedByYouException e) {
            LOG.info("storage.bucket created concurrently by another instance bucket={}", bucket);
        } catch (SdkException e) {
            throw translate("ensureBucket", bucket, e);
        }
    }

    @Override
    public boolean bucketExists() {
        try {
            s3.headBucket(b -> b.bucket(bucket));
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        } catch (SdkException e) {
            if (isNotFound(e)) {
                return false;
            }
            throw translate("bucketExists", bucket, e);
        }
    }

    @Override
    public void ping() {
        try {
            s3.headBucket(b -> b.bucket(bucket));
        } catch (NoSuchBucketException e) {
            LOG.debug("storage.ping reachable, bucket missing bucket={}", bucket);
        } catch (SdkException e) {
            if (isNotFound(e)) {
                return;
            }
            throw translate("ping", bucket, e);
        }
    }

    private static boolean isNotFound(SdkException e) {
        return e instanceof AwsServiceException service && service.statusCode() == 404;
    }

    private static String stripQuotes(String etag) {
        return etag == null ? null : etag.replace("\"", "");
    }

    /** Classifica a falha do SDK em exceções de domínio. A mensagem não vaza detalhes do provedor.*/
    private StorageException translate(String operation, String target, SdkException e) {
        String message = "Storage operation '%s' failed".formatted(operation);
        if (e instanceof AwsServiceException service) {
            int status = service.statusCode();
            String code = service.awsErrorDetails() == null ? "" : service.awsErrorDetails().errorCode();
            if (status == 401 || status == 403 || AUTH_ERROR_CODES.contains(code)) {
                return new StorageAuthenticationException(message, e);
            }
            if (status == 404 && ("NoSuchKey".equals(code) || "NotFound".equals(code))) {
                return new StorageObjectNotFoundException(target, e);
            }
            if (status >= 500) {
                return new StorageUnavailableException(message, e);
            }
        }
        if (e instanceof SdkClientException) {
            return new StorageUnavailableException(message, e);
        }
        LOG.warn("storage.operation failed operation={} target={}", operation, target);
        return new StorageOperationException(message, e);
    }
}
