package ca.corbett.movienight.api.handler;

import ca.corbett.movienight.api.util.ResponseWriter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Handles the {@code GET /api/health} endpoint.
 * <p>
 * Returns a simple JSON response confirming the server is alive.
 * This is primarily for smoke testing and health monitoring.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class HealthHandler implements HttpHandler {

    private final ResponseWriter responseWriter;
    private final String apiBasePath;

    public HealthHandler(ResponseWriter responseWriter, String apiBasePath) {
        this.responseWriter = responseWriter;
        this.apiBasePath = apiBasePath;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            byte[] body = ("""
                    {"error":"Method Not Allowed","message":"Only GET is allowed on {basePath}health","status":405}
                    """.replace("{basePath}", apiBasePath)).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(405, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
            return;
        }

        HealthResponse response = new HealthResponse("ok");
        responseWriter.writeJson(exchange, HttpURLConnection.HTTP_OK, response);
    }

    /**
     * Simple health check response envelope.
     */
    public record HealthResponse(String status) {
    }
}
