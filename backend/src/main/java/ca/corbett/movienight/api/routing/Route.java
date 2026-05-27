package ca.corbett.movienight.api.routing;

import java.util.Objects;

/**
 * Represents a single route: an HTTP method and path pattern.
 * <p>
 * Routes are matched against incoming requests in the order they are registered.
 * The first matching route wins.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class Route {

    private final String method;
    private final String path;

    public Route(String method, String path) {
        this.method = Objects.requireNonNull(method, "method cannot be null");
        this.path = Objects.requireNonNull(path, "path cannot be null");
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Route route)) return false;
        return method.equals(route.method) && path.equals(route.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, path);
    }
}
