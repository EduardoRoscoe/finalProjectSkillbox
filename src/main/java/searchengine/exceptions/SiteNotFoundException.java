package searchengine.exceptions;

import searchengine.model.SiteEntity;

public class SiteNotFoundException extends RuntimeException {
    public SiteNotFoundException(int siteId) {
        super("Site with ID " + siteId + " not found.");
    }

    public SiteNotFoundException(String message) {
        super(message);
    }
}
