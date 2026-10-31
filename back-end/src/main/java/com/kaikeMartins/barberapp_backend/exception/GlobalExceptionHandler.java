package com.kaikeMartins.barberapp_backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<StandardError> build(HttpStatus status, String error,
                                                String message, HttpServletRequest request) {
        StandardError err = new StandardError(
                Instant.now(),
                status.value(),
                error,
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<StandardError> entityNotFound(NotFoundException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Resource not found", e.getMessage(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<StandardError> badRequest(BadRequestException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Bad request", e.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<StandardError> forbidden(ForbiddenException e, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", e.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<StandardError> accessDenied(AccessDeniedException e, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", "Acesso negado.", request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<StandardError> badCredentials(BadCredentialsException e, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication failed", e.getMessage(), request);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<StandardError> disabled(DisabledException e, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Account not verified",
                "Conta não verificada. Confirme o código enviado por e-mail.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> validation(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation error");

        return build(HttpStatus.BAD_REQUEST, "Validation error", message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> generic(Exception e, HttpServletRequest request) {
        log.error("Erro inesperado em {}", request.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error",
                "Erro interno. Tente novamente em instantes.", request);
    }
}