package searchengine.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import searchengine.exceptions.LoopSiteIndexationCustomException;
import searchengine.model.SiteEntity;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class RestExceptionHandler {
    Logger logger = LoggerFactory.getLogger(RestExceptionHandler.class);
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleException(ResponseStatusException ex) {
        logger.error(ex.getMessage(), ex);
        Map<String, Object> response = new HashMap<>();
        response.put("result", false);
        response.put("error", ex.getClass().getSimpleName() + ": " + ex.getMessage());

        return new ResponseEntity<>(response, ex.getStatus());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleException(HttpMessageNotReadableException ex) {
        logger.error(ex.getMessage(), ex);
        Map<String, Object> response = new HashMap<>();
        response.put("result", false);
        response.put("error", ex.getClass().getSimpleName() + ": " + ex.getMessage());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(LoopSiteIndexationCustomException.class)
    public ResponseEntity<Object> handleException(LoopSiteIndexationCustomException ex) {
        logger.error("Error from RestExceptionHandler", ex);
        Map<String, Object> response = new HashMap<>();
        response.put("result", false);
        response.put("error", ex.getMessageIntoJson());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception ex) {
        logger.error(ex.getMessage(), ex);
        Map<String, Object> response = new HashMap<>();
        response.put("result", false);
        response.put("error", ex.getClass().getSimpleName() + ": " + ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
