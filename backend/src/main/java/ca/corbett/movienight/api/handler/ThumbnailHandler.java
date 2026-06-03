package ca.corbett.movienight.api.handler;

import ca.corbett.movienight.api.dto.ThumbnailRequest;
import ca.corbett.movienight.api.util.ResponseWriter;
import ca.corbett.movienight.api.util.ThumbnailUtil;
import ca.corbett.movienight.config.AppConfig;
import ca.corbett.movienight.db.Database;
import ca.corbett.movienight.model.MediaGroup;
import ca.corbett.movienight.model.MediaItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * HTTP handler for thumbnail management endpoints.
 * <p>Routes:</p>
 * <ul>
 *   <li>GET    /api/thumbnails/media-items/{itemId}</li>
 *   <li>POST   /api/thumbnails/media-items/{itemId}</li>
 *   <li>PUT    /api/thumbnails/media-items/{itemId}</li>
 *   <li>DELETE /api/thumbnails/media-items/{itemId}</li>
 *   <li>GET    /api/thumbnails/media-groups/{groupId}</li>
 *   <li>POST   /api/thumbnails/media-groups/{groupId}</li>
 *   <li>PUT    /api/thumbnails/media-groups/{groupId}</li>
 *   <li>DELETE /api/thumbnails/media-groups/{groupId}</li>
 * </ul>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class ThumbnailHandler implements HttpHandler {

    private static final Logger log = Logger.getLogger(ThumbnailHandler.class.getName());

    private final AppConfig appConfig;
    private final Database database;
    private final ResponseWriter responseWriter;
    private final ObjectMapper objectMapper;

    public ThumbnailHandler(AppConfig appConfig,
                            Database database,
                            ResponseWriter responseWriter,
                            ObjectMapper objectMapper) {
        this.appConfig = appConfig;
        this.database = database;
        this.responseWriter = responseWriter;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equals(method)) {
                handleGet(exchange, path);
            }
            else if ("POST".equals(method)) {
                handleCreate(exchange, path);
            }
            else if ("PUT".equals(method)) {
                handleReplace(exchange, path);
            }
            else if ("DELETE".equals(method)) {
                handleDelete(exchange, path);
            }
            else {
                // Handled by Router (405)
                throw new UnsupportedOperationException("Method " + method + " not supported");
            }
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    // ====================================================================
    // GET — retrieve thumbnail image
    // ====================================================================

    private void handleGet(HttpExchange exchange, String path) throws Exception {
        String basePath = appConfig.getApiBasePath() + "thumbnails";
        log.fine("Handling GET thumbnail request for path: " + path);

        if (path.startsWith(basePath + "/media-items/")) {
            handleGetMediaItemThumbnail(exchange, path);
        }
        else if (path.startsWith(basePath + "/media-groups/")) {
            handleGetMediaGroupThumbnail(exchange, path);
        }
        else {
            throw new IllegalArgumentException("Unknown thumbnail resource type in path: " + path);
        }
    }

    private void handleGetMediaItemThumbnail(HttpExchange exchange, String path) throws Exception {
        long itemId = extractResourceId(path);
        MediaItem item = database.getMediaItemById(itemId);
        if (item == null) {
            throw new Database.NotFoundException("No MediaItem found with id=" + itemId);
        }

        BufferedImage image = ThumbnailUtil.getThumbnail(item, appConfig);
        if (image == null) {
            throw new Database.NotFoundException("No thumbnail exists for MediaItem id=" + itemId);
        }

        writeImageResponse(exchange, image);
    }

    private void handleGetMediaGroupThumbnail(HttpExchange exchange, String path) throws Exception {
        long groupId = extractResourceId(path);
        MediaGroup group = database.getMediaGroupById(groupId);
        if (group == null) {
            throw new Database.NotFoundException("No MediaGroup found with id=" + groupId);
        }

        BufferedImage image = ThumbnailUtil.getThumbnail(group, appConfig);
        if (image == null) {
            throw new Database.NotFoundException("No thumbnail exists for MediaGroup id=" + groupId);
        }

        writeImageResponse(exchange, image);
    }

    // ====================================================================
    // POST — create thumbnail (returns 201)
    // ====================================================================

    private void handleCreate(HttpExchange exchange, String path) throws Exception {
        String basePath = appConfig.getApiBasePath() + "thumbnails";
        log.info("Handling POST thumbnail request for path: " + path);

        if (path.startsWith(basePath + "/media-items/")) {
            handleCreateMediaItemThumbnail(exchange, path);
        }
        else if (path.startsWith(basePath + "/media-groups/")) {
            handleCreateMediaGroupThumbnail(exchange, path);
        }
        else {
            throw new IllegalArgumentException("Unknown thumbnail resource type in path: " + path);
        }
    }

    private void handleCreateMediaItemThumbnail(HttpExchange exchange, String path) throws Exception {
        long itemId = extractResourceId(path);
        MediaItem item = database.getMediaItemById(itemId);
        if (item == null) {
            throw new Database.NotFoundException("No MediaItem found with id=" + itemId);
        }

        BufferedImage image = readImageBody(exchange);
        ThumbnailUtil.storeThumbnail(item, image, appConfig);

        writeJsonResponse(exchange, HttpURLConnection.HTTP_CREATED,
                          Map.of("success", true, "message", "Thumbnail created", "id", itemId));
    }

    private void handleCreateMediaGroupThumbnail(HttpExchange exchange, String path) throws Exception {
        long groupId = extractResourceId(path);
        MediaGroup group = database.getMediaGroupById(groupId);
        if (group == null) {
            throw new Database.NotFoundException("No MediaGroup found with id=" + groupId);
        }

        BufferedImage image = readImageBody(exchange);
        ThumbnailUtil.storeThumbnail(group, image, appConfig);

        writeJsonResponse(exchange, HttpURLConnection.HTTP_CREATED,
                          Map.of("success", true, "message", "Thumbnail created", "id", groupId));
    }

    // ====================================================================
    // PUT — replace thumbnail (returns 200)
    // ====================================================================

    private void handleReplace(HttpExchange exchange, String path) throws Exception {
        String basePath = appConfig.getApiBasePath() + "thumbnails";
        log.info("Handling PUT thumbnail request for path: " + path);

        if (path.startsWith(basePath + "/media-items/")) {
            handleReplaceMediaItemThumbnail(exchange, path);
        }
        else if (path.startsWith(basePath + "/media-groups/")) {
            handleReplaceMediaGroupThumbnail(exchange, path);
        }
        else {
            throw new IllegalArgumentException("Unknown thumbnail resource type in path: " + path);
        }
    }

    private void handleReplaceMediaItemThumbnail(HttpExchange exchange, String path) throws Exception {
        long itemId = extractResourceId(path);
        MediaItem item = database.getMediaItemById(itemId);
        if (item == null) {
            throw new Database.NotFoundException("No MediaItem found with id=" + itemId);
        }

        BufferedImage image = readImageBody(exchange);
        ThumbnailUtil.storeThumbnail(item, image, appConfig);

        writeJsonResponse(exchange, HttpURLConnection.HTTP_OK,
                          Map.of("success", true, "message", "Thumbnail replaced", "id", itemId));
    }

    private void handleReplaceMediaGroupThumbnail(HttpExchange exchange, String path) throws Exception {
        long groupId = extractResourceId(path);
        MediaGroup group = database.getMediaGroupById(groupId);
        if (group == null) {
            throw new Database.NotFoundException("No MediaGroup found with id=" + groupId);
        }

        BufferedImage image = readImageBody(exchange);
        ThumbnailUtil.storeThumbnail(group, image, appConfig);

        writeJsonResponse(exchange, HttpURLConnection.HTTP_OK,
                          Map.of("success", true, "message", "Thumbnail replaced", "id", groupId));
    }

    // ====================================================================
    // DELETE — remove thumbnail (returns 204)
    // ====================================================================

    private void handleDelete(HttpExchange exchange, String path) throws Exception {
        String basePath = appConfig.getApiBasePath() + "thumbnails";
        log.info("Handling DELETE thumbnail request for path: " + path);

        if (path.startsWith(basePath + "/media-items/")) {
            handleDeleteMediaItemThumbnail(exchange, path);
        }
        else if (path.startsWith(basePath + "/media-groups/")) {
            handleDeleteMediaGroupThumbnail(exchange, path);
        }
        else {
            throw new IllegalArgumentException("Unknown thumbnail resource type in path: " + path);
        }
    }

    private void handleDeleteMediaItemThumbnail(HttpExchange exchange, String path) throws Exception {
        long itemId = extractResourceId(path);
        MediaItem item = database.getMediaItemById(itemId);
        if (item == null) {
            throw new Database.NotFoundException("No MediaItem found with id=" + itemId);
        }

        ThumbnailUtil.removeThumbnail(item, appConfig);
        responseWriter.writeNoContent(exchange);
    }

    private void handleDeleteMediaGroupThumbnail(HttpExchange exchange, String path) throws Exception {
        long groupId = extractResourceId(path);
        MediaGroup group = database.getMediaGroupById(groupId);
        if (group == null) {
            throw new Database.NotFoundException("No MediaGroup found with id=" + groupId);
        }

        ThumbnailUtil.removeThumbnail(group, appConfig);
        responseWriter.writeNoContent(exchange);
    }

    // ====================================================================
    // Helpers
    // ====================================================================

    /**
     * Extracts the numeric resource ID from a thumbnail path.
     * e.g., "/api/thumbnails/media-items/123" → 123
     */
    private long extractResourceId(String path) {
        String idStr = path.substring(path.lastIndexOf('/') + 1);
        try {
            long id = Long.parseLong(idStr);
            if (id <= 0) {
                throw new IllegalArgumentException("Resource ID must be greater than 0");
            }
            return id;
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Resource ID must be a valid number");
        }
    }

    /**
     * Reads the request body and decodes the image from either
     * multipart/form-data or JSON (base64).
     */
    private BufferedImage readImageBody(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null) {
            throw new IllegalArgumentException("Content-Type header is required");
        }

        if (contentType.startsWith("multipart/form-data")) {
            return parseMultipartImage(exchange);
        }
        else if (contentType.startsWith("application/json")) {
            return parseJsonImage(exchange);
        }
        else {
            throw new IllegalArgumentException("Unsupported Content-Type: " + contentType
                                                       + ". Expected multipart/form-data or application/json");
        }
    }

    /**
     * Parses a multipart/form-data request body and extracts the first file part as a BufferedImage.
     */
    private BufferedImage parseMultipartImage(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        String boundary = extractBoundary(contentType);
        if (boundary == null) {
            throw new IllegalArgumentException("multipart/form-data must include a boundary parameter");
        }

        byte[] body = exchange.getRequestBody().readAllBytes();
        Map<String, byte[]> parts = parseMultipartBody(body, boundary);

        // Look for the first file part
        for (Map.Entry<String, byte[]> entry : parts.entrySet()) {
            String fieldName = entry.getKey();
            byte[] fileContent = entry.getValue();

            // Try to decode as image
            try (ByteArrayInputStream bais = new ByteArrayInputStream(fileContent)) {
                BufferedImage image = ImageIO.read(bais);
                if (image != null) {
                    return image;
                }
            }
        }

        throw new IllegalArgumentException("No valid image found in multipart body");
    }

    /**
     * Parses a JSON request body containing a base64-encoded image.
     */
    private BufferedImage parseJsonImage(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            ThumbnailRequest request = objectMapper.readValue(is, ThumbnailRequest.class);

            if (request.getThumbnailBase64() == null || request.getThumbnailBase64().isBlank()) {
                throw new IllegalArgumentException("thumbnailBase64 field is required");
            }

            byte[] decodedBytes = Base64.getDecoder().decode(request.getThumbnailBase64());
            try (ByteArrayInputStream bais = new ByteArrayInputStream(decodedBytes)) {
                BufferedImage image = ImageIO.read(bais);
                if (image == null) {
                    throw new IllegalArgumentException("Could not decode base64 image");
                }
                return image;
            }
        }
        catch (JsonProcessingException jpe) {
            throw new IOException("Malformed JSON: " + jpe.getOriginalMessage(), jpe);
        }
    }

    /**
     * Extracts the boundary parameter from a multipart Content-Type header.
     */
    private String extractBoundary(String contentType) {
        if (contentType == null || !contentType.contains("boundary=")) {
            return null;
        }
        String[] parts = contentType.split("boundary=");
        if (parts.length < 2) {
            return null;
        }
        String boundary = parts[1].trim();
        // Remove surrounding quotes if present
        if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
            boundary = boundary.substring(1, boundary.length() - 1);
        }
        return boundary;
    }

    /**
     * Parses a multipart body into field name → content mapping.
     * Only extracts file parts (those with a Content-Disposition containing "filename").
     */
    private Map<String, byte[]> parseMultipartBody(byte[] body, String boundary) throws IOException {
        String delimiter = "--" + boundary;
        String bodyStr = new String(body, StandardCharsets.ISO_8859_1);

        // Split by boundary
        String[] rawParts = bodyStr.split(delimiter);

        return java.util.Arrays.stream(rawParts)
                               .filter(s -> !s.trim().startsWith("--") && !s.trim().isEmpty())
                               .map(s -> s.trim())
                               .map(part -> parseMultipartPart(part))
                               .filter(entry -> entry != null)
                               .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue()));
    }

    /**
     * Parses a single multipart part, extracting the field name and file content.
     * Returns null if the part is not a file part.
     */
    private Map.Entry<String, byte[]> parseMultipartPart(String part) {
        int endOffset = 4;
        int headerEndIndex = part.indexOf("\r\n\r\n");
        if (headerEndIndex == -1) {
            headerEndIndex = part.indexOf("\n\n");
            if (headerEndIndex == -1) {
                return null;
            }
            endOffset = 2;
        }

        String headers = part.substring(0, headerEndIndex);
        String content = part.substring(headerEndIndex + endOffset);

        // Check if this is a file part (has Content-Disposition with filename)
        if (!headers.contains("Content-Disposition") || !headers.contains("filename")) {
            return null;
        }

        // Extract field name
        String fieldName = extractFieldName(headers);
        if (fieldName == null) {
            return null;
        }

        // Remove trailing CRLF from content
        if (content.endsWith("\r\n")) {
            content = content.substring(0, content.length() - 2);
        }

        return Map.entry(fieldName, content.getBytes(StandardCharsets.ISO_8859_1));
    }

    /**
     * Extracts the field name from a Content-Disposition header.
     */
    private String extractFieldName(String headers) {
        for (String line : headers.split("\r\n|\n")) {
            if (line.toLowerCase().contains("name=")) {
                String[] parts = line.split("name=");
                if (parts.length >= 2) {
                    String name = parts[1].trim();
                    // Remove surrounding quotes if present
                    if (name.startsWith("\"") && name.endsWith("\"")) {
                        name = name.substring(1, name.length() - 1);
                    }
                    return name;
                }
            }
        }
        return null;
    }

    /**
     * Writes an image response with an appropriate Content-Type (either image/jpeg or image/png).
     */
    private void writeImageResponse(HttpExchange exchange, BufferedImage image) throws IOException {
        byte[] bytes = toImageBytes(image);

        String mimeType = image.getColorModel().hasAlpha() ? "image/png" : "image/jpeg";

        exchange.getResponseHeaders().set("Content-Type", mimeType);
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, bytes.length);

        try (java.io.OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Converts a BufferedImage to bytes in the appropriate format.
     * Most of the time, this will return a jpg image.
     * If the given BufferedImage has an alpha channel, we will write it as PNG instead.
     */
    private byte[] toImageBytes(BufferedImage image) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        String formatName = image.getColorModel().hasAlpha() ? "png" : "jpg";
        if (!ImageIO.write(image, formatName, baos)) {
            throw new IOException("Failed to write image in format: " + formatName);
        }
        return baos.toByteArray();
    }

    /**
     * Writes a JSON response body with the given status code.
     */
    private void writeJsonResponse(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] bytes = objectMapper.writeValueAsBytes(body);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (java.io.OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
