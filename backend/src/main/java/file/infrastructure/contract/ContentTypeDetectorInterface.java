package file.infrastructure.contract;

import java.io.InputStream;

/** Detecta o tipo pelo conteúdo (magic bytes), usando o nome apenas como dica.*/
public interface ContentTypeDetectorInterface {
    /** Não fecha o stream. Retorna {@code application/octet-stream} se não identificar.*/
    String detect(InputStream content, String fileName);
}
