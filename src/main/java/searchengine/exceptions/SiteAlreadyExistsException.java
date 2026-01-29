package searchengine.exceptions;

public class SiteAlreadyExistsException extends RuntimeException {
    public SiteAlreadyExistsException(String message) {
        super(message);
    }
}
