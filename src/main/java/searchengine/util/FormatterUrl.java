package searchengine.util;

import searchengine.exceptions.InvalidUrlFormatException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class FormatterUrl {
    public static String formatStringIntoURL (String url) {
        url = URLDecoder.decode(url, StandardCharsets.UTF_8); // decode from any signs of the post request
        if (url.startsWith("url=")) {
            url = url.substring(4); // take out the url= from it
        }
        if (url.startsWith("\"") && url.endsWith("\"")) {
            url = url.substring(1, url.length() - 1);
        }
        if (!(url == null) && !url.endsWith("/")) {
            url = url.concat("/");
        }
        if (!(url.startsWith("https://") || url.startsWith("http://"))) {
            url = "https://".concat(url);
        }

        return url;
    }

    public static String verifyAndFormatUrl(String url) {
        if (!Verifier.siteIsValidFormat(url)) {
            throw new InvalidUrlFormatException("сайт имеет недопустимый формат: " + url);
        }
        url = formatStringIntoURL(url);

        return url;
    }
}
