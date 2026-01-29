package searchengine.exceptions;

public class PageNotFoundException extends RuntimeException{
    public PageNotFoundException(int pageId) {
        super("Page with ID " + pageId + " not found.");
    }

    public PageNotFoundException(String message) {
        super(message);
    }
}
