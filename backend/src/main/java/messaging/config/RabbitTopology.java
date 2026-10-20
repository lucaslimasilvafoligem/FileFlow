package messaging.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import messaging.infrastructure.record.MessagingProperties;
import processing.infrastructure.enumerate.ProcessingFamily;

/**
 * Topologia dos jobs, declarada no startup:
 * <pre>
 *  fileflow.jobs (direct) --[familia]--> fileflow.jobs.{familia}
 *  falha transitória: fileflow.jobs.retry --[familia.N]--> fileflow.jobs.{familia}.retry.N
 *                     (TTL do tier N, expira de volta em fileflow.jobs com a chave da família)
 *  esgotado/venenosa: fileflow.jobs.dlx --[dead]--> fileflow.jobs.dlq
 * </pre>
 * Alterar argumentos de uma fila já criada exige apagá-la no broker (PRECONDITION_FAILED).
 */
@Configuration
public class RabbitTopology {
    public static final String JOBS_EXCHANGE = "fileflow.jobs";
    static final String RETRY_EXCHANGE = "fileflow.jobs.retry";
    static final String DEAD_LETTER_EXCHANGE = "fileflow.jobs.dlx";
    static final String DEAD_LETTER_QUEUE = "fileflow.jobs.dlq";
    static final String DEAD_ROUTING_KEY = "dead";

    static String queueName(ProcessingFamily family) {
        return JOBS_EXCHANGE + "." + family.routingKey();
    }

    static String retryQueueName(ProcessingFamily family, int attempt) {
        return queueName(family) + ".retry." + attempt;
    }

    static String retryRoutingKey(ProcessingFamily family, int attempt) {
        return family.routingKey() + "." + attempt;
    }

    @Bean
    public Declarables jobTopology(MessagingProperties properties) {
        DirectExchange jobs = new DirectExchange(JOBS_EXCHANGE, true, false);
        DirectExchange retry = new DirectExchange(RETRY_EXCHANGE, true, false);
        DirectExchange deadLetter = new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
        Queue dlq = QueueBuilder.durable(DEAD_LETTER_QUEUE).build();

        List<Declarable> declarables = new ArrayList<>(
            List.of(
                jobs,
                retry,
                deadLetter,
                dlq,
                BindingBuilder.bind(dlq).to(deadLetter).with(DEAD_ROUTING_KEY)
            )
        );
        List<Duration> delays = properties.retry().delays();
        for (ProcessingFamily family : ProcessingFamily.values()) {
            String key = family.routingKey();
            Queue main = QueueBuilder.durable(queueName(family))
                    .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                    .deadLetterRoutingKey(DEAD_ROUTING_KEY)
                    .build();
            Binding mainBinding = BindingBuilder.bind(main).to(jobs).with(key);
            declarables.add(main);
            declarables.add(mainBinding);
            for (int attempt = 1; attempt <= delays.size(); attempt++) {
                Queue wait = QueueBuilder.durable(retryQueueName(family, attempt))
                        .ttl((int) delays.get(attempt - 1).toMillis())
                        .deadLetterExchange(JOBS_EXCHANGE)
                        .deadLetterRoutingKey(key)
                        .build();
                declarables.add(wait);
                declarables.add(BindingBuilder.bind(wait).to(retry).with(retryRoutingKey(family, attempt)));
            }
        }
        return new Declarables(declarables);
    }
}
