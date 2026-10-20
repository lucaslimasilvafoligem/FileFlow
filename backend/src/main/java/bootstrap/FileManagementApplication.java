package bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
    "bootstrap",
    "storage",
    "file",
    "messaging",
    "processing",
    "exception"
})
@ConfigurationPropertiesScan(basePackages = {
    "storage.config",
    "file.config",
    "processing.config",
    "messaging.infrastructure.record"
})
@EnableJpaRepositories(basePackages = {
    "file.infrastructure.repository",
    "processing.infrastructure.repository",
    "messaging.infrastructure.repository"
})
@EntityScan(basePackages = {
    "file.infrastructure.entity",
    "processing.infrastructure.entity",
    "messaging.infrastructure.entity"
})
public class FileManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileManagementApplication.class, args);
    }
}