package searchengine.exceptions;

public class TooManyPageErrorsException extends SiteIndexationErrorException{
    public TooManyPageErrorsException(String message, Exception e) {
        super(message, e);
    }

    public TooManyPageErrorsException(String message) {
        super(message);
    }
}
