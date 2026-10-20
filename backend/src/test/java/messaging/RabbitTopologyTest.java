package messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;

import messaging.config.RabbitTopology;
import messaging.infrastructure.record.MessagingProperties;

class RabbitTopologyTest {

    private final MessagingProperties properties = new MessagingProperties(
            new MessagingProperties.Retry(List.of(Duration.ofSeconds(5), Duration.ofSeconds(30), Duration.ofMinutes(2))),
            new MessagingProperties.Outbox(100));

    private final Declarables topology = new RabbitTopology().jobTopology(properties);

    @Test
    void declaresDlqPlusMainAndRetryQueuesPerFamily() {
        List<String> names = topology.getDeclarablesByType(Queue.class).stream().map(Queue::getName).toList();

        assertThat(names).hasSize(1 + 5 + 5 * 3)
                .contains("fileflow.jobs.dlq", "fileflow.jobs.image", "fileflow.jobs.image.retry.2");
    }

    @Test
    void retryQueueExpiresBackIntoMainExchangeWithFamilyRoutingKey() {
        Queue retry = queue("fileflow.jobs.audio.retry.2");

        assertThat(retry.getArguments())
                .containsEntry("x-message-ttl", 30_000)
                .containsEntry("x-dead-letter-exchange", "fileflow.jobs")
                .containsEntry("x-dead-letter-routing-key", "audio");
    }

    @Test
    void mainQueuesDeadLetterIntoDlq() {
        Queue main = queue("fileflow.jobs.image");

        assertThat(main.getArguments())
                .containsEntry("x-dead-letter-exchange", "fileflow.jobs.dlx")
                .containsEntry("x-dead-letter-routing-key", "dead");
    }

    private Queue queue(String name) {
        return topology.getDeclarablesByType(Queue.class).stream()
                .filter(q -> q.getName().equals(name)).findFirst().orElseThrow();
    }
}
