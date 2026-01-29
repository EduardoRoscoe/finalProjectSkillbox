package searchengine.exceptions;

public class UnableToConnectToSiteException extends RuntimeException{
    public UnableToConnectToSiteException (String message) {
        super(message);
    }

    public UnableToConnectToSiteException(String message, Throwable cause) {
        super(message, cause);
    }
}
