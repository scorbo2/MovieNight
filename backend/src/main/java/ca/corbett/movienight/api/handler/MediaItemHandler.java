package ca.corbett.movienight.api.handler;

import ca.corbett.movienight.api.dto.MediaItemListResponse;
import ca.corbett.movienight.api.dto.MediaItemResponse;
import ca.corbett.movienight.api.dto.MediaItemUpsertRequest;
import ca.corbett.movienight.api.util.QueryParamParser;
import ca.corbett.movienight.api.util.RequestParser;
import ca.corbett.movienight.api.util.ResponseWriter;
import ca.corbett.movienight.config.AppConfig;
import ca.corbett.movienight.db.Database;
import ca.corbett.movienight.service.MediaItemService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP handler for nested MediaItem collection endpoints.
 * <p>
 * Routes:
 * <ul>
 *   <li>POST   /api/media-groups/{groupId}/items</li>
 *   <li>GET    /api/media-groups/{groupId}/items</li>
 * </ul>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class MediaItemHandler implements HttpHandler {

    private final AppConfig appConfig;

    private final MediaItemService mediaItemService;
    private final ResponseWriter responseWriter;
    private final RequestParser requestParser;

    public MediaItemHandler(MediaItemService mediaItemService,
                            ResponseWriter responseWriter,
                            RequestParser requestParser,
                            AppConfig appConfig) {
        this.mediaItemService = mediaItemService;
        this.responseWriter = responseWriter;
        this.requestParser = requestParser;
        this.appConfig = appConfig;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("POST".equals(method)) {
                handleCreate(exchange, path);
            }
            else if ("GET".equals(method)) {
                handleGetOrList(exchange, path);
            }
            else if ("PUT".equals(method)) {
                handleUpdate(exchange, path);
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

    /**
     * GET /api/media-items/{itemId} or GET /api/media-groups/{groupId}/items.
     */
    private void handleGetOrList(HttpExchange exchange, String path) throws Exception {
        if (path.startsWith(appConfig.getApiBasePath() + "media-items/")) {
            handleGetItemById(exchange, path);
        }
        else {
            handleList(exchange, path);
        }
    }

    private void checkNestedItemsPath(String path) {
        if (!path.endsWith("/items")) {
            throw new RuntimeException("ROUTE_NOT_MATCHED");
        }
    }

    /**
     * POST /api/media-groups/{groupId}/items — Create a MediaItem within a group.
     */
    private void handleCreate(HttpExchange exchange, String path) throws Exception {
        checkNestedItemsPath(path);

        // Extract groupId from path: {apiBasePath}media-groups/{groupId}/items
        String groupIdPart = path.substring(
                appConfig.getApiBasePath().length() + "media-groups".length() + 1); // "{groupId}/items"
        String groupIdStr = groupIdPart.split("/")[0];

        long groupId;
        try {
            groupId = Long.parseLong(groupIdStr);
            if (groupId <= 0) {
                throw new IllegalArgumentException("groupId must be greater than 0");
            }
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("groupId must be a valid number");
        }

        MediaItemUpsertRequest request = requestParser.readBody(exchange, MediaItemUpsertRequest.class);

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("item.title cannot be blank");
        }
        if (request.getMediaFilePath() == null || request.getMediaFilePath().isBlank()) {
            throw new IllegalArgumentException("item.mediaFilePath cannot be blank");
        }

        MediaItemResponse response = mediaItemService.createItem(groupId, request);

        exchange.getResponseHeaders().set("Location", appConfig.getApiBasePath() + "media-items/" + response.getId());
        responseWriter.writeJson(exchange, HttpURLConnection.HTTP_CREATED, response);
    }

    /**
     * GET /api/media-groups/{groupId}/items — List/search items within a group.
     */
    private void handleList(HttpExchange exchange, String path) throws Exception {
        checkNestedItemsPath(path);

        // Extract groupId from path: {apiBasePath}media-groups/{groupId}/items
        String groupIdPart = path.substring(
                appConfig.getApiBasePath().length() + "media-groups".length() + 1); // "{groupId}/items"
        String groupIdStr = groupIdPart.split("/")[0];

        long groupId;
        try {
            groupId = Long.parseLong(groupIdStr);
            if (groupId <= 0) {
                throw new IllegalArgumentException("groupId must be greater than 0");
            }
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("groupId must be a valid number");
        }

        Map<String, String> params = QueryParamParser.parse(exchange.getRequestURI().getQuery());

        // Pagination: if one param is present, both must be
        Optional<Integer> pageOpt = QueryParamParser.parseIntOptional(params, "pageNumber");
        Optional<Integer> sizeOpt = QueryParamParser.parseIntOptional(params, "pageSize");

        int pageNumber;
        int pageSize;

        if (pageOpt.isPresent() && sizeOpt.isPresent()) {
            pageNumber = pageOpt.get();
            pageSize = sizeOpt.get();
            if (pageNumber <= 0) {
                throw new IllegalArgumentException("pageNumber must be greater than 0");
            }
            if (pageSize <= 0) {
                throw new IllegalArgumentException("pageSize must be greater than 0");
            }
        }
        else if (pageOpt.isPresent() != sizeOpt.isPresent()) {
            throw new IllegalArgumentException("pageNumber and pageSize must both be provided or both omitted");
        }
        else {
            pageNumber = 1;
            pageSize = appConfig.getDefaultPageSize();
        }

        String titleContains = QueryParamParser.get(params, "titleContains").orElse(null);
        String descriptionContains = QueryParamParser.get(params, "descriptionContains").orElse(null);
        String mediaFilePathContains = QueryParamParser.get(params, "mediaFilePathContains").orElse(null);
        String tagContains = QueryParamParser.get(params, "tagContains").orElse(null);

        MediaItemListResponse response = mediaItemService.listItems(
                groupId,
                titleContains,
                descriptionContains,
                mediaFilePathContains,
                tagContains,
                pageNumber,
                pageSize
        );

        responseWriter.writeJson(exchange, HttpURLConnection.HTTP_OK, response);
    }

    /**
     * GET /api/media-items/{itemId} — Get a single MediaItem by ID.
     */
    private void handleGetItemById(HttpExchange exchange, String path) throws Exception {
        String itemIdStr = path.substring(appConfig.getApiBasePath().length() + "media-items".length() + 1);
        long itemId;
        try {
            itemId = Long.parseLong(itemIdStr);
            if (itemId <= 0) {
                throw new IllegalArgumentException("itemId must be greater than 0");
            }
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("itemId must be a valid number");
        }

        MediaItemResponse response = mediaItemService.getItemById(itemId);
        responseWriter.writeJson(exchange, HttpURLConnection.HTTP_OK, response);
    }

    /**
     * PUT /api/media-items/{itemId} — Update a MediaItem.
     */
    private void handleUpdate(HttpExchange exchange, String path) throws Exception {
        String itemIdStr = path.substring(appConfig.getApiBasePath().length() + "media-items".length() + 1);
        long itemId;
        try {
            itemId = Long.parseLong(itemIdStr);
            if (itemId <= 0) {
                throw new IllegalArgumentException("itemId must be greater than 0");
            }
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("itemId must be a valid number");
        }

        MediaItemUpsertRequest request = requestParser.readBody(exchange, MediaItemUpsertRequest.class);

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("item.title cannot be blank");
        }
        if (request.getMediaFilePath() == null || request.getMediaFilePath().isBlank()) {
            throw new IllegalArgumentException("item.mediaFilePath cannot be blank");
        }
        if (request.getMediaGroupId() <= 0) {
            throw new IllegalArgumentException("item.mediaGroupId must be greater than 0");
        }

        MediaItemResponse response = mediaItemService.updateItem(itemId, request);
        responseWriter.writeJson(exchange, HttpURLConnection.HTTP_OK, response);
    }

    /**
     * DELETE /api/media-items/{itemId} — Delete a MediaItem.
     */
    private void handleDelete(HttpExchange exchange, String path) throws Exception {
        String itemIdStr = path.substring(appConfig.getApiBasePath().length() + "media-items".length() + 1);
        long itemId;
        try {
            itemId = Long.parseLong(itemIdStr);
            if (itemId <= 0) {
                throw new IllegalArgumentException("itemId must be greater than 0");
            }
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("itemId must be a valid number");
        }

        boolean deleted = mediaItemService.deleteItem(itemId);
        if (!deleted) {
            throw new Database.NotFoundException("No MediaItem found with id=" + itemId);
        }

        responseWriter.writeNoContent(exchange);
    }
}
