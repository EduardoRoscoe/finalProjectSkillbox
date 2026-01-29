package searchengine.exceptions;

public class LemmaAlreadyExistsException extends RuntimeException{
    public LemmaAlreadyExistsException(String message) {
        super(message);
    }
}
