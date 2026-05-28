package ca.corbett.movienight.api.handler;

import ca.corbett.movienight.api.util.ExceptionMapper;
import ca.corbett.movienight.api.util.QueryParamParser;
import ca.corbett.movienight.api.util.RequestParser;
import ca.corbett.movienight.api.util.ResponseWriter;
import ca.corbett.movienight.config.AppConfig;
import ca.corbett.movienight.db.Database;
import ca.corbett.movienight.model.MediaItem;
import ca.corbett.movienight.service.MediaGroupService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * HTTP handler for returning VLC-compatible (m3u) playlists of items,
 * as an alternative to HTTP streaming as offered by {@link StreamHandler}.
 * <p>
 * If the client machine has VLC installed, and has configured their browser
 * to auto-open m3u playlist files, these endpoints can be a nice alternative
 * to using the HTML5 video player. Specifically, allowing VLC to stream the
 * media instead of using the HTML5 video player can yield not only better
 * streaming performance, but also access to multiple audio and subtitle
 * tracks for media that has them.
 * </p>
 * <p>
 * Routes:
 * </p>
 * <ul>
 *     <li><code>GET /api/playlist/media-item/{id}</code> - returns an m3u playlist
 *     consisting of a single media item with the given ID.</li>
 *     <li><code>POST /api/playlist/media-item</code> - accepts a JSON body with an array
 *     of media item IDs, and returns an m3u playlist containing those media items in the order specified.
 *     The body should be of the form <code>{ "mediaItemIds": [1, 2, 3] }</code>.</li>
 *     <li><code>GET /api/playlist/media-group/{id}</code> - returns an m3u playlist
 *     consisting of all media items in the media group with the given ID. Note that only
 *     direct child items of the given group are returned - this is not a recursive search.</li>
 * </ul>
 * <p>
 *     An optional query parameter <code>?local=true</code> can be added to any of the above routes to
 *     indicate that the generated playlist should return direct filesystem paths instead of streaming URLs.
 *     This is intended for the case where the browser is being used on the same machine as the server (or where
 *     both client machine and server machine have access to the same shared filesystem). Player performance
 *     will be much improved in this case, since there is no http streaming involved. The default is false,
 *     meaning that streaming URLs will be used in the generated playlist.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class PlaylistHandler implements HttpHandler {
    private static final Logger log = Logger.getLogger(PlaylistHandler.class.getName());

    private final ResponseWriter responseWriter;
    private final RequestParser requestParser;
    private final Database database;
    private final MediaGroupService mediaGroupService;
    private final AppConfig appConfig;

    public PlaylistHandler(Database database, MediaGroupService mediaGroupService,
                           ResponseWriter responseWriter, RequestParser requestParser,
                           AppConfig appConfig) {
        this.database = database;
        this.mediaGroupService = mediaGroupService;
        this.responseWriter = responseWriter;
        this.requestParser = requestParser;
        this.appConfig = appConfig;
    }

    public static class PlaylistMediaItemIdsRequest {
        public long[] mediaItemIds;

        public long[] getMediaItemIds() {
            return mediaItemIds;
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            boolean isLocal = parseLocalParam(exchange);

            if ("GET".equals(method)) {
                handleGetPlaylist(exchange, path, isLocal);
            }
            else if ("POST".equals(method)) {
                handlePostPlaylist(exchange, path, isLocal);
            }
            else {
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(405, 0);
            }
        }
        catch (Database.NotFoundException e) {
            var mapped = ExceptionMapper.map(e);
            responseWriter.writeJson(exchange, (int)mapped[0], mapped[1]);
        }
        catch (IllegalArgumentException e) {
            var mapped = ExceptionMapper.map(e);
            responseWriter.writeJson(exchange, (int)mapped[0], mapped[1]);
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
     * Looks for a query parameter "local" in the request URI, and returns true if
     * found and if the value is "true" (case-insensitive).
     * If the parameter is not found, or if it has any other value, returns false.
     * See class javadocs for a description of what this parameter does.
     */
    private boolean parseLocalParam(HttpExchange exchange) {
        Map<String, String> params = QueryParamParser.parse(exchange.getRequestURI().getQuery());
        return QueryParamParser.parseBoolean(params, "local");
    }

    private void handleGetPlaylist(HttpExchange exchange, String path, boolean isLocal) throws Exception {
        String prefix = appConfig.getApiBasePath() + "playlist/";
        if (!path.startsWith(prefix)) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }
        String remainder = path.substring(prefix.length());

        if ("media-item".equals(remainder)) {
            handleSingleMediaItemPlaylist(exchange, isLocal);
        }
        else if (remainder.startsWith("media-item/")) {
            handleSingleMediaItemPlaylistById(exchange, remainder.substring("media-item/".length()), isLocal);
        }
        else if (remainder.startsWith("media-group/")) {
            handleMediaGroupPlaylist(exchange, remainder.substring("media-group/".length()), isLocal);
        }
        else {
            throw new IllegalArgumentException("Unknown playlist route: " + remainder);
        }
    }

    private void handlePostPlaylist(HttpExchange exchange, String path, boolean isLocal) throws Exception {
        String prefix = appConfig.getApiBasePath() + "playlist/";
        if (!path.startsWith(prefix)) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }
        String remainder = path.substring(prefix.length());

        if ("media-item".equals(remainder)) {
            handleMultiMediaItemPlaylist(exchange, isLocal);
        }
        else {
            throw new IllegalArgumentException("Unknown playlist route: " + remainder);
        }
    }

    private void handleSingleMediaItemPlaylistById(HttpExchange exchange, String idStr, boolean isLocal)
            throws Exception {
        int itemId;
        try {
            itemId = Integer.parseInt(idStr);
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid media item ID: " + idStr);
        }
        if (itemId <= 0) {
            throw new IllegalArgumentException("Media item ID must be a positive integer: " + idStr);
        }

        MediaItem item = database.getMediaItemById(itemId);
        if (item == null) {
            throw new Database.NotFoundException("Media item not found with ID: " + itemId);
        }

        String playlist = generatePlaylist(List.of(item), appConfig, getServerName(exchange), isLocal);
        responseWriter.writeText(exchange, HttpURLConnection.HTTP_OK, playlist);
    }

    private void handleSingleMediaItemPlaylist(HttpExchange exchange, boolean isLocal) throws Exception {
        String path = exchange.getRequestURI().getPath();
        String prefix = appConfig.getApiBasePath() + "playlist/media-item";
        String idStr = path.substring(prefix.length()).replace("/", "");
        if (idStr.isEmpty()) {
            throw new IllegalArgumentException("Media item ID is required");
        }
        int itemId;
        try {
            itemId = Integer.parseInt(idStr);
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid media item ID: " + idStr);
        }
        if (itemId <= 0) {
            throw new IllegalArgumentException("Media item ID must be a positive integer: " + idStr);
        }

        MediaItem item = database.getMediaItemById(itemId);
        if (item == null) {
            throw new Database.NotFoundException("Media item not found with ID: " + itemId);
        }

        String playlist = generatePlaylist(List.of(item), appConfig, getServerName(exchange), isLocal);
        responseWriter.writeText(exchange, HttpURLConnection.HTTP_OK, playlist);
    }

    private void handleMediaGroupPlaylist(HttpExchange exchange, String groupIdStr, boolean isLocal) throws Exception {
        long groupId;
        try {
            groupId = Long.parseLong(groupIdStr);
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid media group ID: " + groupIdStr);
        }
        if (groupId <= 0) {
            throw new IllegalArgumentException("Media group ID must be a positive integer: " + groupIdStr);
        }

        List<MediaItem> items = database.getMediaItemsByGroupId(groupId);
        String playlist = generatePlaylist(items, appConfig, getServerName(exchange), isLocal);
        responseWriter.writeText(exchange, HttpURLConnection.HTTP_OK, playlist);
    }

    private void handleMultiMediaItemPlaylist(HttpExchange exchange, boolean isLocal) throws Exception {
        if (!requestParser.hasBody(exchange)) {
            throw new IllegalArgumentException("Request body is required");
        }

        PlaylistMediaItemIdsRequest request = requestParser.readBody(exchange, PlaylistMediaItemIdsRequest.class);
        if (request.mediaItemIds == null || request.mediaItemIds.length == 0) {
            throw new IllegalArgumentException("mediaItemIds array is required and must not be empty");
        }

        List<MediaItem> items = new java.util.ArrayList<>();
        for (long id : request.mediaItemIds) {
            MediaItem item = database.getMediaItemById(id);
            if (item != null) {
                items.add(item);
            }
        }

        String playlist = generatePlaylist(items, appConfig, getServerName(exchange), isLocal);
        responseWriter.writeText(exchange, HttpURLConnection.HTTP_OK, playlist);
    }

    private String getServerName(HttpExchange exchange) {
        String hostHeader = exchange.getRequestHeaders().getFirst("Host");
        if (hostHeader != null && !hostHeader.isEmpty()) {
            // Host header format is "hostname:port" or "[ipv6]:port"
            int colonIdx = hostHeader.lastIndexOf(':');
            if (colonIdx > 0) {
                return hostHeader.substring(0, colonIdx);
            }
            return hostHeader;
        }
        return "localhost";
    }

    /**
     * A utility method to generate an m3u playlist in String form, given a list of mediaItems and basic
     * information about the server. The returned playlist will contain one entry for each valid MediaItem
     * in the list, in the order they appear in the list. Invalid MediaItems (null, no id, missing media file)
     * will be silently ignored.
     *
     * @param mediaItems A List of at least one MediaItem to include in the playlist.
     * @param appConfig  Contains our server port and our data directory.
     * @param serverName The server name or IP address to use in the streaming URLs in the playlist.
     * @param isLocal   If true, the playlist will contain direct filesystem paths instead of streaming URLs.
     * @return An m3u playlist as a String. May be empty if mediaItems was empty, or if it contained no valid MediaItems.
     */
    public static String generatePlaylist(List<MediaItem> mediaItems, AppConfig appConfig, String serverName, boolean isLocal) {
        StringBuilder m3u = new StringBuilder();
        m3u.append("#EXTM3U\n");
        for (MediaItem mediaItem : mediaItems) {
            if (mediaItem == null || mediaItem.getId() == 0 || mediaItem.getMediaFilePath() == null) {
                log.warning("generatePlaylist: skipping invalid media item.");
                continue;
            }
            String relPath = mediaItem.getMediaFilePath().replaceFirst("^/", "");
            File mediaFile = appConfig.getDataDir().resolve(relPath).toFile();
            if (!mediaFile.exists() || !mediaFile.canRead()) {
                log.warning("generatePlaylist: skipping media item with missing or unreadable media file: "
                                    + mediaFile.getAbsolutePath());
                continue;
            }
            String itemTitle = mediaItem.getTitle() != null
                    ? mediaItem.getTitle()
                    : "Untitled Media Item " + mediaItem.getId();
            m3u.append("#EXTINF:-1,").append(itemTitle).append("\n");
            if (isLocal) {
                // Browser is on the same machine or has access to the same shared filesystem:
                m3u.append(mediaFile.getAbsolutePath());
            }
            else {
                // Browser is remote, needs to stream over HTTP:
                m3u.append(buildStreamUrl(mediaItem, serverName, appConfig));
            }
            m3u.append("\n");
        }
        return m3u.toString();
    }

    /**
     * Utility method to build up a streaming URL pointing back at this server for a given media item.
     * The URL will point to our streaming endpoint with the media item's ID.
     *
     * @param mediaItem  The MediaItem to consider.
     * @param serverName The hostname or IP address of this server, as it should appear in the URL.
     * @param appConfig  Contains our server port.
     * @return A URL pointing back to this server's streaming endpoint for the given MediaItem.
     */
    public static String buildStreamUrl(MediaItem mediaItem, String serverName, AppConfig appConfig) {
        return "http://" + serverName + ":" + appConfig.getPort() + "/api/stream/" + mediaItem.getId();
    }
}
