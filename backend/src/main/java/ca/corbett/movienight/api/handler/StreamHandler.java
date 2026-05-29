package ca.corbett.movienight.api.handler;

import ca.corbett.movienight.config.AppConfig;
import ca.corbett.movienight.db.Database;
import ca.corbett.movienight.model.MediaItem;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HTTP handler for streaming media files to the client.
 * Only MediaItems can be streamed. MediaGroups are simple containers and have no media content of their own.
 * <p>
 * Routes:
 * </p>
 * <ul>
 *     <li>GET /api/stream/{mediaItemId}</li>
 * </ul>
 * <p>
 * Note that this handler has no create, update, or delete operations.
 * This is for simple read-only streaming of MediaItems. To create or
 * manage MediaItems, refer to the MediaItemHandler.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class StreamHandler implements HttpHandler {

    private static final Logger log = Logger.getLogger(StreamHandler.class.getName());

    private final AppConfig appConfig;
    private final Database database;

    public static class RangeNotSatisfiableException extends Exception {
        public RangeNotSatisfiableException(String message) {
            super(message);
        }
    }

    private record HttpRange(long start, long end) {
        public long length() {
            return end - start + 1;
        }
    }

    public StreamHandler(AppConfig appConfig, Database database) {
        this.appConfig = appConfig;
        this.database = database;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equals(method)) {
                handleStream(exchange, path);
            }
            else {
                // Handled by Router (405)
                throw new UnsupportedOperationException("Method " + method + " not supported");
            }
        }
        catch (Exception e) {
            // Getting a lot of these, but they seem harmless.
            if (!"Connection reset by peer".equals(e.getMessage())) {
                throw new IOException(e);
            }
        }
    }

    private void handleStream(HttpExchange exchange, String path) throws Exception {
        // Extract mediaItemId from path
        String prefix = appConfig.getApiBasePath() + "stream/";
        if (!path.startsWith(prefix)) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }
        String mediaItemIdStr = path.substring(prefix.length());
        int mediaItemId;
        try {
            mediaItemId = Integer.parseInt(mediaItemIdStr);
            if (mediaItemId <= 0) {
                throw new NumberFormatException("Media item ID must be a positive integer: " + mediaItemIdStr);
            }
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid media item ID: " + mediaItemIdStr);
        }

        MediaItem item = database.getMediaItemById(mediaItemId);
        if (item == null) {
            throw new Database.NotFoundException("Media item not found with ID: " + mediaItemId);
        }

        Path mediaFilePath = appConfig.getMediaDir().resolve(item.getMediaFilePath()).normalize();
        if (!mediaFilePath.startsWith(appConfig.getMediaDir())) {
            throw new SecurityException("Media file path is outside of data directory: " + mediaFilePath);
        }
        File mediaFile = mediaFilePath.toFile();
        if (!mediaFile.exists() || !mediaFile.isFile() || mediaFile.length() == 0) {
            throw new IOException("No valid media file found for media item ID: " + mediaItemId);
        }
        String mimeType = getMediaMimeType(mediaFile);

        try {
            if (exchange.getRequestHeaders().get("Range") != null) {
                HttpRange range;
                try {
                    range = parseRangeHeader(exchange.getRequestHeaders().getFirst("Range"), mediaFile.length());
                }
                catch (IllegalArgumentException ignored) {
                    exchange.getResponseHeaders().add("Accept-Ranges", "bytes");
                    throw new RangeNotSatisfiableException("Invalid range header");
                }

                // Optional safety cap to avoid huge in-memory chunks from abusive ranges.
                // By default, we'll limit range requests to 32MB, which is good for streaming.
                long rangeLimit = appConfig.getRangeLimitMB() * 1024 * 1024L; // defaults to 32MB
                long bytesToRead = Math.min(range.length(), rangeLimit);
                if (bytesToRead < rangeLimit) {
                    log.log(Level.INFO, "Client requested {} with offset {} for media id {}. " +
                                    "Supplying smaller range of {} instead.",
                            new Object[]{range.length(), range.start, mediaItemId, bytesToRead});
                }

                exchange.getResponseHeaders().add("Content-Type", mimeType);
                exchange.getResponseHeaders().add("Accept-Ranges", "bytes");
                exchange.getResponseHeaders()
                        .add("Content-Range",
                             "bytes " + range.start + "-" + (range.start + bytesToRead - 1) + "/" + mediaFile.length());
                exchange.sendResponseHeaders(206, Math.min(bytesToRead, mediaFile.length())); // 206 Partial Content
                try (var output = exchange.getResponseBody();
                     var input = new java.io.RandomAccessFile(mediaFile, "r")) {
                    input.seek(range.start);
                    byte[] buffer = new byte[32 * 1024]; // 32KB buffer for streaming
                    long bytesToWrite = bytesToRead;
                    int bytesRead;
                    while (bytesToWrite > 0 && (bytesRead = input.read(buffer, 0,
                                                                       (int)Math.min(buffer.length,
                                                                                     bytesToWrite))) != -1) {
                        output.write(buffer, 0, bytesRead);
                        bytesToWrite -= bytesRead;
                    }
                }
            }
            else {
                exchange.getResponseHeaders().add("Content-Type", mimeType);
                exchange.getResponseHeaders().add("Accept-Ranges", "bytes");
                exchange.sendResponseHeaders(200, mediaFile.length());
                try (var output = exchange.getResponseBody();
                     var input = new BufferedInputStream(new FileInputStream(mediaFile))) {
                    byte[] buffer = new byte[32 * 1024]; // 32KB buffer for streaming
                    int bytesRead;
                    while ((bytesRead = input.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                }
            }
        }
        finally {
            // Record that this media item was streamed. Non-fatal — if the DB update fails,
            // the stream response is already sent and the client is happy.
            try {
                database.updateMediaItemLastWatchedDate(mediaItemId, LocalDate.now());
            }
            catch (SQLException e) {
                log.log(Level.WARNING, "Failed to update lastWatchedDate for media item " + mediaItemId, e);
            }
        }
    }

    private String getMediaMimeType(File mediaFile) {
        String name = mediaFile.getName().toLowerCase();
        if (!name.contains(".")) {
            name = "something.unknown"; // force the fallback case below
        }
        return switch (name.substring(name.lastIndexOf('.') + 1)) {
            case "mp4" -> "video/mp4";
            case "mkv" -> "video/x-matroska";
            case "avi" -> "video/x-msvideo";
            case "mp3" -> "audio/mpeg";
            case "flac" -> "audio/flac";
            case "wav" -> "audio/wav";
            default -> "application/octet-stream"; // fallback for unknown types
        };
    }

    /**
     * Parses a Range header value per RFC 7233 Section 3.5.
     * <p>
     * Supported formats:
     * <ul>
     *   <li>Single range: {@code bytes=0-100} — first and last byte positions</li>
     *   <li>Open-ended range: {@code bytes=500-} — from byte 500 to end of file</li>
     *   <li>Suffix range: {@code bytes=-500} — last 500 bytes of the file</li>
     *   <li>Multiple ranges: {@code bytes=0-100, 200-300} — returns the first range</li>
     * </ul>
     * <p>
     * Validation rules (RFC 7233 §3.2):
     * <ul>
     *   <li>If first-byte-pos >= file-length → 416 Range Not Satisfiable</li>
     *   <li>If last-byte-pos >= file-length → clamp to file-length - 1</li>
     *   <li>If suffix-length >= file-length → return entire file</li>
     *   <li>If first-byte-pos > last-byte-pos → invalid (except suffix ranges)</li>
     * </ul>
     *
     * @param rangeHeader the Range header value (e.g. "bytes=0-100")
     * @param fileLength  the size of the file in bytes
     * @return the first parsed range
     * @throws IllegalArgumentException if the header is malformed or unsatisfiable
     */
    private HttpRange parseRangeHeader(String rangeHeader, long fileLength) {
        if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
            throw new IllegalArgumentException("Invalid Range header: " + rangeHeader);
        }

        List<HttpRange> ranges = parseRangeHeaderMulti(rangeHeader.substring(6), fileLength);
        if (ranges.isEmpty()) {
            throw new IllegalArgumentException("Empty range list");
        }
        return ranges.get(0);
    }

    /**
     * Parses all ranges from a single Range header value per RFC 7233.
     *
     * @param byteRangeSpec the portion after "bytes=" (e.g. "0-100, 200-300")
     * @param fileLength    the size of the file in bytes
     * @return list of parsed ranges (first one is used by the handler)
     * @throws IllegalArgumentException if no valid ranges are found
     */
    private List<HttpRange> parseRangeHeaderMulti(String byteRangeSpec, long fileLength) {
        List<HttpRange> result = new ArrayList<>();
        String[] rangeSpecs = byteRangeSpec.split(",");

        for (String spec : rangeSpecs) {
            spec = spec.trim();
            if (spec.isEmpty()) {
                continue;
            }

            // Suffix range: bytes=-N (last N bytes)
            if (spec.startsWith("-")) {
                long suffixLength;
                try {
                    suffixLength = Long.parseLong(spec.substring(1).trim());
                }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid suffix range: " + spec);
                }

                long start, end;
                if (suffixLength >= fileLength) {
                    // Suffix is larger than file — entire file
                    start = 0;
                    end = fileLength - 1;
                }
                else {
                    start = fileLength - suffixLength;
                    end = fileLength - 1;
                }
                result.add(new HttpRange(start, end));
                continue;
            }

            // Single range: bytes=start-end or bytes=start-
            String[] parts = spec.split("-", 2);
            long start, end;
            try {
                start = Long.parseLong(parts[0].trim());
                if (parts.length > 1 && !parts[1].isBlank()) {
                    end = Long.parseLong(parts[1].trim());
                }
                else {
                    end = fileLength - 1;
                }
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid range format: " + spec);
            }

            // RFC 7233 §3.2: first-byte-pos >= file-length is unsatisfiable
            if (start >= fileLength) {
                throw new IllegalArgumentException("Range not satisfiable: " + spec);
            }

            // RFC 7233 §3.2: clamp last-byte-pos to file-length - 1
            if (end >= fileLength) {
                end = fileLength - 1;
            }

            if (start > end) {
                throw new IllegalArgumentException("Invalid range (start > end): " + spec);
            }

            result.add(new HttpRange(start, end));
        }

        if (result.isEmpty()) {
            throw new IllegalArgumentException("No valid ranges in: " + byteRangeSpec);
        }
        return result;
    }
}
