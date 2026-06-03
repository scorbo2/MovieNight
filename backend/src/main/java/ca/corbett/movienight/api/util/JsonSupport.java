package ca.corbett.movienight.api.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Shared Jackson {@link ObjectMapper} configuration.
 * <p>
 * Provides a single, thread-safe instance configured for our API needs:
 * <ul>
 *   <li>Proper handling of {@link java.time.LocalDate} via {@code JavaTimeModule}.</li>
 *   <li>Readable JSON output (pretty-print disabled for production, enabled for debug).</li>
 *   <li>Fail-on-unknown-properties to reject requests with unexpected fields.</li>
 * </ul>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class JsonSupport {

    private static final ObjectMapper INSTANCE;

    static {
        INSTANCE = new ObjectMapper();
        INSTANCE.registerModule(new JavaTimeModule());
        INSTANCE.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        INSTANCE.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        INSTANCE.disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private JsonSupport() {
        // Utility class — no instances.
    }

    /**
     * Returns the shared, thread-safe {@link ObjectMapper} instance.
     */
    public static ObjectMapper getObjectMapper() {
        return INSTANCE;
    }
}
