package ca.corbett.movienight.api.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

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
    private static final Pattern INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9.-]");
    private static final Pattern LEADING_DOTS = Pattern.compile("^\\.+");
    private static final Pattern WINDOWS_RESERVED = Pattern.compile(
            "^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?$",
            Pattern.CASE_INSENSITIVE
    );

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

    public void writePlaylist(HttpExchange exchange, int statusCode, String playlist) throws IOException {
        writePlaylist(exchange, statusCode, playlist, "playlist");
    }

    /**
     * Writes an M3U playlist response body with the given status code.
     * Sets {@code Content-Type: audio/x-mpegurl} and a
     * {@code Content-Disposition: attachment; filename="playlist.m3u"} header so that
     * browsers download the file with the correct {@code .m3u} extension rather than
     * sniffing the {@code #EXTM3U} header and mis-classifying the response as an HLS
     * ({@code .m3u8}) stream.
     *
     * @param exchange   the HTTP exchange
     * @param statusCode the HTTP status code
     * @param playlist   the M3U playlist content
     * @param filename   the filename to suggest in the Content-Disposition header (without extension)
     * @throws IOException if writing fails
     */
    public void writePlaylist(HttpExchange exchange, int statusCode, String playlist, String filename)
            throws IOException {
        byte[] bytes = playlist.getBytes(StandardCharsets.UTF_8);
        String safeFilename = sanitizeFilename(filename, filename);

        exchange.getResponseHeaders().set("Content-Type", "audio/x-mpegurl; charset=utf-8");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + safeFilename + ".m3u\"");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Given any arbitrary String, returns a sanitized version that is safe to use as a filename
     * on any operating system. If the given input is null or empty, or if the resulting sanitized
     * filename is empty, the given defaultName will be returned.
     * <p>
     * The only allowable characters are alphanumeric characters, dots (.), hyphens (-), and underscores (_).
     * All other characters are replaced with underscores. Leading dots are removed. On Windows,
     * reserved filenames such as "CON" or "AUX" are prefixed with an underscore. The resulting filename
     * is truncated to a maximum of 200 characters.
     * </p>
     * <p>
     * Note: copy+pasted from swing-extras 2.9
     * </p>
     */
    public static String sanitizeFilename(String input, String defaultName) {
        if (input == null || input.trim().isEmpty()) {
            return defaultName;
        }

        String sanitized = INVALID_CHARS.matcher(input).replaceAll("_");
        sanitized = LEADING_DOTS.matcher(sanitized).replaceFirst("");

        if (WINDOWS_RESERVED.matcher(sanitized).matches()) {
            sanitized = "_" + sanitized;
        }

        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200);
        }

        return sanitized.isEmpty() ? defaultName : sanitized;
    }
}
