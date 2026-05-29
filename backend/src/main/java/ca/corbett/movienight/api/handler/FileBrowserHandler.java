package ca.corbett.movienight.api.handler;


import ca.corbett.movienight.api.util.ExceptionMapper;
import ca.corbett.movienight.api.util.QueryParamParser;
import ca.corbett.movienight.api.util.ResponseWriter;
import ca.corbett.movienight.config.AppConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Provides a chrooted view of the server's filesystem for browsing the media directory remotely.
 * Clients can use this to navigate the media directory tree and a media file.
 * The file browser is "locked" to our configured media directory. The client can't
 * navigate outside of it.
 * <p>Routes:</p>
 * <ul>
 *     <li>GET /api/files[?path=...]</li>
 * </ul>
 * <p>
 * If path is omitted, we'll list the contents of the media directory. If path is provided, we'll resolve it
 * relative to the media directory and list its contents. If the resolved path is not a directory
 * or doesn't exist, we'll fall back to listing the media directory. The response will include the
 * absolute path of the listed directory, and if it's not the media directory, the absolute path
 * of its parent directory. This allows the client to implement "up" navigation. The response will also
 * include a list of entries in the listed directory, where each entry has a name, type (file or directory),
 * and absolute path. The client can then attach the path of the selected media file to the
 * media item upsert request when creating or updating a media item.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class FileBrowserHandler implements HttpHandler {

    private static final Logger log = Logger.getLogger(FileBrowserHandler.class.getName());
    private static final Set<String> ALLOWED_VIDEO_EXTENSIONS = Set.of("mpg", "mkv", "mp4", "mov", "avi");

    private final AppConfig appConfig;
    private final ResponseWriter responseWriter;

    public FileBrowserHandler(AppConfig appConfig, ResponseWriter responseWriter) {
        this.appConfig = appConfig;
        this.responseWriter = responseWriter;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String requestPath = exchange.getRequestURI().getPath();
            String prefix = appConfig.getApiBasePath() + "files/";
            if (!requestPath.startsWith(prefix)) {
                throw new IllegalArgumentException("Invalid path: " + requestPath);
            }

            if ("GET".equals(method)) {
                Path targetPath = resolvePathParameter(exchange);
                List<File> files = listFiles(targetPath);
                responseWriter.writeJson(exchange, HttpURLConnection.HTTP_OK, buildResponse(targetPath, files));
            }
            else {
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(405, 0); // 405 Method Not Allowed
            }
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            var mapped = ExceptionMapper.map(e);
            responseWriter.writeJson(exchange, (int)mapped[0], mapped[1]);
        }
    }

    /**
     * Parses our path parameter if one was given, and resolves it to an actual filesystem path
     * within our media directory. If the given path is outside of our media directory or does
     * not exist, we will ignore it and return the media directory instead.
     */
    private Path resolvePathParameter(HttpExchange exchange) {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = QueryParamParser.parse(query);
        Path mediaDirPath = appConfig.getMediaDir();
        Path pathParam = Path.of(params.getOrDefault("path", mediaDirPath.toAbsolutePath().toString())).normalize();

        // If the given path is not within our media dir, ignore it and use mediaDir instead.
        if (!pathParam.startsWith(mediaDirPath.normalize())) {
            log.warning("Received file browser request with path outside media directory: " + pathParam);
            return mediaDirPath.normalize();
        }

        // If the given path does not actually exist, ignore it and use mediaDir instead.
        if (!Files.exists(pathParam)) {
            log.warning("Received file browser request with non-existent path: " + pathParam);
            return mediaDirPath.normalize();
        }

        // If the given path exists but is a file, that's okay, just use its parent directory instead.
        // The UI can use this to supply a media file, and we will silently do the right thing.
        if (Files.isRegularFile(pathParam)) {
            pathParam = pathParam.getParent();
        }

        return pathParam;
    }

    /**
     * Given a directory, returns a list of its immediate children, sorted with directories first and then files,
     * and with names sorted alphabetically within those groups. If the given path is not
     * a directory or doesn't exist, returns an empty list.
     */
    private List<File> listFiles(Path directory) {
        File dirFile = directory.toFile();
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            log.warning("Attempted to list files in non-existent or non-directory path: " + directory);
            return List.of();
        }
        File[] children = dirFile.listFiles();
        if (children == null) {
            log.warning("Failed to list files in directory (null returned): " + directory);
            return List.of();
        }
        children = Arrays.stream(children)
                .filter(file -> file.isDirectory() || hasAllowedVideoExtension(file))
                .toArray(File[]::new);
        Arrays.sort(children, Comparator
                .comparing((File f) -> !f.isDirectory())
                .thenComparing(f -> f.getName().toLowerCase()));
        return Arrays.asList(children);
    }

    private boolean hasAllowedVideoExtension(File file) {
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return false;
        }
        String extension = fileName.substring(dotIndex + 1).toLowerCase();
        return ALLOWED_VIDEO_EXTENSIONS.contains(extension);
    }

    /**
     * Builds and returns a Map representing the JSON response for a file listing.
     * The map will include the absolute path of the current directory, the absolute path
     * of the parent directory if it's not the media directory, and a list of entries
     * representing the immediate children of the current directory.
     * Each entry includes the name, type (file or directory), and absolute path of the child.
     * <p>
     *     Sample response:
     * </p>
     * <pre>
     * {
     *   "path": "/path/to/mediaDir/subdir",
     *   "parent": "/path/to/mediaDir", // omitted if current directory is media dir
     *   "entries": [
     *   {
     *     "name": "child1",
     *     "type": "directory",
     *     "path": "/path/to/mediaDir/subdir/child1"
     *   },
     *   {
     *     "name": "child2.mp4",
     *     "type": "file",
     *     "path": "/path/to/mediaDir/subdir/child2.mp4"
     *   }
     *   // ... more entries ...
     *   ]
     * }
     * </pre>
     */
    private Map<String, Object> buildResponse(Path currentPath, List<File> files) {
        Map<String, Object> response = new HashMap<>();
        response.put("path", currentPath.toAbsolutePath().toString());
        if (!currentPath.equals(appConfig.getMediaDir())) {
            response.put("parent", currentPath.getParent().toAbsolutePath().toString());
        }
        List<Map<String, String>> entries = new ArrayList<>();
        for (File file : files) {
            // Skip hidden files:
            if (file.getName().startsWith(".")) {
                continue;
            }
            // Skip symbolic links to avoid traversal outside the browsed tree
            if (Files.isSymbolicLink(file.toPath())) {
                continue;
            }
            Map<String, String> entry = new HashMap<>();
            entry.put("name", file.getName());
            entry.put("type", file.isDirectory() ? "directory" : "file");
            entry.put("path", file.getAbsolutePath());
            entries.add(entry);
        }
        response.put("entries", entries);
        return response;
    }
}