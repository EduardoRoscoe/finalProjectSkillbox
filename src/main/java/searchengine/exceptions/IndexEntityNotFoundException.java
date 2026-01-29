package searchengine.exceptions;

public class IndexEntityNotFoundException extends RuntimeException {
    public IndexEntityNotFoundException(String message) {
        super(message);
    }
}
