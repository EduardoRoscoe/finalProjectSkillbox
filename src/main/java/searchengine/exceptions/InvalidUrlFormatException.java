package searchengine.exceptions;

public class InvalidUrlFormatException extends RuntimeException {
    public InvalidUrlFormatException(String message) {
        super(message);
    }
}
