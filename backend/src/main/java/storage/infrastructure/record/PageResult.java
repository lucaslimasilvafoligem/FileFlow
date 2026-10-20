package storage.infrastructure.record;
import java.util.List;

/** Página de resultados; {@code nextToken} é nulo na última página. */
public record PageResult<T>(List<T> items, String nextToken) {
    public boolean hasNext() {
        return nextToken != null;
    }
}
