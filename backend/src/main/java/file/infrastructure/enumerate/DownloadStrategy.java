package file.infrastructure.enumerate;

public enum DownloadStrategy {
    /** Devolve uma URL temporária; o cliente baixa direto do storage (padrão).*/
    PRESIGNED,
    /** O backend faz streaming do objeto. */
    STREAM
}
