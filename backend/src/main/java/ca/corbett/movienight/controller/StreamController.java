package ca.corbett.movienight.controller;

import ca.corbett.movienight.service.MediaService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
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

    public StreamController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    /**
     * Streams a video file to the client, with support for HTTP range requests.
     * The id encodes the media type and numeric ID: "M" + movieId or "E" + episodeId.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> streamVideo(@PathVariable String id,
                                         @RequestHeader HttpHeaders headers,
                                         HttpServletRequest request) {
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

        HttpRange range = ranges.get(0);
        long start = range.getRangeStart(contentLength);
        long end = range.getRangeEnd(contentLength);
        long rangeLength = end - start + 1;

        ResourceRegion region = new ResourceRegion(resource, start, rangeLength);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(mediaType)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(region);
    }
}
