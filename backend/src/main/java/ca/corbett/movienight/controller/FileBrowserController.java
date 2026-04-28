package ca.corbett.movienight.controller;

import ca.corbett.movienight.service.MediaService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "http://localhost:5173")
public class FileBrowserController {

    private static final Logger log = LoggerFactory.getLogger(FileBrowserController.class);

    @Value("${movienight.prefix.movies:/}")
    private String movieDirectory;
    private File resolvedMovieDirectory;

    @Value("${movienight.prefix.episodes:/}")
    private String episodeDirectory;
    private File resolvedEpisodeDirectory;

    @Value("${movienight.prefix.music:/}")
    private String musicVideoDirectory;
    private File resolvedMusicVideoDirectory;

    /**
     * We'll check to ensure we got valid values for our media directories,
     * and resolve them to actual filesystem paths for use later.
     * This allows us to fail fast if our configuration is invalid,
     * and also allows us to avoid having to do this on every findById request.
     */
    @PostConstruct
    public void resolveMediaDirectories() {
        resolvedMovieDirectory = MediaService.resolveMediaDirectory(movieDirectory, "movienight.prefix.movies");
        resolvedEpisodeDirectory = MediaService.resolveMediaDirectory(episodeDirectory, "movienight.prefix.episodes");
        resolvedMusicVideoDirectory = MediaService.resolveMediaDirectory(musicVideoDirectory,
                                                                         "movienight.prefix.music");
    }

    /**
     * Provides a chrooted view of the given mediaDir, starting at the given optional path.
     * The path is relative to the given mediaDir. We will not provide options for navigating
     * outside of mediaDir.
     */
    private ResponseEntity<Map<String, Object>> listFilesInternal(File mediaDir, String path) {
        if (path.startsWith(mediaDir.getAbsolutePath())) {
            path = path.substring(mediaDir.getAbsolutePath().length());
        }
        log.info("Listing files in media directory: {}, path: {}", mediaDir.getAbsolutePath(), path);
        File currentDir = new File(mediaDir, path);

        // The UI might give us a file. Not a problem: just use its parent directory.
        if (currentDir.isFile()) {
            currentDir = mediaDir.getParentFile() == null ? mediaDir : currentDir.getParentFile();
        }

        if (!currentDir.exists() || !currentDir.isDirectory()) {
            // Fall back to our chroot
            currentDir = mediaDir;
        }

        boolean isRoot = currentDir.getAbsolutePath().equals(mediaDir.getAbsolutePath());
        String canonicalPath = currentDir.getAbsolutePath();
        File parentDir = isRoot ? null : currentDir.getParentFile();
        String parentPath = (parentDir != null) ? parentDir.getAbsolutePath() : null;

        List<Map<String, String>> entries = new ArrayList<>();
        File[] children = currentDir.listFiles();
        if (children != null) {
            Arrays.sort(children, Comparator
                    .comparing((File f) -> !f.isDirectory())
                    .thenComparing(f -> f.getName().toLowerCase()));
            for (File child : children) {
                if (child.getName().startsWith(".")) {
                    continue;
                }
                // Skip symbolic links to avoid traversal outside the browsed tree
                if (Files.isSymbolicLink(child.toPath())) {
                    continue;
                }
                Map<String, String> entry = new HashMap<>();
                entry.put("name", child.getName());
                entry.put("type", child.isDirectory() ? "directory" : "file");
                entry.put("path", child.getAbsolutePath());
                entries.add(entry);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("path", canonicalPath);
        if (!isRoot) {
            result.put("parent", parentPath);
        }
        result.put("entries", entries);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/movies")
    public ResponseEntity<Map<String, Object>> listMovieFiles(
            @RequestParam(defaultValue = "/") String path) {
        return listFilesInternal(resolvedMovieDirectory, path);
    }

    @GetMapping("/episodes")
    public ResponseEntity<Map<String, Object>> listEpisodeFiles(
            @RequestParam(defaultValue = "/") String path) {
        return listFilesInternal(resolvedEpisodeDirectory, path);
    }

    @GetMapping("/music")
    public ResponseEntity<Map<String, Object>> listMusicVideoFiles(
            @RequestParam(defaultValue = "/") String path) {
        return listFilesInternal(resolvedMusicVideoDirectory, path);
    }
}
