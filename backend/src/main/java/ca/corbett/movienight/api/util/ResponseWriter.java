package ca.corbett.movienight.api.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

/**
 * Utility for writing HTTP responses.
 * <p>
 * Handles serialization to JSON, setting appropriate headers, and flushing
 * the response body. All responses use {@code application/json; charset=utf-8}.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class ResponseWriter {

    private static final String CONTENT_TYPE = "application/json; charset=utf-8";

    private final ObjectMapper objectMapper;

    public ResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Writes a JSON response body with the given status code.
     *
     * @param exchange   the HTTP exchange
     * @param statusCode the HTTP status code
     * @param body       the object to serialize as JSON
     * @throws IOException if serialization or writing fails
     */
    public void writeJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] bytes = objectMapper.writeValueAsBytes(body);

        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE);
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Writes a {@code 204 No Content} response with no body.
     *
     * @param exchange the HTTP exchange
     * @throws IOException if writing fails
     */
    public void writeNoContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_NO_CONTENT, -1);
    }

    /**
     * Writes a plain-text response body with the given status code.
     *
     * @param exchange   the HTTP exchange
     * @param statusCode the HTTP status code
     * @param text       the plain text body
     * @throws IOException if writing fails
     */
    public void writeText(HttpExchange exchange, int statusCode, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
