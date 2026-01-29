package searchengine.util;

import java.net.*;
import java.nio.charset.StandardCharsets;

import org.apache.commons.beanutils.converters.URLConverter;
import org.apache.commons.validator.routines.UrlValidator;

public class Verifier {
    public static boolean siteIsValidFormat(String siteUrl) {
        if (siteUrl == null) {
            return false;
        }
        if (siteUrl.isEmpty()) {
            return false;
        }
        siteUrl = FormatterUrl.formatStringIntoURL(siteUrl);
        UrlValidator validator = new UrlValidator(new String[]{"http", "https"});
        return validator.isValid(siteUrl);
    }
}
