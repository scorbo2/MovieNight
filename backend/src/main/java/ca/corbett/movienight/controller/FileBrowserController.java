package ca.corbett.movienight.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
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

    @GetMapping
    public ResponseEntity<Map<String, Object>> listFiles(
            @RequestParam(defaultValue = "/") String path) {

        File dir;
        try {
            dir = new File(path).getCanonicalFile();
        } catch (Exception e) {
            dir = new File(File.separator);
        }

        if (!dir.exists() || !dir.isDirectory()) {
            // Fall back to filesystem root
            dir = new File(File.separator);
        }

        String canonicalPath = dir.getAbsolutePath();
        File parentDir = dir.getParentFile();
        String parentPath = (parentDir != null) ? parentDir.getAbsolutePath() : canonicalPath;

        List<Map<String, String>> entries = new ArrayList<>();
        File[] children = dir.listFiles();
        if (children != null) {
            Arrays.sort(children, Comparator
                    .comparing((File f) -> !f.isDirectory())
                    .thenComparing(f -> f.getName().toLowerCase()));
            for (File child : children) {
                if (child.getName().startsWith(".")) {
                    continue;
                }
                // Skip symbolic links to avoid traversal outside the browsed tree
                if (java.nio.file.Files.isSymbolicLink(child.toPath())) {
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
        result.put("parent", parentPath);
        result.put("entries", entries);
        return ResponseEntity.ok(result);
    }
}
