package searchengine.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;

public class ResponseHandler {
    public static ResponseEntity<Object> stringIsNotValidPositiveNumber
            (String attributeName, String stringNumber) {
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        response.put("result", false);
        int parsedLimit = 0;
        try {
            parsedLimit = Integer.parseInt(stringNumber);
            if (parsedLimit < 0) {
                response.put("result", false);
                response.put("error", attributeName + " не должно быть отрицательным: " + stringNumber);
            }
        } catch (NumberFormatException e) {
            if (stringNumber.matches(".*[\\p{L}\\p{S}\\p{P}].*")) { // if it matches any letter or symbol
                response.put("error",
                        attributeName + " должен быть числом, а не: " + stringNumber);
            } else {
                response.put("error",
                        attributeName + " не является допустимым номером: " + stringNumber);
            }
        }
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    public static ResponseEntity<Object> invalidSiteFormat(String site) {
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        response.put("result", false);
        response.put("error", "сайт имеет недопустимый формат: " + site);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    public static ResponseEntity<Object> siteNotFound(String errorMessage) {
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        response.put("result", false);
        response.put("error", errorMessage);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
}
