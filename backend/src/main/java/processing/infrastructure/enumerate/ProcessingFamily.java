package processing.infrastructure.enumerate;

import java.util.Locale;

/** Família de processamento: cada uma tem sua fila e seus workers. */
public enum ProcessingFamily {
    FILE,
    IMAGE,
    AUDIO,
    VIDEO,
    DOCUMENT;

    public String routingKey() {
        return name().toLowerCase(Locale.ROOT);
    }
}
