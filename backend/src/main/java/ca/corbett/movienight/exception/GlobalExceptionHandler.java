package ca.corbett.movienight.exception;

import ca.corbett.movienight.config.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException exception,
                                                                          HttpServletRequest request) {
        HttpStatusCode statusCode = exception.getStatusCode();
        String reason = exception.getReason();
        if (reason == null || reason.isBlank()) {
            reason = statusCode.toString();
        }

        logException(statusCode, request, reason, exception);
        return buildResponse(statusCode, reason, request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                         HttpServletRequest request) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();
        String message = "Validation failed";

        logger.warn("Validation failed for {} {}: {}",
                request.getMethod(),
                request.getRequestURI(),
                String.join("; ", details));

        return buildResponse(HttpStatus.BAD_REQUEST, message, request, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception,
                                                                      HttpServletRequest request) {
        List<String> details = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        String message = "Validation failed";

        logger.warn("Constraint violation for {} {}: {}",
                request.getMethod(),
                request.getRequestURI(),
                String.join("; ", details));

        return buildResponse(HttpStatus.BAD_REQUEST, message, request, details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnhandledException(Exception exception,
                                                                     HttpServletRequest request) {
        logger.error("Unhandled exception for {} {}", request.getMethod(), request.getRequestURI(), exception);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, List.of());
    }

    @ExceptionHandler({
            AsyncRequestNotUsableException.class,
            ClientAbortException.class
    })
    public void handleClientAbort(Exception e) {
        // Client disconnected mid-stream — completely normal for video playback, ignore it
        logger.debug("Client disconnected during streaming (normal): {}", e.getMessage());
    }

    private void logException(HttpStatusCode statusCode,
                              HttpServletRequest request,
                              String message,
                              Exception exception) {
        if (statusCode.is5xxServerError()) {
            logger.error("Request failed for {} {}: {}", request.getMethod(), request.getRequestURI(), message, exception);
            return;
        }
        logger.warn("Request failed for {} {}: {}", request.getMethod(), request.getRequestURI(), message);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatusCode statusCode,
                                                           String message,
                                                           HttpServletRequest request,
                                                           List<String> details) {
        return ResponseEntity.status(statusCode)
                .body(new ApiErrorResponse(
                        OffsetDateTime.now(),
                        statusCode.value(),
                        resolveError(statusCode),
                        message,
                        request.getRequestURI(),
                        resolveCorrelationId(request),
                        details
                ));
    }

    private String formatFieldError(FieldError fieldError) {
        String message = fieldError.getDefaultMessage();
        if (message == null || message.isBlank()) {
            message = "is invalid";
        }
        return fieldError.getField() + ": " + message;
    }

    private String resolveError(HttpStatusCode statusCode) {
        if (statusCode instanceof HttpStatus httpStatus) {
            return httpStatus.getReasonPhrase();
        }
        return "HTTP " + statusCode.value();
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        return correlationId == null ? null : correlationId.toString();
    }
}
