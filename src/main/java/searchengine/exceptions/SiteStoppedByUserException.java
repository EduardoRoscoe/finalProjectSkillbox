package searchengine.exceptions;

import searchengine.model.SiteEntity;

public class SiteStoppedByUserException extends RuntimeException {
    public SiteStoppedByUserException(String message, RuntimeException exception) {
        super(message);
        addSuppressed(exception);
    }

    public SiteStoppedByUserException(String message) {
        super(message);
    }
}
