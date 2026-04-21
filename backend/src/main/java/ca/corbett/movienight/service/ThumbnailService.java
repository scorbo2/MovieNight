package ca.corbett.movienight.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

@Service
public class ThumbnailService {

    private static final int MAX_DIMENSION = 2000;
    private static final int MIN_DIMENSION = 26;
    private static final List<String> EXTENSIONS = List.of("jpg", "jpeg", "png");

    private final String dataDir;
    private boolean enabled = false;

    public ThumbnailService(@Value("${movienight.data-dir:}") String dataDir) {
        this.dataDir = dataDir;
    }

    @PostConstruct
    public void init() {
        if (dataDir == null || dataDir.isBlank()) {
            return;
        }
        Path dir = Paths.get(dataDir);
        if (!Files.exists(dir)) {
            return;
        }
        try {
            Files.createDirectories(dir.resolve("movies"));
            Files.createDirectories(dir.resolve("episodes"));
            enabled = true;
        } catch (IOException e) {
            // silently disable thumbnail support
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Validate and save a thumbnail for the given subdir (movies/episodes) and entity id.
     * Replaces any existing thumbnail for the same id.
     */
    public void saveThumbnail(MultipartFile file, String subDir, Long id) throws IOException {
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Thumbnail support is not available");
        }

        byte[] bytes = file.getBytes();

        // Use ImageReader to read dimensions from metadata first (avoids full decode)
        // This guards against decompression bombs before committing memory to a full decode.
        ImageInputStream iis;
        try {
            iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image file");
        }
        if (iis == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image file");
        }
        Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
        if (!readers.hasNext()) {
            try { iis.close(); } catch (IOException ignored) {}
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image file");
        }
        ImageReader reader = readers.next();
        try {
            reader.setInput(iis, true, true);
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Image too large (max " + MAX_DIMENSION + "x" + MAX_DIMENSION + ")");
            }
            if (width < MIN_DIMENSION || height < MIN_DIMENSION) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Image too small (min " + MIN_DIMENSION + "x" + MIN_DIMENSION + ")");
            }
        } finally {
            reader.dispose();
            try { iis.close(); } catch (IOException ignored) {}
        }

        // Dimensions are within bounds — now fully decode to validate image integrity
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image file");
        }
        if (image == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image file");
        }

        // Delete any existing thumbnail before saving the new one
        deleteThumbnail(subDir, id);

        String ext = resolveExtension(file.getContentType());
        Path dest = Paths.get(dataDir, subDir, id + "." + ext);
        Files.write(dest, bytes);
    }

    /**
     * Delete the thumbnail for the given subdir and entity id, if it exists.
     */
    public void deleteThumbnail(String subDir, Long id) {
        if (!enabled) {
            return;
        }
        for (String ext : EXTENSIONS) {
            Path p = Paths.get(dataDir, subDir, id + "." + ext);
            try {
                Files.deleteIfExists(p);
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Returns true if a thumbnail exists for the given subdir and entity id.
     */
    public boolean hasThumbnail(String subDir, Long id) {
        return getThumbnailPath(subDir, id) != null;
    }

    /**
     * Returns the Path to the thumbnail file, or null if it does not exist or
     * thumbnail support is disabled.
     */
    public Path getThumbnailPath(String subDir, Long id) {
        if (!enabled) {
            return null;
        }
        for (String ext : EXTENSIONS) {
            Path p = Paths.get(dataDir, subDir, id + "." + ext);
            if (Files.exists(p)) {
                return p;
            }
        }
        return null;
    }

    private String resolveExtension(String contentType) {
        if ("image/png".equalsIgnoreCase(contentType)) {
            return "png";
        }
        return "jpg";
    }
}
