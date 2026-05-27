package ca.corbett.movienight.api.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;

/**
 * Parses request bodies from {@link HttpExchange} instances.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class RequestParser {

    private final ObjectMapper objectMapper;

    public RequestParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Reads the request body and deserializes it into the given type.
     *
     * @param exchange the HTTP exchange
     * @param targetType the target class to deserialize into
     * @param <T> the target type
     * @return the deserialized object
     * @throws IOException if reading or deserialization fails
     */
    public <T> T readBody(HttpExchange exchange, Class<T> targetType) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return objectMapper.readValue(is, targetType);
        }
    }

    /**
     * Checks whether the request has a non-empty body.
     *
     * @param exchange the HTTP exchange
     * @return {@code true} if the request has a body
     */
    public boolean hasBody(HttpExchange exchange) {
        String contentLengthHeader = exchange.getRequestHeaders().getFirst("Content-Length");
        if (contentLengthHeader != null) {
            try {
                return Long.parseLong(contentLengthHeader) > 0;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        // If no Content-Length, check Transfer-Encoding: chunked
        String transferEncoding = exchange.getRequestHeaders().getFirst("Transfer-Encoding");
        return "chunked".equalsIgnoreCase(transferEncoding);
    }
}
