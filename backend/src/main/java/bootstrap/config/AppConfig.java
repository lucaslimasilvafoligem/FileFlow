package bootstrap.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AppConfig {
    /** Relógio injetável: permite testar lógica dependente de tempo.*/
    @Bean
    Clock clock() {return Clock.systemUTC();}
}
