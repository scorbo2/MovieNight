package ca.corbett.movienight.controller;

import ca.corbett.movienight.service.MediaService;
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

    private boolean rangeLimitWarningIssued = false;

    public StreamController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    /**
     * Streams a video file to the client, with support for HTTP range requests.
     * The id encodes the media type and numeric ID: "M" + movieId or "E" + episodeId.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> streamVideo(@PathVariable String id,
                                         @RequestHeader HttpHeaders headers) {
        // We'll log ONE notice about the configured range request limit:
        if (!rangeLimitWarningIssued) {
            if (rangeRequestMaxChunkMB <= 0) {
                logger.warn("StreamController range request chunk size is unlimited - this may cause memory issues. "
                                    + "You can control this with the movienight.max-range-request-size-mb property.");
            }
            else {
                logger.info("StreamController range request chunk size set to {} MB", rangeRequestMaxChunkMB);
            }
            rangeLimitWarningIssued = true;
        }

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
        int maxChunkSize = rangeLimitMB == 0 ? Integer.MAX_VALUE : 1024 * 1024 * rangeLimitMB;
        int bytesToRead = (int) Math.min(rangeLength, maxChunkSize);
        if (bytesToRead < rangeLength) {
            logger.info("Client requested {} bytes with offset {} for media id {}. " +
                                "Supplying configured max range of {} MB instead.",
                        rangeLength, start, id, rangeLimitMB);
        }
        end = start + bytesToRead - 1;
        rangeLength = bytesToRead;

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
}
