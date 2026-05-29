package ca.corbett.movienight.api;

import ca.corbett.movienight.api.dto.ErrorResponse;
import ca.corbett.movienight.api.handler.FileBrowserHandler;
import ca.corbett.movienight.api.handler.HealthHandler;
import ca.corbett.movienight.api.handler.MediaGroupHandler;
import ca.corbett.movienight.api.handler.MediaItemHandler;
import ca.corbett.movienight.api.handler.PlaylistHandler;
import ca.corbett.movienight.api.handler.StaticFrontendHandler;
import ca.corbett.movienight.api.handler.StreamHandler;
import ca.corbett.movienight.api.handler.ThumbnailHandler;
import ca.corbett.movienight.api.routing.Route;
import ca.corbett.movienight.api.routing.Router;
import ca.corbett.movienight.api.util.ExceptionMapper;
import ca.corbett.movienight.api.util.JsonSupport;
import ca.corbett.movienight.api.util.RequestParser;
import ca.corbett.movienight.api.util.ResponseWriter;
import ca.corbett.movienight.config.AppConfig;
import ca.corbett.movienight.db.Database;
import ca.corbett.movienight.service.MediaGroupService;
import ca.corbett.movienight.service.MediaItemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bootstraps the HTTP server.
 * <p>
 * Creates and configures the underlying {@link HttpServer}, wires up the
 * routing infrastructure, and registers all known routes.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class ApiServer {

    private static final Logger log = Logger.getLogger(ApiServer.class.getName());

    private final HttpServer server;
    private final Router router;
    private final ResponseWriter responseWriter;
    private final RequestParser requestParser;
    private final Database database;
    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;

    private ApiServer(HttpServer server, Router router, ResponseWriter responseWriter,
                      RequestParser requestParser, Database database, AppConfig appConfig,
                      ObjectMapper objectMapper) {
        this.server = server;
        this.router = router;
        this.responseWriter = responseWriter;
        this.requestParser = requestParser;
        this.database = database;
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates an {@link ApiServer} bound to the given configuration and database.
     *
     * @param config   the application configuration
     * @param database the database instance (must be opened before calling this method)
     * @return a fully configured and started {@code ApiServer}
     * @throws IOException if the server cannot be created or started
     */
    public static ApiServer create(AppConfig config, Database database) throws IOException {
        ObjectMapper objectMapper = JsonSupport.getObjectMapper();

        HttpServer server = HttpServer.create(new InetSocketAddress(config.getPort()), 0);

        ResponseWriter responseWriter = new ResponseWriter(objectMapper);
        Router router = new Router(responseWriter);
        RequestParser requestParser = new RequestParser(objectMapper);

        MediaGroupService mediaGroupService = new MediaGroupService(database, config.getPageSize());
        MediaItemService mediaItemService = new MediaItemService(database, config);

        String basePath = config.getApiBasePath();
        int defaultPageSize = config.getPageSize();
        ApiServer apiServer = new ApiServer(server, router, responseWriter, requestParser, database, config,
                                            objectMapper);

        // Register the health check endpoint
        apiServer.registerHealthEndpoint();

        // Register direct MediaItem resource endpoints (must come before nested collection
        // routes so the Router tries them first for /api/media-items paths)
        apiServer.registerMediaItemDirectEndpoints(mediaItemService);

        // Register MediaItem nested collection endpoints (must come before MediaGroup routes
        // so the Router tries the item handler first for /api/media-groups paths)
        apiServer.registerMediaItemCollectionEndpoints(mediaItemService);

        // Register MediaGroup endpoints
        apiServer.registerMediaGroupEndpoints(mediaGroupService);

        // Register thumbnail endpoints
        apiServer.registerThumbnailEndpoints();

        // Register streaming endpoints:
        apiServer.registerStreamingEndpoints();

        // Register playlist endpoints
        apiServer.registerPlaylistEndpoints(mediaItemService, mediaGroupService);

        // Register file browser endpoint:
        apiServer.registerFileBrowserEndpoint();

        // Mount the router at the API base path (more specific path first)
        server.createContext(basePath, apiServer::handleAll);

        // Mount the static frontend handler at the root path (catch-all for non-API requests)
        server.createContext("/", new StaticFrontendHandler());

        return apiServer;
    }

    /**
     * Registers the {@code GET /api/health} endpoint.
     */
    private void registerHealthEndpoint() {
        Route route = new Route("GET", appConfig.getApiBasePath() + "health");
        HealthHandler handler = new HealthHandler(responseWriter, appConfig.getApiBasePath());
        router.register(route, handler);
    }

    /**
     * Registers all MediaGroup endpoints.
     * <p>
     * Routes:
     * <ul>
     *   <li>POST   /api/media-groups</li>
     *   <li>GET    /api/media-groups</li>
     *   <li>GET    /api/media-groups/{groupId}</li>
     *   <li>PUT    /api/media-groups/{groupId}</li>
     *   <li>DELETE /api/media-groups/{groupId}</li>
     * </ul>
     */
    private void registerMediaGroupEndpoints(MediaGroupService mediaGroupService) {
        Route route;
        MediaGroupHandler handler = new MediaGroupHandler(mediaGroupService, responseWriter, requestParser, appConfig);

        // POST /api/media-groups — create
        route = new Route("POST", appConfig.getApiBasePath() + "media-groups");
        router.register(route, handler);

        // GET /api/media-groups — list/search
        route = new Route("GET", appConfig.getApiBasePath() + "media-groups");
        router.register(route, handler);

        // PUT /api/media-groups/{groupId} — update
        route = new Route("PUT", appConfig.getApiBasePath() + "media-groups");
        router.register(route, handler);

        // DELETE /api/media-groups/{groupId} — delete
        route = new Route("DELETE", appConfig.getApiBasePath() + "media-groups");
        router.register(route, handler);

        // GET /api/media-groups/{groupId} — get by ID
        route = new Route("GET", appConfig.getApiBasePath() + "media-groups");
        router.register(route, handler);
    }

    /**
     * Registers direct MediaItem resource endpoints.
     * <p>
     * Routes:
     * <ul>
     *   <li>GET    /api/media-items</li>
     *   <li>GET    /api/media-items/{itemId}</li>
     *   <li>PUT    /api/media-items/{itemId}</li>
     *   <li>DELETE /api/media-items/{itemId}</li>
     * </ul>
     */
    private void registerMediaItemDirectEndpoints(MediaItemService mediaItemService) {
        Route route;
        MediaItemHandler handler = new MediaItemHandler(mediaItemService, responseWriter, requestParser, appConfig);

        // GET /api/media-items — list/search across all groups
        // GET /api/media-items/{itemId} — get by ID
        route = new Route("GET", appConfig.getApiBasePath() + "media-items");
        router.register(route, handler);

        // PUT /api/media-items/{itemId} — update
        route = new Route("PUT", appConfig.getApiBasePath() + "media-items");
        router.register(route, handler);

        // DELETE /api/media-items/{itemId} — delete
        route = new Route("DELETE", appConfig.getApiBasePath() + "media-items");
        router.register(route, handler);
    }

    /**
     * Registers nested MediaItem collection endpoints.
     * <p>
     * Routes:
     * <ul>
     *   <li>POST   /api/media-groups/{groupId}/items</li>
     *   <li>GET    /api/media-groups/{groupId}/items</li>
     * </ul>
     */
    private void registerMediaItemCollectionEndpoints(MediaItemService mediaItemService) {
        Route route;
        MediaItemHandler handler = new MediaItemHandler(mediaItemService, responseWriter, requestParser, appConfig);

        // POST /api/media-groups/{groupId}/items — create item within group
        route = new Route("POST", appConfig.getApiBasePath() + "media-groups");
        router.register(route, handler);

        // GET /api/media-groups/{groupId}/items — list/search items within group
        route = new Route("GET", appConfig.getApiBasePath() + "media-groups");
        router.register(route, handler);
    }

    /**
     * Registers thumbnail management endpoints.
     * <p>
     * Routes:
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
     */
    private void registerThumbnailEndpoints() {
        Route route;
        ThumbnailHandler handler = new ThumbnailHandler(appConfig, database, responseWriter, objectMapper);

        // MediaItem thumbnail routes
        route = new Route("GET", appConfig.getApiBasePath() + "thumbnails/media-items");
        router.register(route, handler);

        route = new Route("POST", appConfig.getApiBasePath() + "thumbnails/media-items");
        router.register(route, handler);

        route = new Route("PUT", appConfig.getApiBasePath() + "thumbnails/media-items");
        router.register(route, handler);

        route = new Route("DELETE", appConfig.getApiBasePath() + "thumbnails/media-items");
        router.register(route, handler);

        // MediaGroup thumbnail routes
        route = new Route("GET", appConfig.getApiBasePath() + "thumbnails/media-groups");
        router.register(route, handler);

        route = new Route("POST", appConfig.getApiBasePath() + "thumbnails/media-groups");
        router.register(route, handler);

        route = new Route("PUT", appConfig.getApiBasePath() + "thumbnails/media-groups");
        router.register(route, handler);

        route = new Route("DELETE", appConfig.getApiBasePath() + "thumbnails/media-groups");
        router.register(route, handler);
    }

    /**
     * Registers streaming endpoints.
     * <p>
     * Routes:
     * </p>
     * <ul>
     *     <li>GET /api/stream/{mediaItemId}</li>
     * </ul>
     * <p>
     * Note that these endpoints are for simple read-only streaming of MediaItems. To create or
     * manage MediaItems, refer to the MediaItemHandler.
     * </p>
     */
    private void registerStreamingEndpoints() {
        Route route;
        StreamHandler handler = new StreamHandler(appConfig, database);

        // GET /api/stream/{mediaItemId} — stream media item content
        route = new Route("GET", appConfig.getApiBasePath() + "stream");
        router.register(route, handler);
    }

    /**
     * Registers playlist endpoints.
     * <p>
     * Routes:
     * </p>
     * <ul>
     *     <li>GET /api/playlist/media-item/{id}</li>
     *     <li>POST /api/playlist/media-item</li>
     *     <li>GET /api/playlist/media-group/{id}</li>
     * </ul>
     */
    private void registerPlaylistEndpoints(MediaItemService mediaItemService, MediaGroupService mediaGroupService) {
        Route route;
        PlaylistHandler handler = new PlaylistHandler(database, mediaGroupService, responseWriter, requestParser,
                                                      appConfig);

        // GET /api/playlist/media-item/{id} — single media item playlist
        route = new Route("GET", appConfig.getApiBasePath() + "playlist/media-item");
        router.register(route, handler);

        // POST /api/playlist/media-item — multi-item playlist by IDs
        route = new Route("POST", appConfig.getApiBasePath() + "playlist/media-item");
        router.register(route, handler);

        // GET /api/playlist/media-group/{id} — playlist from media group
        route = new Route("GET", appConfig.getApiBasePath() + "playlist/media-group");
        router.register(route, handler);
    }

    private void registerFileBrowserEndpoint() {
        Route route;
        FileBrowserHandler handler = new FileBrowserHandler(appConfig, responseWriter);

        // GET /api/files[?path=...] — list files in media directory
        route = new Route("GET", appConfig.getApiBasePath() + "files");
        router.register(route, handler);
    }

    /**
     * Handles all requests under the API base path, delegating to the router
     * and wrapping errors in JSON error responses.
     */
    void handleAll(com.sun.net.httpserver.HttpExchange exchange) {
        try {
            router.handle(exchange);
        }
        catch (Exception ex) {
            handleException(exchange, ex);
        }
    }

    /**
     * Handles exceptions thrown during request processing by mapping them
     * to appropriate HTTP status codes and JSON error responses.
     */
    private void handleException(com.sun.net.httpserver.HttpExchange exchange, Exception ex) {
        try {
            Object[] mapped = ExceptionMapper.map(ex);
            int statusCode = (int)mapped[0];
            ErrorResponse errorResponse = (ErrorResponse)mapped[1];

            responseWriter.writeJson(exchange, statusCode, errorResponse);
        }
        catch (IOException ioEx) {
            log.log(Level.SEVERE, "Error writing error response", ioEx);
            try {
                exchange.sendResponseHeaders(500, -1);
            }
            catch (IOException ignored) {
            }
        }
    }

    /**
     * Starts the server (if not already started).
     */
    public void start() {
        server.start();
    }

    /**
     * Stops the server gracefully.
     */
    public void stop() {
        server.stop(1);
        log.info("API server stopped.");
    }

    /**
     * Returns the underlying {@link HttpServer}.
     */
    public HttpServer getServer() {
        return server;
    }
}
