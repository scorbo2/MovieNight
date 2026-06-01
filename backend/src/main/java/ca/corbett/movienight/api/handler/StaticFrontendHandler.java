package ca.corbett.movienight.api.handler;

import ca.corbett.movienight.config.AppConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HTTP handler for serving static frontend files from the {@code static/frontend} resource directory.
 * <p>
 * Routes:
 * <ul>
 *     <li>GET / (and sub-paths)</li>
 * </ul>
 * <p>
 * This handler:
 * <ul>
 *     <li>Serves files from the classpath resource directory {@code /static/frontend/}</li>
 *     <li>Supports content negotiation with proper MIME types</li>
 *     <li>Handles index.html as a default file for directory requests</li>
 *     <li>Returns 404 for missing files</li>
 *     <li>Prevents directory traversal attacks</li>
 *     <li>Injects runtime-configured values (API base path, page size, etc.) into index.html</li>
 * </ul>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class StaticFrontendHandler implements HttpHandler {

    private static final Logger log = Logger.getLogger(StaticFrontendHandler.class.getName());

    private static final String RESOURCE_BASE = "/static/frontend";
    private static final String INDEX_FILE = "index.html";

    private final AppConfig config;

    public StaticFrontendHandler(AppConfig config) {
        this.config = config;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        log.fine("Received request for " + exchange.getRequestURI());
        try {
            String method = exchange.getRequestMethod();

            if (!"GET".equals(method) && !"HEAD".equals(method)) {
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                byte[] body = "{\"error\":\"Method Not Allowed\",\"message\":\"Only GET and HEAD are allowed\",\"status\":405}"
                        .getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(405, body.length);
                try (var os = exchange.getResponseBody()) {
                    os.write(body);
                }
                return;
            }

            String path = exchange.getRequestURI().getPath();
            handleRequest(exchange, path);
        }
        catch (Exception e) {
            log.log(Level.WARNING, "Error handling static file request", e);
            try {
                exchange.sendResponseHeaders(500, -1);
            }
            catch (IOException ignored) {
            }
        }
    }

    private void handleRequest(HttpExchange exchange, String path) throws IOException {
        // Decode the path to handle URL-encoded characters
        String resourcePath = URLDecoder.decode(path, StandardCharsets.UTF_8);

        // Remove leading slash for resource path
        if (resourcePath.startsWith("/")) {
            resourcePath = resourcePath.substring(1);
        }

        // Security: Prevent directory traversal attacks
        if (resourcePath.contains("..")) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        // Build the full resource path
        String fullResourcePath = RESOURCE_BASE + "/" + resourcePath;

        // If the path is empty or a directory, try to serve index.html
        if (resourcePath.isEmpty() || resourcePath.endsWith("/")) {
            fullResourcePath = RESOURCE_BASE + "/" + resourcePath + INDEX_FILE;
        }

        // Try to load the resource from the classpath
        try (InputStream resourceStream = getClass().getResourceAsStream(fullResourcePath)) {
            if (resourceStream == null) {
                // Resource not found, try serving index.html for SPA routing
                try (InputStream indexStream = getClass().getResourceAsStream(RESOURCE_BASE + "/" + INDEX_FILE)) {
                    if (indexStream != null) {
                        serveResource(exchange, indexStream, "text/html; charset=utf-8");
                    }
                    else {
                        exchange.sendResponseHeaders(404, -1);
                    }
                }
                return;
            }

            // Determine MIME type based on file extension
            String mimeType = getMimeType(fullResourcePath);

            serveResource(exchange, resourceStream, mimeType);
        }
    }

    private void serveResource(HttpExchange exchange, InputStream resourceStream, String mimeType)
            throws IOException {
        byte[] buffer = readAllBytes(resourceStream);

        // Inject runtime-configured values into index.html
        if (mimeType.startsWith("text/html")) {
            buffer = injectConfigValues(buffer);
        }

        exchange.getResponseHeaders().set("Content-Type", mimeType);
        exchange.sendResponseHeaders(200, buffer.length);

        if (!"HEAD".equals(exchange.getRequestMethod())) {
            try (var os = exchange.getResponseBody()) {
                os.write(buffer);
            }
        }
    }

    /**
     * Certain values that are configurable on the backend need to be pushed to the UI
     * somehow. We use injection for that purpose, so that the UI code can reference them
     * as global variables without needing to make an API call first.
     */
    private byte[] injectConfigValues(byte[] htmlBytes) {
        String html = new String(htmlBytes, StandardCharsets.UTF_8);
        String apiBasePath = config.getApiBasePath();
        int pageSize = config.getPageSize();

        // Escape the API base path for safe JavaScript string literal (and prevent </script> injection)
        String escapedPath = apiBasePath.replace("\\", "\\\\")
                                        .replace("\"", "\\\"")
                                        .replace("\n", "\\n")
                                        .replace("\r", "\\r")
                                        .replace("<", "\\u003c");
        String injection = "<script>window.MOVIENIGHT_CONFIG = { API_BASE_PATH: \"" + escapedPath
                + "\", PAGE_SIZE: " + pageSize + " };</script>";

        // Insert the script right before </head>, if there is a head element. But only do it once!
        String result = html.replaceFirst("</head>", injection + "</head>");
        if (!result.contains(injection)) {
            // Fallback if no </head> tag found: insert at the beginning
            result = injection + html;
        }

        return result.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] readAllBytes(InputStream stream) throws IOException {
        try (stream) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            stream.transferTo(baos);
            return baos.toByteArray();
        }
    }

    private String getMimeType(String resourcePath) {
        String path = resourcePath.toLowerCase();

        if (path.endsWith(".html") || path.endsWith(".htm")) {
            return "text/html; charset=utf-8";
        }
        else if (path.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        else if (path.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        else if (path.endsWith(".json")) {
            return "application/json; charset=utf-8";
        }
        else if (path.endsWith(".svg")) {
            return "image/svg+xml";
        }
        else if (path.endsWith(".png")) {
            return "image/png";
        }
        else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        else if (path.endsWith(".gif")) {
            return "image/gif";
        }
        else if (path.endsWith(".webp")) {
            return "image/webp";
        }
        else if (path.endsWith(".ico")) {
            return "image/x-icon";
        }
        else if (path.endsWith(".woff")) {
            return "font/woff";
        }
        else if (path.endsWith(".woff2")) {
            return "font/woff2";
        }
        else if (path.endsWith(".ttf")) {
            return "font/ttf";
        }
        else if (path.endsWith(".eot")) {
            return "application/vnd.ms-fontobject";
        }
        else if (path.endsWith(".txt")) {
            return "text/plain; charset=utf-8";
        }
        else {
            return "application/octet-stream";
        }
    }
}

