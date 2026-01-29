package searchengine.exceptions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import searchengine.model.SiteEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LoopSiteIndexationCustomException extends Exception {
    LinkedHashMap<String, Exception> causes;
    List<SiteEntity> indexedSites;
    public LoopSiteIndexationCustomException(LinkedHashMap<String, Exception> causes, List<SiteEntity> indexedSites) {
        super(formatMessage(causes));
        this.causes = causes;
        this.indexedSites = indexedSites;
        for (Exception exception : causes.values()) {
            addSuppressed(exception);
        }
    }

    private static String formatMessage(LinkedHashMap<String, Exception> causes) {
        String message = "These sites failed the indexation with these errors: ";
        LinkedHashMap<String, String> jsonMap = new LinkedHashMap<>();
        jsonMap.put(message, "");
        for (Map.Entry<String, Exception> entry : causes.entrySet()) {
            message = message + "\n\t'" + entry.getKey() + "': " + entry.getValue().getMessage();
        }
        return message;
    }
    public LinkedHashMap<String, LinkedHashMap<String,String>> getMessageIntoJson() {
        String message = "These sites failed the indexation with these errors";
        LinkedHashMap<String, LinkedHashMap<String,String>> jsonMap = new LinkedHashMap<>();
        LinkedHashMap<String, String> sitesMap = new LinkedHashMap<>();
        for (Map.Entry<String, Exception> entry : causes.entrySet()) {
            String errorClassName = entry.getValue().getClass().getSimpleName();
            String errorMessage = entry.getValue().getMessage();
            String errorFullMessage = errorClassName + ": " + errorMessage;
            String siteUrl = entry.getKey();
            sitesMap.put(siteUrl, errorFullMessage);
        }
        jsonMap.put(message, sitesMap);
        String messageIndexed = "These sites have been successfully indexed";
        LinkedHashMap<String, String> indexedSitesMap = new LinkedHashMap<>();
        if (indexedSites.isEmpty()) {
            messageIndexed = "No sites have been indexed";
        }
        for (SiteEntity site : indexedSites) {
            indexedSitesMap.put(site.getUrl(), "Indexed");
        }
        jsonMap.put(messageIndexed, indexedSitesMap);
        return jsonMap;
    }

    public List<SiteEntity> getIndexedSites(){
        return indexedSites;
    }
    public LinkedHashMap<String, Exception> getCauses() {
        return causes;
    }
}
