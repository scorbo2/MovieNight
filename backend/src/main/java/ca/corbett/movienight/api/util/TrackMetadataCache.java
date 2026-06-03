package ca.corbett.movienight.api.util;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A package-internal class that manages an in-memory, least-recently-used
 * cache of a fixed size for TrackMetadata objects. This is used internally
 * by TrackMetadataUtil to avoid unnecessary I/O and JSON parsing for
 * frequently accessed media items.
 * <p>
 * This cache is thread-safe.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
class TrackMetadataCache {

    private static final int CACHE_SIZE = 1024; // We could add this to AppConfig, but eh....

    private final Set<File> cacheKeys = new LinkedHashSet<>(CACHE_SIZE);
    private final Map<File, TrackMetadataUtil.MetadataWrapper> cache = new java.util.HashMap<>(CACHE_SIZE);

    public synchronized int size() {
        return cache.size();
    }

    public synchronized void clear() {
        cacheKeys.clear();
        cache.clear();
    }

    /**
     * Returns the TrackMetadata for the given cacheKey if present, or null if not in cache.
     * If the key is present, it is marked as being recently accessed. This saves it from being
     * evicted if the cache overflows.
     */
    public synchronized TrackMetadataUtil.MetadataWrapper get(File cacheKey) {
        if (cache.containsKey(cacheKey)) {
            // Move this key to the end of the access order:
            cacheKeys.remove(cacheKey);
            cacheKeys.add(cacheKey);
            return cache.get(cacheKey);
        }
        return null;
    }

    /**
     * Adds the given TrackMetadata to the cache, or updates the existing entry if the key is already present.
     * If this addition would result in the cache overflowing, the least recently used entry is evicted to make room.
     */
    public synchronized void put(File cacheKey, TrackMetadataUtil.MetadataWrapper metadata) {
        // Do we already have this one?
        // Side effect: if we do, this marks it as recently accessed.
        if (get(cacheKey) != null) {
            // Update the existing value and we're done:
            cache.put(cacheKey, metadata);
            return;
        }

        // Check for cache overflow:
        if (cache.size() >= CACHE_SIZE) {
            // Evict the least recently used entry:
            File lruKey = cacheKeys.iterator().next();
            cacheKeys.remove(lruKey);
            cache.remove(lruKey);
        }

        // Now we can add this guy:
        cacheKeys.add(cacheKey);
        cache.put(cacheKey, metadata);
    }
}
