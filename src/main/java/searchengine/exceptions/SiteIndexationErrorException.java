package searchengine.exceptions;

import searchengine.model.SiteEntity;

public class SiteIndexationErrorException extends RuntimeException {
    public SiteIndexationErrorException(String message, Exception e) {
        super(message);
        addSuppressed(e);
    }

    public SiteIndexationErrorException(String message) {
        super(message);
    }
}
