package org.example.cloudapp.config.handler;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.example.cloudapp.dto.ApiErrorDto;
import org.example.cloudapp.exception.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object validation(MethodArgumentNotValidException ex, HttpServletRequest request, Model model) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();
        return handle("Некорректные данные формы", details, HttpStatus.BAD_REQUEST, request, model);
    }

    @ExceptionHandler(AppException.class)
    public Object app(AppException ex, HttpServletRequest request, Model model) {
        return handle(ex.getMessage(), List.of(), HttpStatus.BAD_REQUEST, request, model);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Object missingResource(NoResourceFoundException ex, HttpServletRequest request, Model model) {
        return handle("Ресурс не найден", List.of(), HttpStatus.NOT_FOUND, request, model);
    }

    @ExceptionHandler(Exception.class)
    public Object internal(Exception ex, HttpServletRequest request, Model model) {
        log.error("Unhandled request error", ex);
        return handle("Внутренняя ошибка приложения", List.of(), HttpStatus.INTERNAL_SERVER_ERROR, request, model);
    }

    private Object handle(String message, List<String> details, HttpStatus status, HttpServletRequest request, Model model) {
        log.warn("Request failed: {} {} {}", status.value(), request.getRequestURI(), message);
        if (wantsJson(request)) {
            return ResponseEntity.status(status)
                    .body(new ApiErrorDto(message, request.getRequestURI(), status.value(), Instant.now(), details));
        }
        model.addAttribute("status", status.value());
        model.addAttribute("message", message);
        model.addAttribute("details", details);
        return "error/custom";
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private boolean wantsJson(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        String requestedWith = request.getHeader("X-Requested-With");
        return uri.startsWith("/api/")
                || uri.startsWith("/ajax/")
                || "XMLHttpRequest".equals(requestedWith)
                || (accept != null && accept.contains("application/json"));
    }
}
