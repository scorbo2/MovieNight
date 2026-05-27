package ca.corbett.movienight.api.util;

import java.util.Map;
import java.util.Optional;

/**
 * Parses query parameters from an {@link com.sun.net.httpserver.HttpExchange}.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class QueryParamParser {

    private QueryParamParser() {
        // Utility class — no instances.
    }

    /**
     * Extracts query parameters from the request URI.
     *
     * @param queryString the raw query string (may be {@code null} or empty)
     * @return a map of parameter names to their first values
     */
    public static Map<String, String> parse(String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return Map.of();
        }

        Map<String, String> params = new java.util.HashMap<>();
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = java.net.URLDecoder.decode(pair.substring(0, idx),
                                                        java.nio.charset.StandardCharsets.UTF_8);
                String value = java.net.URLDecoder.decode(pair.substring(idx + 1),
                                                          java.nio.charset.StandardCharsets.UTF_8);
                // First value wins
                params.putIfAbsent(key, value);
            }
        }
        return params;
    }

    /**
     * Returns the first value for the given parameter, or empty if absent.
     *
     * @param params the parsed query parameter map
     * @param name   the parameter name
     * @return the parameter value, or {@link Optional#empty()}
     */
    public static Optional<String> get(Map<String, String> params, String name) {
        return Optional.ofNullable(params.get(name));
    }

    /**
     * Returns the first value for the given parameter, or the default if absent.
     *
     * @param params       the parsed query parameter map
     * @param name         the parameter name
     * @param defaultValue the default value
     * @return the parameter value, or the default
     */
    public static String getOrDefault(Map<String, String> params, String name, String defaultValue) {
        return params.getOrDefault(name, defaultValue);
    }

    /**
     * Parses a parameter as an integer.
     *
     * @param params the parsed query parameter map
     * @param name   the parameter name
     * @return the parsed integer value
     * @throws IllegalArgumentException if the parameter is absent or not a valid integer
     */
    public static int parseInt(Map<String, String> params, String name) {
        String value = getRequired(params, name);
        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a valid integer");
        }
    }

    /**
     * Parses a parameter as an integer, returning empty if absent.
     *
     * @param params the parsed query parameter map
     * @param name   the parameter name
     * @return the parsed integer value, or {@link Optional#empty()}
     */
    public static Optional<Integer> parseIntOptional(Map<String, String> params, String name) {
        return get(params, name).map(v -> {
            try {
                return Integer.parseInt(v);
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException(name + " must be a valid integer");
            }
        });
    }

    /**
     * Parses a parameter as a long.
     *
     * @param params the parsed query parameter map
     * @param name   the parameter name
     * @return the parsed long value
     * @throws IllegalArgumentException if the parameter is absent or not a valid long
     */
    public static long parseLong(Map<String, String> params, String name) {
        String value = getRequired(params, name);
        try {
            return Long.parseLong(value);
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a valid number");
        }
    }

    /**
     * Parses a parameter as a long, returning empty if absent.
     *
     * @param params the parsed query parameter map
     * @param name   the parameter name
     * @return the parsed long value, or {@link Optional#empty()}
     */
    public static Optional<Long> parseLongOptional(Map<String, String> params, String name) {
        return get(params, name).map(v -> {
            try {
                return Long.parseLong(v);
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException(name + " must be a valid number");
            }
        });
    }

    /**
     * Parses a parameter as a boolean.
     *
     * @param params the parsed query parameter map
     * @param name   the parameter name
     * @return the parsed boolean value
     * @throws IllegalArgumentException if the parameter is absent or not a valid boolean
     */
    public static boolean parseBoolean(Map<String, String> params, String name) {
        String value = getRequired(params, name);
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException(name + " must be 'true' or 'false'");
    }

    /**
     * Parses a parameter as a boolean, returning empty if absent.
     *
     * @param params the parsed query parameter map
     * @param name   the parameter name
     * @return the parsed boolean value, or {@link Optional#empty()}
     */
    public static Optional<Boolean> parseBooleanOptional(Map<String, String> params, String name) {
        return get(params, name).map(v -> {
            if ("true".equalsIgnoreCase(v)) { return true; }
            if ("false".equalsIgnoreCase(v)) { return false; }
            throw new IllegalArgumentException(name + " must be 'true' or 'false'");
        });
    }

    private static String getRequired(Map<String, String> params, String name) {
        String value = params.get(name);
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
