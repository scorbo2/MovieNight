package ca.corbett.movienight.controller;

import ca.corbett.movienight.service.MediaService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/stream")
@CrossOrigin(origins = "http://localhost:5173")
public class StreamController {

    private static final Logger logger = LoggerFactory.getLogger(StreamController.class);
    private static final MediaType DEFAULT_VIDEO_MEDIA_TYPE = MediaType.parseMediaType("video/mp4");

    private final MediaService mediaService;

    @Value("${movienight.max-range-request-size-mb:32}")
    private int rangeRequestMaxChunkMB;

    @Value("${movienight.enable-vlc-integration:false}")
    private boolean enableVlcIntegration;

    public StreamController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostConstruct
    public void logRangeLimit() {
        if (rangeRequestMaxChunkMB <= 0) {
            logger.warn("StreamController range request chunk size is unlimited - this may cause memory issues. "
                                + "You can control this with the movienight.max-range-request-size-mb property.");
        }
        else {
            logger.info("StreamController range request chunk size set to {} MB", rangeRequestMaxChunkMB);
        }
    }

    /**
     * Streams a video file to the client, with support for HTTP range requests.
     * The id encodes the media type and numeric ID: "M" + movieId or "E" + episodeId.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> streamVideo(@PathVariable String id,
                                         @RequestHeader HttpHeaders headers) {
        String filePath = mediaService.findById(id);
        Path videoPath = Paths.get(filePath);

        if (!Files.exists(videoPath)) {
            logger.warn("Video file not found for media id {} (path: {})", id, filePath);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Video file not found for media id: " + id);
        }

        Resource resource = new FileSystemResource(videoPath);
        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElse(DEFAULT_VIDEO_MEDIA_TYPE);

        List<HttpRange> ranges = headers.getRange();
        if (ranges.isEmpty()) {
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(resource);
        }

        long contentLength;
        try {
            contentLength = resource.contentLength();
        } catch (IOException e) {
            logger.error("Failed to determine content length for media id {} (path: {})", id, filePath, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to read video file for media id: " + id);
        }

        // Dev note: Spring Boot's built-in resource range handling seems
        // broken. It consistently yields conversion errors which neither
        // Copilot nor Claude can solve. So, our "nuclear option" fallback
        // is to bypass it entirely and implement basic range handling ourselves.
        // This may seem like overkill, but it works very nicely!

        HttpRange range = ranges.get(0);
        long start = range.getRangeStart(contentLength);
        long end = range.getRangeEnd(contentLength);
        long rangeLength = end - start + 1;

        // Check for 416 - Range Not Satisfiable
        if (end >= contentLength || start > end) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + contentLength)
                    .build();
        }

        // Optional safety cap to avoid huge in-memory chunks from abusive ranges.
        // By default, we'll limit range requests to 32MB, which is good for streaming,
        // but the user can configure this in our properties. (zero means no limit)
        int rangeLimitMB = Math.max(0, rangeRequestMaxChunkMB); // reject negative config values
        long maxChunkSize = rangeLimitMB == 0 ? Integer.MAX_VALUE : 1024 * 1024 * (long)rangeLimitMB;
        maxChunkSize = Math.min(maxChunkSize, Integer.MAX_VALUE); // cap to max int for array allocation
        int bytesToRead = (int) Math.min(rangeLength, maxChunkSize);
        if (bytesToRead < rangeLength) {
            logger.info("Client requested {} with offset {} for media id {}. " +
                                "Supplying configured max range of {} MB instead.",
                        getPrintableSize(rangeLength), getPrintableSize(start), id, rangeLimitMB);
        }
        end = start + bytesToRead - 1;
        rangeLength = bytesToRead;

        // TODO: A streaming response body might be a better approach than in-memory buffering...
        byte[] data = new byte[bytesToRead];
        try (var input = Files.newInputStream(videoPath)) {
            input.skipNBytes(start);
            int read = input.read(data, 0, bytesToRead);
            if (read < bytesToRead) {
                data = java.util.Arrays.copyOf(data, Math.max(read, 0));
                end = start + data.length - 1;
                rangeLength = data.length;
            }
        } catch (IOException e) {
            logger.error("Failed to read range for media id {} (path: {})", id, filePath, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to stream video file for media id: " + id);
        }

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(mediaType)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + contentLength)
                .contentLength(rangeLength)
                .body(data);
    }

    /**
     * This simple endpoint returns "true" or "false" depending on whether VLC integration is enabled in the properties.
     * This value can't change at runtime, so the UI should only hit it once on startup.
     * We can use this to show or hide the "Watch in VLC" button on each media card.
     */
    @GetMapping("/vlc-enabled")
    public ResponseEntity<String> isVlcIntegrationEnabled() {
        return ResponseEntity.ok(enableVlcIntegration ? "true" : "false");
    }

    /**
     * Returns a VLC style M3U playlist file that points to the streaming endpoint for the given media ID.
     * If the client has VLC installed and associated with M3U files, and has a browser like Firefox that
     * allows automatic launching of an associated application, this can be a good alternative to using
     * the built-in video player. In particular, this is a great option for watching video that has multiple
     * audio tracks, since the built-in video player doesn't support that.
     * <p>
     * Note that this endpoint will work even if the VLC integration is disabled!
     * It's up to the UI to show or hide the VLC option as needed... the API doesn't care.
     * </p>
     */
    @GetMapping("/{id}/playlist")
    public ResponseEntity<String> getPlaylist(@PathVariable String id, HttpServletRequest request) {
        String filePath = mediaService.findById(id);
        Path videoPath = Paths.get(filePath);
        String fileName = videoPath.getFileName().toString();

        if (!Files.exists(videoPath)) {
            logger.warn("Video file not found for media id {} (path: {})", id, filePath);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                              "Video file not found for media id: " + id);
        }

        // Build the stream URL pointing back to our existing streaming endpoint:
        String streamUrl = request.getScheme() + "://" +
                request.getServerName() + ":" +
                request.getServerPort() +
                "/api/stream/" + id;

        // The M3U format is very straightforward:
        String m3u = "#EXTM3U\n#EXTINF:-1," + fileName + "\n" + streamUrl + "\n";

        // VLC will be able to stream directly from our existing streaming endpoint:
        return ResponseEntity.ok()
                             .header(HttpHeaders.CONTENT_TYPE, "audio/x-mpegurl")
                             .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"stream.m3u\"")
                             .body(m3u);
    }

    /**
     * Given a count in bytes, returns a formatted String like "75 MB" or "1.2 GB"
     * for easier readability in logs and error messages.
     *
     * @param bytes a byte count to format
     * @return a human-readable string representing the size in appropriate units (B, KB, MB, GB, etc.)
     */
    public static String getPrintableSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int)(Math.log(bytes) / Math.log(1024));
        String unit = "KMGTPE".charAt(exp - 1) + "B";
        double size = bytes / Math.pow(1024, exp);
        return String.format("%.1f %s", size, unit);

    }
}
