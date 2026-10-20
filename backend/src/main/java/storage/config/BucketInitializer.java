package storage.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import storage.infrastructure.contract.StoragePort;

/** Garante o bucket no startup. Falha rápida se o storage estiver inacessível ou as credenciais forem inválidas.*/
@Component
@ConditionalOnProperty(
    name = "storage.initialize-bucket",
    havingValue = "true",
    matchIfMissing = true
)
class BucketInitializer implements ApplicationRunner {
    private final StoragePort storage;

    BucketInitializer(StoragePort storage) {
        this.storage = storage;
    }

    @Override
    public void run(ApplicationArguments args) {
        storage.ensureBucket();
    }
}
