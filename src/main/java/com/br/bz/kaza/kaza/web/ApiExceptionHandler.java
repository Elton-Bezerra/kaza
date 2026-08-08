package com.br.bz.kaza.kaza.web;

import com.br.bz.kaza.kaza.service.email.EmailDispatchException;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import com.br.bz.kaza.kaza.security.AdminAuthHttpException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> badRequest(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, String> conflict(IllegalStateException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    Map<String, String> forbidden(AccessDeniedException e) {
        return Map.of("error", "forbidden");
    }

    @ExceptionHandler(AdminAuthHttpException.class)
    ResponseEntity<Map<String, String>> adminAuth(AdminAuthHttpException e) {
        String error = e.getStatus() == HttpStatus.FORBIDDEN ? "forbidden" : "unauthorized";
        return ResponseEntity.status(e.getStatus()).body(Map.of("error", error));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    Map<String, String> tooManyRequests(TooManyRequestsException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(EmailDispatchException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    Map<String, String> emailDispatch(EmailDispatchException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> validation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return Map.of("error", message);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    Map<String, String> uploadTooLarge() {
        return Map.of("error", "Document must not exceed 10 MB");
    }
}
