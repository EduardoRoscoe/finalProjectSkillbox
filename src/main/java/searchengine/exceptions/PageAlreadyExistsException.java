package searchengine.exceptions;

public class PageAlreadyExistsException extends RuntimeException {
    public PageAlreadyExistsException(String message) {
        super(message);
    }
}
