package ca.corbett.movienight.api.util;

import ca.corbett.movienight.api.dto.ErrorResponse;
import ca.corbett.movienight.api.handler.StreamHandler;
import ca.corbett.movienight.db.Database;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Maps exceptions thrown by handlers and services into appropriate HTTP
 * status codes and {@link ErrorResponse} bodies.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class ExceptionMapper {

    private static final Logger log = Logger.getLogger(ExceptionMapper.class.getName());

    private ExceptionMapper() {
        // Utility class — no instances.
    }

    /**
     * Maps an exception to an {@link ErrorResponse} and HTTP status code.
     * <p>
     * If the exception is an {@link IOException} that wraps a known
     * exception (e.g., {@link Database.NotFoundException}, {@link IllegalArgumentException}),
     * the cause is used for mapping instead.
     *
     * @param ex the exception to map
     * @return a two-element array: [status code, ErrorResponse]
     */
    public static Object[] map(Throwable ex) {
        // Unwrap IOException if it wraps a known exception type
        Throwable cause = ex;
        if (ex instanceof IOException ioEx && ioEx.getCause() != null) {
            cause = ioEx.getCause();
        }

        if (cause instanceof Database.NotFoundException nfe) {
            return new Object[]{
                    HttpURLConnection.HTTP_NOT_FOUND,
                    new ErrorResponse("Not Found", nfe.getMessage(), HttpURLConnection.HTTP_NOT_FOUND)
            };
        }

        if (cause instanceof StreamHandler.RangeNotSatisfiableException rnse) {
            return new Object[]{
                    416, // HTTP 416 Range Not Satisfiable
                    new ErrorResponse("Range Not Satisfiable", rnse.getMessage(), 416)
            };
        }

        if (cause instanceof IllegalArgumentException iae) {
            return new Object[]{
                    HttpURLConnection.HTTP_BAD_REQUEST,
                    new ErrorResponse("Bad Request", iae.getMessage(), HttpURLConnection.HTTP_BAD_REQUEST)
            };
        }

        if (cause instanceof JsonProcessingException jpe) {
            return new Object[]{
                    HttpURLConnection.HTTP_BAD_REQUEST,
                    new ErrorResponse("Bad Request", "Malformed JSON: " + jpe.getOriginalMessage(),
                                      HttpURLConnection.HTTP_BAD_REQUEST)
            };
        }

        if (cause instanceof SQLException sqle) {
            log.log(Level.SEVERE, "Database error", sqle);
            return new Object[]{
                    HttpURLConnection.HTTP_INTERNAL_ERROR,
                    new ErrorResponse("Internal Server Error", "A database error occurred.",
                                      HttpURLConnection.HTTP_INTERNAL_ERROR)
            };
        }

        // Check the original exception for types we didn't unwrap
        if (ex instanceof Database.NotFoundException nfe) {
            return new Object[]{
                    HttpURLConnection.HTTP_NOT_FOUND,
                    new ErrorResponse("Not Found", nfe.getMessage(), HttpURLConnection.HTTP_NOT_FOUND)
            };
        }

        if (ex instanceof IllegalArgumentException iae) {
            return new Object[]{
                    HttpURLConnection.HTTP_BAD_REQUEST,
                    new ErrorResponse("Bad Request", iae.getMessage(), HttpURLConnection.HTTP_BAD_REQUEST)
            };
        }

        if (ex instanceof JsonProcessingException jpe) {
            return new Object[]{
                    HttpURLConnection.HTTP_BAD_REQUEST,
                    new ErrorResponse("Bad Request", "Malformed JSON: " + jpe.getOriginalMessage(),
                                      HttpURLConnection.HTTP_BAD_REQUEST)
            };
        }

        if (ex instanceof SQLException sqle) {
            log.log(Level.SEVERE, "Database error", sqle);
            return new Object[]{
                    HttpURLConnection.HTTP_INTERNAL_ERROR,
                    new ErrorResponse("Internal Server Error", "A database error occurred.",
                                      HttpURLConnection.HTTP_INTERNAL_ERROR)
            };
        }

        // Catch-all for unexpected exceptions
        log.log(Level.SEVERE, "Unexpected error", ex);
        return new Object[]{
                HttpURLConnection.HTTP_INTERNAL_ERROR,
                new ErrorResponse("Internal Server Error", "An unexpected error occurred.",
                                  HttpURLConnection.HTTP_INTERNAL_ERROR)
        };
    }
}
