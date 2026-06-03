package ca.corbett.movienight.api.routing;

import ca.corbett.movienight.api.dto.ErrorResponse;
import ca.corbett.movienight.api.util.ExceptionMapper;
import ca.corbett.movienight.api.util.ResponseWriter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Internal router that matches HTTP method and normalized path against
 * registered routes.
 * <p>
 * The router maintains a list of route-to-handler pairs. When a request
 * arrives, it finds the first matching route. If no route matches, it
 * returns 404. If the path matches but the method does not, it returns
 * 405.
 * <p>
 * Supports handler delegation: if a handler throws a {@code RuntimeException}
 * with message "ROUTE_NOT_MATCHED", the router tries the next matching route.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class Router {

    private static final Logger log = Logger.getLogger(Router.class.getName());

    private final List<RouteEntry> routes = new ArrayList<>();
    private final ResponseWriter responseWriter;

    public Router(ResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    /**
     * Registers a handler for a given route.
     *
     * @param route   the route (method + path pattern)
     * @param handler the handler to invoke for matching requests
     */
    public void register(Route route, HttpHandler handler) {
        routes.add(new RouteEntry(route, handler));
    }

    /**
     * Handles the incoming exchange by finding a matching route.
     *
     * @param exchange the HTTP exchange
     */
    public void handle(HttpExchange exchange) {
        String method = exchange.getRequestMethod();
        String path = normalizePath(exchange.getRequestURI().getPath());

        // Find the first matching route index
        int startIdx = -1;
        for (int i = 0; i < routes.size(); i++) {
            if (matches(routes.get(i).route.getPath(), path)
                    && routes.get(i).route.getMethod().equals(method)) {
                startIdx = i;
                break;
            }
        }

        if (startIdx == -1) {
            // No route matched at all
            boolean pathMatched = false;
            for (RouteEntry entry : routes) {
                if (matches(entry.route.getPath(), path)) {
                    pathMatched = true;
                    break;
                }
            }
            if (pathMatched) {
                sendMethodNotAllowed(exchange, "Method Not Allowed",
                                     "The HTTP method " + method + " is not allowed for this endpoint.");
            }
            else {
                sendNotFound(exchange, "Not Found", "No resource found at " + path);
            }
            return;
        }

        tryRoute(exchange, startIdx, method, path);
    }

    /**
     * Tries to handle the request using the route at the given index.
     * If the handler throws "ROUTE_NOT_MATCHED", tries the next route.
     */
    private void tryRoute(HttpExchange exchange, int idx, String method, String path) {
        if (idx >= routes.size()) {
            // No more routes to try
            boolean pathMatched = false;
            for (RouteEntry entry : routes) {
                if (matches(entry.route.getPath(), path)) {
                    pathMatched = true;
                    break;
                }
            }
            if (pathMatched) {
                sendMethodNotAllowed(exchange, "Method Not Allowed",
                                     "The HTTP method " + method + " is not allowed for this endpoint.");
            }
            else {
                sendNotFound(exchange, "Not Found", "No resource found at " + path);
            }
            return;
        }

        RouteEntry entry = routes.get(idx);
        if (matches(entry.route.getPath(), path)
                && entry.route.getMethod().equals(method)) {
            try {
                entry.handler.handle(exchange);
            }
            catch (IOException e) {
                try {
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException re
                            && "ROUTE_NOT_MATCHED".equals(re.getMessage())) {
                        // Try the next route
                        tryRoute(exchange, idx + 1, method, path);
                        return;
                    }
                    // Use ExceptionMapper for consistent error handling
                    Object[] mapped = ExceptionMapper.map(e);
                    int statusCode = (int)mapped[0];
                    ErrorResponse body = (ErrorResponse)mapped[1];
                    responseWriter.writeJson(exchange, statusCode, body);
                }
                catch (IOException ioEx) {
                    log.severe("Error writing error response: " + ioEx.getMessage());
                }
            }
        }
        else {
            // Route didn't match, try next
            tryRoute(exchange, idx + 1, method, path);
        }
    }

    private boolean matches(String pattern, String path) {
        String normalizedPattern = normalizePath(pattern);
        String normalizedPath = path;

        // Check if the pattern is a prefix match (collection-style routes)
        if (normalizedPattern.endsWith("/")) {
            return normalizedPath.startsWith(normalizedPattern);
        }

        // Check for exact match
        if (normalizedPath.equals(normalizedPattern)) {
            return true;
        }

        // Check if the path is under the pattern (for routes like /api/media-groups)
        // A path like /api/media-groups/123/items starts with /api/media-groups/
        if (normalizedPath.startsWith(normalizedPattern + "/")) {
            // Check that the next character after the pattern is a digit (ID) or "items"
            String remainder = normalizedPath.substring(normalizedPattern.length() + 1);
            if (remainder.isEmpty()) {
                return true;
            }
            // If remainder starts with a digit, it's an ID match
            if (Character.isDigit(remainder.charAt(0))) {
                return true;
            }
        }

        return false;
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        // Remove trailing slash (except for root)
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private void sendMethodNotAllowed(HttpExchange exchange, String error, String message) {
        try {
            ErrorResponse body = new ErrorResponse(error, message, 405);
            responseWriter.writeJson(exchange, 405, body);
        }
        catch (IOException e) {
            log.severe("Error writing 405 response: " + e.getMessage());
        }
    }

    private void sendNotFound(HttpExchange exchange, String error, String message) {
        try {
            ErrorResponse body = new ErrorResponse(error, message, HttpURLConnection.HTTP_NOT_FOUND);
            responseWriter.writeJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, body);
        }
        catch (IOException e) {
            log.severe("Error writing 404 response: " + e.getMessage());
        }
    }

    private record RouteEntry(Route route, HttpHandler handler) {
    }
}
