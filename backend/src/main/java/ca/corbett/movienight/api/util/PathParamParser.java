package ca.corbett.movienight.api.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts numeric path parameters from a request path.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class PathParamParser {

    private static final Pattern LONG_ID_PATTERN = Pattern.compile("(\\d+)");

    private PathParamParser() {
        // Utility class — no instances.
    }

    /**
     * Extracts a numeric ID from the given path.
     * <p>
     * Matches the last sequence of digits in the path (e.g., {@code /api/media-groups/123}
     * yields {@code 123}). Returns {@code null} if no numeric ID is found.
     *
     * @param path the request path
     * @return the extracted ID, or {@code null} if not found
     */
    public static Long extractId(String path) {
        Matcher matcher = LONG_ID_PATTERN.matcher(path);
        Long lastId = null;
        while (matcher.find()) {
            try {
                lastId = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                // Skip non-numeric matches
            }
        }
        return lastId;
    }

    /**
     * Validates that the given ID is positive.
     *
     * @param id the ID to validate
     * @throws IllegalArgumentException if the ID is {@code null} or not positive
     */
    public static void validatePositive(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Resource ID is required");
        }
        if (id <= 0) {
            throw new IllegalArgumentException("Resource ID must be greater than 0");
        }
    }

    /**
     * Extracts and validates a numeric ID from the given path.
     *
     * @param path the request path
     * @return the validated positive ID
     */
    public static long extractAndValidateId(String path) {
        Long id = extractId(path);
        validatePositive(id);
        return id;
    }
}
