package searchengine.exceptions;

public class IndexEntityAlreadyExistsException extends RuntimeException{
    public IndexEntityAlreadyExistsException(String message) {
        super(message);
    }
}
