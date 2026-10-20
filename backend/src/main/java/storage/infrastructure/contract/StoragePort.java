package storage.infrastructure.contract;
import java.io.InputStream;
import java.time.Duration;
import storage.infrastructure.record.PageResult;
import storage.infrastructure.record.StorageObject;
import storage.infrastructure.record.StorageObjectMetadata;
import storage.infrastructure.record.UploadObject;

public interface StoragePort {
    StorageObject upload(UploadObject input);

    StorageObjectMetadata getMetadata(String key);

    /** O chamador é responsável por fechar o stream.*/
    InputStream download(String key);

    /** Idempotente: remover chave inexistente não é erro.*/
    void delete(String key);

    boolean exists(String key);

    /** Paginação por continuation token (semântica do S3). {@code limit} entre 1 e 1000.*/
    PageResult<StorageObjectMetadata> list(String prefix, String continuationToken, int limit);

    default String generatePresignedUrl(String key, Duration expiration) {
        return generatePresignedUrl(key, expiration, null);
    }

    /** @param downloadFileName nome sugerido ao navegador no download; pode ser nulo*/
    String generatePresignedUrl(String key, Duration expiration, String downloadFileName);

    /** Cria o bucket se necessário; seguro sob corrida entre instâncias. */
    void ensureBucket();

    boolean bucketExists();

    /** Verifica conectividade e credenciais; lança exceção de storage em caso de falha.*/
    void ping();
}
