package lk.ac.nsbm.autocare.exception;

import lk.ac.nsbm.autocare.controller.PartRestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * The REST half of the centralised error handling.
 *
 * The same AutoCareException hierarchy serves both front ends: Thymeleaf
 * renders it as a page, this advice renders it as JSON with the status the
 * exception itself nominates. Neither controller contains a try/catch.
 */
@RestControllerAdvice(assignableTypes = PartRestController.class)
public class RestExceptionHandler {

    @ExceptionHandler(AutoCareException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessFailure(AutoCareException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", ex.getHttpStatus().value());
        body.put("errorCode", ex.getErrorCode());
        body.put("title", ex.getTitle());
        body.put("message", ex.getUserMessage());
        body.put("action", ex.getSuggestedAction());
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    /** Bean Validation failures on @RequestBody, reported field by field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new TreeMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", 400);
        body.put("errorCode", "VALIDATION_FAILED");
        body.put("title", "Invalid part details");
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }
}
