package com.malvinas.personal.infrastructure.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        f -> f.getDefaultMessage() != null ? f.getDefaultMessage() : "Valor invalido",
                        (a, b) -> a));
        log.warn("Validacion fallida [{}]: {}", req.getRequestURI(), fields);
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, "Error de validacion", req, fields));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest req) {
        log.warn("Regla de negocio [{}]: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.of(ex.getStatus(), ex.getMessage(), req, null));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        log.info("No encontrado [{}]: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND, ex.getMessage(), req, null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN, "Acceso denegado", req, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Error no controlado [{}]: {}", req.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", req, null));
    }

    public record ErrorResponse(
            int status, String error, String message, String path,
            OffsetDateTime timestamp, Map<String, String> fields) {
        static ErrorResponse of(HttpStatus status, String message,
                                HttpServletRequest req, Map<String, String> fields) {
            return new ErrorResponse(status.value(), status.getReasonPhrase(),
                    message, req.getRequestURI(), OffsetDateTime.now(), fields);
        }
    }
}
