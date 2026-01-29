package searchengine.exceptions;

public class LemmaNotFoundException extends RuntimeException{
    public LemmaNotFoundException(String message) {
        super(message);
    }
}
