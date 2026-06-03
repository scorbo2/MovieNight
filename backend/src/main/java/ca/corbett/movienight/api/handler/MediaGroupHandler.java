package ca.corbett.movienight.api.handler;

import ca.corbett.movienight.api.dto.MediaGroupListResponse;
import ca.corbett.movienight.api.dto.MediaGroupResponse;
import ca.corbett.movienight.api.dto.MediaGroupUpsertRequest;
import ca.corbett.movienight.api.util.PathParamParser;
import ca.corbett.movienight.api.util.QueryParamParser;
import ca.corbett.movienight.api.util.RequestParser;
import ca.corbett.movienight.api.util.ResponseWriter;
import ca.corbett.movienight.config.AppConfig;
import ca.corbett.movienight.db.Database;
import ca.corbett.movienight.service.MediaGroupService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP handler for all MediaGroup endpoints.
 * <p>
 * Routes:
 * <ul>
 *   <li>POST   /api/media-groups</li>
 *   <li>GET    /api/media-groups</li>
 *   <li>GET    /api/media-groups/{groupId}</li>
 *   <li>PUT    /api/media-groups/{groupId}</li>
 *   <li>DELETE /api/media-groups/{groupId}</li>
 * </ul>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class MediaGroupHandler implements HttpHandler {

    private final MediaGroupService mediaGroupService;
    private final ResponseWriter responseWriter;
    private final RequestParser requestParser;
    private final AppConfig appConfig;

    public MediaGroupHandler(MediaGroupService mediaGroupService,
                             ResponseWriter responseWriter,
                             RequestParser requestParser,
                             AppConfig appConfig) {
        this.mediaGroupService = mediaGroupService;
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
                handleCreate(exchange);
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
     * POST /api/media-groups — Create a new MediaGroup.
     */
    private void handleCreate(HttpExchange exchange) throws Exception {
        MediaGroupUpsertRequest request = requestParser.readBody(exchange, MediaGroupUpsertRequest.class);

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("group.title cannot be blank");
        }

        MediaGroupResponse response = mediaGroupService.createGroup(request);

        exchange.getResponseHeaders().set("Location", appConfig.getApiBasePath() + "media-groups/" + response.getId());
        responseWriter.writeJson(exchange, HttpURLConnection.HTTP_CREATED, response);
    }

    /**
     * GET /api/media-groups or GET /api/media-groups/{groupId}.
     * <p>
     * If the path is exactly /api/media-groups, returns a paginated list.
     * If the path is /api/media-groups/{id}, returns a single group.
     */
    private void handleGetOrList(HttpExchange exchange, String path) throws Exception {
        String basePath = appConfig.getApiBasePath() + "media-groups";

        if (path.equals(basePath)) {
            handleList(exchange);
        }
        else if (path.startsWith(basePath + "/")) {
            handleGetById(exchange, path);
        }
        else {
            throw new IllegalArgumentException("Invalid path: " + path);
        }
    }

    /**
     * GET /api/media-groups — List/search MediaGroups with pagination.
     */
    private void handleList(HttpExchange exchange) throws Exception {
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
            pageSize = appConfig.getPageSize();
        }

        // Optional filters
        Long parentGroupId = QueryParamParser.parseLongOptional(params, "parentGroupId")
                                             .map(v -> {
                                                 if (v <= 0) {
                                                     throw new IllegalArgumentException(
                                                             "parentGroupId must be greater than 0");
                                                 }
                                                 return v;
                                             }).orElse(null);

        boolean topLevelOnly = QueryParamParser.parseBooleanOptional(params, "topLevelOnly").orElse(false);

        // topLevelOnly cannot be combined with parentGroupId
        if (topLevelOnly && parentGroupId != null) {
            throw new IllegalArgumentException("topLevelOnly cannot be combined with parentGroupId");
        }

        String titleContains = QueryParamParser.get(params, "titleContains").orElse(null);
        String descriptionContains = QueryParamParser.get(params, "descriptionContains").orElse(null);

        MediaGroupListResponse response = mediaGroupService.listGroups(
                parentGroupId,
                topLevelOnly,
                titleContains,
                descriptionContains,
                pageNumber,
                pageSize
        );

        responseWriter.writeJson(exchange, HttpURLConnection.HTTP_OK, response);
    }

    /**
     * GET /api/media-groups/{groupId} — Get a single MediaGroup by ID.
     */
    private void handleGetById(HttpExchange exchange, String path) throws Exception {
        long groupId = PathParamParser.extractAndValidateId(path);
        MediaGroupResponse response = mediaGroupService.getGroupById(groupId);
        responseWriter.writeJson(exchange, HttpURLConnection.HTTP_OK, response);
    }

    /**
     * PUT /api/media-groups/{groupId} — Update a MediaGroup.
     */
    private void handleUpdate(HttpExchange exchange, String path) throws Exception {
        long groupId = PathParamParser.extractAndValidateId(path);
        MediaGroupUpsertRequest request = requestParser.readBody(exchange, MediaGroupUpsertRequest.class);

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("group.title cannot be blank");
        }

        MediaGroupResponse response = mediaGroupService.updateGroup(groupId, request);
        responseWriter.writeJson(exchange, HttpURLConnection.HTTP_OK, response);
    }

    /**
     * DELETE /api/media-groups/{groupId} — Delete a MediaGroup.
     */
    private void handleDelete(HttpExchange exchange, String path) throws Exception {
        long groupId = PathParamParser.extractAndValidateId(path);

        boolean deleted = mediaGroupService.deleteGroup(groupId);
        if (!deleted) {
            throw new Database.NotFoundException("No MediaGroup found with id=" + groupId);
        }

        responseWriter.writeNoContent(exchange);
    }
}
