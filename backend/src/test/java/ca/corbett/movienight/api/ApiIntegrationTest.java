package ca.corbett.movienight.api;

import ca.corbett.movienight.api.util.JsonSupport;
import ca.corbett.movienight.config.AppConfig;
import ca.corbett.movienight.db.Database;
import ca.corbett.movienight.model.MediaItem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the REST API.
 * <p>
 * Starts an ephemeral HttpServer on a random port with a temporary SQLite database,
 * exercises JSON request/response behavior end-to-end, and verifies status codes,
 * headers, payloads, pagination, and error mapping.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
class ApiIntegrationTest {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
                                                            .connectTimeout(java.time.Duration.ofSeconds(5))
                                                            .build();

    private static int PORT;
    private static String BASE_URL;
    private static ApiServer apiServer;
    private static Database database;
    private static Path tempDir;

    private long createdGroupId;
    private long createdItemId;

    /**
     * One-time setup: create the database, start the server, discover the random port.
     */
    @BeforeAll
    static void setUpServer() throws Exception {
        tempDir = Files.createTempDirectory("integration-test-");
        File thumbDir = tempDir.resolve("thumbnails").toFile();
        if (!thumbDir.mkdirs()) {
            throw new IOException("Failed to create thumbnail directory for integration tests");
        }
        File dbFile = tempDir.resolve("test.db").toFile();
        final int port = 0; // 0 == ephemeral random port
        AppConfig appConfig = AppConfig.of(port, tempDir, thumbDir.toPath(), dbFile.toPath(), 10, "/api/", 32);
        System.out.println("ApiIntegrationTest using AppConfig: " + appConfig);
        database = new Database(appConfig);
        database.open();

        apiServer = ApiServer.create(appConfig, database);
        apiServer.start();

        PORT = apiServer.getServer().getAddress().getPort();
        BASE_URL = "http://localhost:" + PORT + "/api";

        System.out.println("Integration test server running on port " + PORT);
    }

    /**
     * One-time teardown: stop the server.
     */
    @AfterAll
    static void tearDownServer() {
        if (apiServer != null) {
            apiServer.stop();
        }
        if (database != null) {
            database.dispose();
        }
    }

    /**
     * Clear all data before each test by deleting all groups and items.
     */
    @BeforeEach
    void clearDatabase() throws SQLException {
        database.executeInTransaction(() -> {
            // Delete all items first (cascade will handle groups)
            try (var stmt = database.getConnection().createStatement()) {
                stmt.execute("DELETE FROM MediaItem");
                stmt.execute("DELETE FROM MediaGroup");
            }
            return null;
        });
    }

    // ========================================================================
    // Health Check
    // ========================================================================

    @Test
    void healthCheck_shouldReturnOk() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/health")).GET().build()
        );

        assertEquals(200, response.statusCode());
        assertEquals("application/json; charset=utf-8", response.headers().firstValue("Content-Type").orElse(""));
        assertTrue(response.body().contains("\"status\":\"ok\""));
    }

    @Test
    void healthCheck_postShouldReturn405() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/health")).POST(
                        HttpRequest.BodyPublishers.ofString("")
                ).build()
        );

        assertEquals(405, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"Method Not Allowed\""));
    }

    // ========================================================================
    // MediaGroup - Create
    // ========================================================================

    @Test
    void createMediaGroup_topLevel_returns201WithLocation() throws Exception {
        String body = """
                {"title":"TV Shows","description":"All TV series"}
                """;

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(body))
                           .build()
        );

        assertEquals(201, response.statusCode());
        assertTrue(response.headers().firstValue("Location").isPresent());
        String location = response.headers().firstValue("Location").orElse("");
        assertTrue(location.contains("/api/media-groups/"));

        // Parse response body
        Map<String, Object> json = parseJson(response.body());
        assertEquals("TV Shows", json.get("title"));
        assertEquals("All TV series", json.get("description"));
        assertNull(json.get("parentGroupId"));
        createdGroupId = Long.parseLong(json.get("id").toString());
    }

    @Test
    void createMediaGroup_childGroup_preservesParent() throws Exception {
        // Create parent first
        HttpResponse<String> parentResp = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"title\":\"TV Shows\",\"description\":\"Parent\"}"))
                           .build()
        );
        assertEquals(201, parentResp.statusCode());
        long parentGroupId = ((Number)parseJson(parentResp.body()).get("id")).longValue();

        // Create child
        HttpResponse<String> childResp = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"parentGroupId\":" + parentGroupId + ",\"title\":\"Dexter\",\"description\":\"Crime drama\"}"))
                           .build()
        );

        assertEquals(201, childResp.statusCode());
        Map<String, Object> childJson = parseJson(childResp.body());
        assertEquals(parentGroupId, ((Number)childJson.get("parentGroupId")).longValue());
        assertEquals("Dexter", childJson.get("title"));
    }

    @Test
    void createMediaGroup_blankTitle_returns400() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString("{\"title\":\"   \"}"))
                           .build()
        );

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"Bad Request\""));
        assertTrue(response.body().contains("title"));
    }

    @Test
    void createMediaGroup_malformedJson_returns400() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString("{bad json"))
                           .build()
        );

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"Bad Request\""));
    }

    // ========================================================================
    // MediaGroup - Read
    // ========================================================================

    @Test
    void getMediaGroupById_existingGroup_returns200() throws Exception {
        long groupId = createTestGroup("Test Group", "Test desc");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/media-groups/" + groupId)).GET().build()
        );

        assertEquals(200, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        assertEquals(groupId, ((Number)json.get("id")).longValue());
        assertEquals("Test Group", json.get("title"));
    }

    @Test
    void getMediaGroupById_nonexistent_returns404() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/media-groups/99999")).GET().build()
        );

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"Not Found\""));
    }

    // ========================================================================
    // MediaGroup - Update
    // ========================================================================

    @Test
    void updateMediaGroup_fullReplacement_returns200() throws Exception {
        long groupId = createTestGroup("Original", "Original desc");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups/" + groupId))
                           .header("Content-Type", "application/json")
                           .PUT(HttpRequest.BodyPublishers.ofString(
                                   "{\"title\":\"Updated\",\"description\":\"Updated desc\"}"))
                           .build()
        );

        assertEquals(200, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        assertEquals("Updated", json.get("title"));
        assertEquals("Updated desc", json.get("description"));

        // Verify via GET
        HttpResponse<String> getResp = sendRequest(
                HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/media-groups/" + groupId)).GET().build()
        );
        assertEquals(200, getResp.statusCode());
        Map<String, Object> getJson = parseJson(getResp.body());
        assertEquals("Updated", getJson.get("title"));
    }

    @Test
    void updateMediaGroup_nonexistent_returns404() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups/99999"))
                           .header("Content-Type", "application/json")
                           .PUT(HttpRequest.BodyPublishers.ofString("{\"title\":\"Ghost\"}"))
                           .build()
        );

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"Not Found\""));
    }

    @Test
    void updateMediaGroup_blankTitle_returns400() throws Exception {
        long groupId = createTestGroup("Original", "desc");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups/" + groupId))
                           .header("Content-Type", "application/json")
                           .PUT(HttpRequest.BodyPublishers.ofString("{\"title\":\"   \"}"))
                           .build()
        );

        assertEquals(400, response.statusCode());
    }

    // ========================================================================
    // MediaGroup - Delete
    // ========================================================================

    @Test
    void deleteMediaGroup_existing_returns204() throws Exception {
        long groupId = createTestGroup("To Delete", null);

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups/" + groupId))
                           .DELETE()
                           .build()
        );

        assertEquals(204, response.statusCode());

        // Verify deletion
        HttpResponse<String> getResp = sendRequest(
                HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/media-groups/" + groupId)).GET().build()
        );
        assertEquals(404, getResp.statusCode());
    }

    @Test
    void deleteMediaGroup_nonexistent_returns404() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups/99999"))
                           .DELETE()
                           .build()
        );

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"Not Found\""));
    }

    @Test
    void deleteMediaGroup_cascadesToChildGroupsAndItems() throws Exception {
        // Create parent group
        HttpResponse<String> parentResp = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"title\":\"Parent\",\"description\":\"Parent group\"}"))
                           .build()
        );
        assertEquals(201, parentResp.statusCode());
        long parentGroupId = Long.parseLong(parseJson(parentResp.body()).get("id").toString());

        // Create child group
        HttpResponse<String> childResp = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"parentGroupId\":" + parentGroupId + ",\"title\":\"Child\",\"description\":\"Child group\"}"))
                           .build()
        );
        assertEquals(201, childResp.statusCode());
        long childGroupId = Long.parseLong(parseJson(childResp.body()).get("id").toString());

        // Create item in child group
        HttpResponse<String> itemResp = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups/" + childGroupId + "/items"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"title\":\"Episode 1\",\"mediaFilePath\":\"/ep1.mkv\"}"))
                           .build()
        );
        assertEquals(201, itemResp.statusCode());
        long itemId = Long.parseLong(parseJson(itemResp.body()).get("id").toString());

        // Delete parent group
        HttpResponse<String> deleteResp = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups/" + parentGroupId))
                           .DELETE()
                           .build()
        );
        assertEquals(204, deleteResp.statusCode());

        // Verify cascade
        HttpResponse<String> childGet = sendRequest(
                HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/media-groups/" + childGroupId)).GET().build()
        );
        assertEquals(404, childGet.statusCode());

        HttpResponse<String> itemGet = sendRequest(
                HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/media-items/" + itemId)).GET().build()
        );
        assertEquals(404, itemGet.statusCode());
    }

    // ========================================================================
    // MediaGroup - List/Search
    // ========================================================================

    @Test
    void listMediaGroups_defaultPagination_returns200() throws Exception {
        createTestGroup("Group A", "desc A");
        createTestGroup("Group B", "desc B");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/media-groups")).GET().build()
        );

        assertEquals(200, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        assertNotNull(json.get("items"));
        assertNotNull(json.get("totalCount"));
        assertNotNull(json.get("pageNumber"));
        assertNotNull(json.get("pageSize"));
        assertTrue(((Number)json.get("totalCount")).longValue() >= 2);
    }

    @Test
    void listMediaGroups_topLevelOnly_returnsOnlyTopLevel() throws Exception {
        HttpResponse<String> parentResp = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"title\":\"Parent\",\"description\":\"Parent\"}"))
                           .build()
        );
        assertEquals(201, parentResp.statusCode());
        long parentGroupId = Long.parseLong(parseJson(parentResp.body()).get("id").toString());

        sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"parentGroupId\":" + parentGroupId + ",\"title\":\"Child\",\"description\":\"Child\"}"))
                           .build()
        );

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups?topLevelOnly=true"))
                           .GET()
                           .build()
        );

        assertEquals(200, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        List<?> items = (List<?>)json.get("items");
        assertEquals(1, items.size());
        Map<String, Object> item = (Map<String, Object>)items.get(0);
        assertNull(item.get("parentGroupId"));
    }

    @Test
    void listMediaGroups_parentGroupIdFilter_returnsOnlyChildren() throws Exception {
        HttpResponse<String> parentResp = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"title\":\"Parent\",\"description\":\"Parent\"}"))
                           .build()
        );
        assertEquals(201, parentResp.statusCode());
        long parentGroupId = Long.parseLong(parseJson(parentResp.body()).get("id").toString());

        sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"parentGroupId\":" + parentGroupId + ",\"title\":\"Child1\",\"description\":\"Child1\"}"))
                           .build()
        );
        sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"parentGroupId\":" + parentGroupId + ",\"title\":\"Child2\",\"description\":\"Child2\"}"))
                           .build()
        );

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups?parentGroupId=" + parentGroupId))
                           .GET()
                           .build()
        );

        assertEquals(200, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        List<?> items = (List<?>)json.get("items");
        assertEquals(2, items.size());
    }

    @Test
    void listMediaGroups_titleContains_returnsMatchingGroups() throws Exception {
        createTestGroup("Action Movies", "Explosions");
        createTestGroup("Comedy", "Laughs");
        createTestGroup("Action Thriller", "More action");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups?titleContains=action"))
                           .GET()
                           .build()
        );

        assertEquals(200, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        List<?> items = (List<?>)json.get("items");
        assertEquals(2, items.size());
    }

    @Test
    void listMediaGroups_conflictingParams_returns400() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups?topLevelOnly=true&parentGroupId=1"))
                           .GET()
                           .build()
        );

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"Bad Request\""));
    }

    @Test
    void listMediaGroups_incompletePagination_returns400() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups?pageNumber=1"))
                           .GET()
                           .build()
        );

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"Bad Request\""));
    }

    @Test
    void listMediaGroups_pagination_returnsCorrectPage() throws Exception {
        for (int i = 0; i < 5; i++) {
            createTestGroup("Group " + i, "Description " + i);
        }

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups?pageNumber=1&pageSize=2"))
                           .GET()
                           .build()
        );

        assertEquals(200, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        List<?> items = (List<?>)json.get("items");
        assertEquals(2, items.size());
        assertEquals(5, ((Number)json.get("totalCount")).longValue());
        assertEquals(1, json.get("pageNumber"));
        assertEquals(2, json.get("pageSize"));
    }

    @Test
    void listMediaGroups_invalidPageNumber_returns400() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups?pageNumber=0&pageSize=10"))
                           .GET()
                           .build()
        );

        assertEquals(400, response.statusCode());
    }

    @Test
    void listMediaGroups_invalidPageSize_returns400() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups?pageNumber=1&pageSize=-1"))
                           .GET()
                           .build()
        );

        assertEquals(400, response.statusCode());
    }

    // ========================================================================
    // MediaItem - Nested Collection (Create)
    // ========================================================================

    @Test
    void createItem_nested_returns201WithLocation() throws Exception {
        long groupId = createTestGroup("Group for items", null);

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups/" + groupId + "/items"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString("""
                                                                             {"title":"Episode 1","description":"Pilot","lastWatchedDate":"2026-05-01","mediaFilePath":"tv/ep1.mkv","tags":["crime","drama"]}
                                                                             """))
                           .build()
        );

        assertEquals(201, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        assertEquals(groupId, ((Number)json.get("mediaGroupId")).longValue());
        assertEquals("Episode 1", json.get("title"));
        assertTrue(response.headers().firstValue("Location").isPresent());
        createdItemId = ((Number)json.get("id")).longValue();
    }

    @Test
    void createItem_blankTitle_returns400() throws Exception {
        long groupId = createTestGroup("Group", null);

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups/" + groupId + "/items"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"title\":\"   \",\"mediaFilePath\":\"/test.mkv\"}"))
                           .build()
        );

        assertEquals(400, response.statusCode());
    }

    @Test
    void createItem_blankFilePath_returns400() throws Exception {
        long groupId = createTestGroup("Group", null);

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups/" + groupId + "/items"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString("{\"title\":\"Test\",\"mediaFilePath\":\"   \"}"))
                           .build()
        );

        assertEquals(400, response.statusCode());
    }

    @Test
    void createItem_nonexistentGroup_returns400() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups/99999/items"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"title\":\"Test\",\"mediaFilePath\":\"/test.mkv\"}"))
                           .build()
        );

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"Bad Request\""));
    }

    // ========================================================================
    // MediaItem - Nested Collection (List/Search)
    // ========================================================================

    @Test
    void listItems_nested_returns200() throws Exception {
        long groupId = createTestGroup("Group", null);
        createTestItem(groupId, "Episode 1", "/ep1.mkv");
        createTestItem(groupId, "Episode 2", "/ep2.mkv");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups/" + groupId + "/items"))
                           .GET()
                           .build()
        );

        assertEquals(200, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        List<?> items = (List<?>)json.get("items");
        assertEquals(2, items.size());
    }

    @Test
    void listItems_titleFilter_returnsMatchingItems() throws Exception {
        long groupId = createTestGroup("Group", null);
        createTestItem(groupId, "Dexter S01E01", "/dexter.mkv");
        createTestItem(groupId, "Breaking Bad S01E01", "/breaking.mkv");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups/" + groupId + "/items?titleContains=dexter"))
                           .GET()
                           .build()
        );

        assertEquals(200, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        List<?> items = (List<?>)json.get("items");
        assertEquals(1, items.size());
    }

    @Test
    void listItems_tagFilter_returnsMatchingItems() throws Exception {
        long groupId = createTestGroup("Group", null);
        MediaItem item1 = new MediaItem();
        item1.setMediaGroupId(groupId);
        item1.setTitle("Item A");
        item1.setMediaFilePath("/a.mkv");
        item1.setTags(List.of("sci-fi", "drama"));
        database.createMediaItem(item1);

        MediaItem item2 = new MediaItem();
        item2.setMediaGroupId(groupId);
        item2.setTitle("Item B");
        item2.setMediaFilePath("/b.mkv");
        item2.setTags(List.of("comedy"));
        database.createMediaItem(item2);

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups/" + groupId + "/items?tagContains=sci-fi"))
                           .GET()
                           .build()
        );

        assertEquals(200, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        List<?> items = (List<?>)json.get("items");
        assertEquals(1, items.size());
    }

    @Test
    void listItems_mediaFilePathFilter_returnsMatchingItems() throws Exception {
        long groupId = createTestGroup("Group", null);
        createTestItem(groupId, "Movie A", "/path/to/arrival.mkv");
        createTestItem(groupId, "Movie B", "/path/to/arrival-remastered.mkv");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(
                                   BASE_URL + "/media-groups/" + groupId + "/items?mediaFilePathContains=arrival"))
                           .GET()
                           .build()
        );

        assertEquals(200, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        List<?> items = (List<?>)json.get("items");
        assertEquals(2, items.size());
    }

    @Test
    void listItems_pagination_returnsCorrectPage() throws Exception {
        long groupId = createTestGroup("Group", null);
        for (int i = 0; i < 5; i++) {
            createTestItem(groupId, "Item " + i, "/item" + i + ".mkv");
        }

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups/" + groupId + "/items?pageNumber=1&pageSize=2"))
                           .GET()
                           .build()
        );

        assertEquals(200, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        List<?> items = (List<?>)json.get("items");
        assertEquals(2, items.size());
        assertEquals(5, ((Number)json.get("totalCount")).longValue());
    }

    // ========================================================================
    // MediaItem - Global Collection (List/Search)
    // ========================================================================

    @Test
    void listItems_globalTitleFilter_searchesAcrossGroups() throws Exception {
        long groupA = createTestGroup("Group A", null);
        long groupB = createTestGroup("Group B", null);

        createTestItem(groupA, "Dexter S01E01", "/dexter.mkv");
        createTestItem(groupB, "Breaking Bad S01E01", "/breaking.mkv");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-items?titleContains=dexter"))
                           .GET()
                           .build()
        );

        assertEquals(200, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        List<?> items = (List<?>)json.get("items");
        assertEquals(1, items.size());
    }

    @Test
    void listItems_globalDescriptionFilter_searchesAcrossGroups() throws Exception {
        long groupA = createTestGroup("Group A", null);
        long groupB = createTestGroup("Group B", null);

        HttpResponse<String> createFirst = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups/" + groupA + "/items"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"title\":\"Arrival\",\"description\":\"First contact\",\"mediaFilePath\":\"/arrival.mkv\"}"
                           ))
                           .build()
        );
        assertEquals(201, createFirst.statusCode());

        HttpResponse<String> createSecond = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups/" + groupB + "/items"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"title\":\"Interstellar\",\"description\":\"Space travel\",\"mediaFilePath\":\"/interstellar.mkv\"}"
                           ))
                           .build()
        );
        assertEquals(201, createSecond.statusCode());

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-items?descriptionContains=contact"))
                           .GET()
                           .build()
        );

        assertEquals(200, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        List<?> items = (List<?>)json.get("items");
        assertEquals(1, items.size());
    }

    @Test
    void listItems_globalTagFilter_searchesAcrossGroups() throws Exception {
        long groupA = createTestGroup("Group A", null);
        long groupB = createTestGroup("Group B", null);

        MediaItem sciFi = new MediaItem();
        sciFi.setMediaGroupId(groupA);
        sciFi.setTitle("Item A");
        sciFi.setMediaFilePath("/a.mkv");
        sciFi.setTags(List.of("sci-fi", "drama"));
        database.createMediaItem(sciFi);

        MediaItem comedy = new MediaItem();
        comedy.setMediaGroupId(groupB);
        comedy.setTitle("Item B");
        comedy.setMediaFilePath("/b.mkv");
        comedy.setTags(List.of("comedy"));
        database.createMediaItem(comedy);

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-items?tagContains=sci-fi"))
                           .GET()
                           .build()
        );

        assertEquals(200, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        List<?> items = (List<?>)json.get("items");
        assertEquals(1, items.size());
    }

    // ========================================================================
    // MediaItem - Direct Resource (GET, PUT, DELETE)
    // ========================================================================

    @Test
    void getItemById_existingItem_returns200() throws Exception {
        long groupId = createTestGroup("Group", null);
        long itemId = createTestItem(groupId, "Episode 1", "/ep1.mkv");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-items/" + itemId))
                           .GET()
                           .build()
        );

        assertEquals(200, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        assertEquals(itemId, ((Number)json.get("id")).longValue());
        assertEquals("Episode 1", json.get("title"));
        assertEquals(groupId, ((Number)json.get("mediaGroupId")).longValue());
    }

    @Test
    void getItemById_nonexistent_returns404() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-items/99999"))
                           .GET()
                           .build()
        );

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"Not Found\""));
    }

    @Test
    void createItem_withAbsoluteMediaFilePath_shouldSaveAsRelativePath() throws Exception {
        // Given an absolute path for our new media item:
        final String mediaFilePath = tempDir.resolve("subdir1/subdir2/ep1.mkv").toString();

        // WHEN we save it:
        long groupId = createTestGroup("Group", null);
        String requestBody = """
                {
                    "title": "Episode 1",
                    "description": "Pilot",
                    "lastWatchedDate": "2026-05-01",
                    "mediaFilePath": "%s",
                    "tags": ["crime", "drama"]
                }
                """.formatted(mediaFilePath);
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups/" + groupId + "/items"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                           .build()
        );

        // THEN the path should have had the media dir stripped off and been saved as a relative path:
        assertEquals(201, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        assertEquals("subdir1/subdir2/ep1.mkv", json.get("mediaFilePath"));
    }

    @Test
    void createItem_withRelativeMediaFilePath_shouldSaveAsIs() throws Exception {
        // Given a relative path for our new media item:
        final String mediaFilePath = "subdir1/subdir2/ep1.mkv";

        // WHEN we save it:
        long groupId = createTestGroup("Group", null);
        String requestBody = """
                {
                    "title": "Episode 1",
                    "description": "Pilot",
                    "lastWatchedDate": "2026-05-01",
                    "mediaFilePath": "%s",
                    "tags": ["crime", "drama"]
                }
                """.formatted(mediaFilePath);
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups/" + groupId + "/items"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                           .build()
        );

        // THEN the path should be saved as-is:
        assertEquals(201, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        assertEquals("subdir1/subdir2/ep1.mkv", json.get("mediaFilePath"));
    }

    @Test
    void updateItem_fullReplacement_returns200() throws Exception {
        long groupId = createTestGroup("Group", null);
        long itemId = createTestItem(groupId, "Original", "/original.mkv");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-items/" + itemId))
                           .header("Content-Type", "application/json")
                           .PUT(HttpRequest.BodyPublishers.ofString("""
                                                                            {"mediaGroupId":%d,"title":"Updated","description":"Updated desc","lastWatchedDate":"2026-05-21","mediaFilePath":"/updated.mkv","tags":["updated"]}
                                                                            """.formatted(groupId)))
                           .build()
        );

        assertEquals(200, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        assertEquals("Updated", json.get("title"));
        assertEquals("updated.mkv", json.get("mediaFilePath")); // note path gets normalized

        // Verify via GET
        HttpResponse<String> getResp = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-items/" + itemId))
                           .GET()
                           .build()
        );
        assertEquals(200, getResp.statusCode());
        Map<String, Object> getJson = parseJson(getResp.body());
        assertEquals("Updated", getJson.get("title"));
    }

    @Test
    void updateItem_nonexistent_returns404() throws Exception {
        // Create a group first so mediaGroupId validation passes
        HttpResponse<String> groupResp = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"title\":\"Test Group\",\"description\":\"Test\"}"))
                           .build()
        );
        assertEquals(201, groupResp.statusCode());
        long groupId = ((Number)parseJson(groupResp.body()).get("id")).longValue();

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-items/99999"))
                           .header("Content-Type", "application/json")
                           .PUT(HttpRequest.BodyPublishers.ofString("""
                                                                            {"mediaGroupId":%d,"title":"Ghost","description":"No such item","lastWatchedDate":null,"mediaFilePath":"ghost.mkv","tags":[]}
                                                                            """.formatted(groupId)))
                           .build()
        );

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"Not Found\""));
    }

    @Test
    void updateItem_withNonexistentGroup_returns400() throws Exception {
        long groupId = createTestGroup("Group", null);
        long itemId = createTestItem(groupId, "Test", "/test.mkv");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-items/" + itemId))
                           .header("Content-Type", "application/json")
                           .PUT(HttpRequest.BodyPublishers.ofString("""
                                                                            {"mediaGroupId":99999,"title":"Test","mediaFilePath":"/test.mkv"}
                                                                            """))
                           .build()
        );

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"Bad Request\""));
    }

    @Test
    void deleteItem_existing_returns204() throws Exception {
        long groupId = createTestGroup("Group", null);
        long itemId = createTestItem(groupId, "To Delete", "/del.mkv");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-items/" + itemId))
                           .DELETE()
                           .build()
        );

        assertEquals(204, response.statusCode());

        // Verify deletion
        HttpResponse<String> getResp = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-items/" + itemId))
                           .GET()
                           .build()
        );
        assertEquals(404, getResp.statusCode());
    }

    @Test
    void deleteItem_nonexistent_returns404() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-items/99999"))
                           .DELETE()
                           .build()
        );

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"Not Found\""));
    }

    @Test
    void deleteItem_cascadeWhenGroupDeleted() throws Exception {
        long groupId = createTestGroup("Group", null);
        long itemId = createTestItem(groupId, "Item", "/item.mkv");

        // Delete the group (which cascades to items)
        HttpResponse<String> deleteGroupResp = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups/" + groupId))
                           .DELETE()
                           .build()
        );
        assertEquals(204, deleteGroupResp.statusCode());

        // Item should be gone
        HttpResponse<String> itemGet = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-items/" + itemId))
                           .GET()
                           .build()
        );
        assertEquals(404, itemGet.statusCode());
    }

    // ========================================================================
    // Error Mapping
    // ========================================================================

    @Test
    void unknownRoute_returns404() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/nonexistent"))
                           .GET()
                           .build()
        );

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"Not Found\""));
    }

    @Test
    void unsupportedMethod_returns405() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups"))
                           .method("PATCH", HttpRequest.BodyPublishers.ofString("{}"))
                           .build()
        );

        assertEquals(405, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"Method Not Allowed\""));
    }

    @Test
    void errorResponseShape_isConsistent() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups/99999"))
                           .GET()
                           .build()
        );

        assertEquals(404, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        assertTrue(json.containsKey("error"));
        assertTrue(json.containsKey("message"));
        assertTrue(json.containsKey("status"));
        assertEquals(404, json.get("status"));
    }

    // ========================================================================
    // Thumbnail - MediaItem
    // ========================================================================

    @Test
    void getMediaItemThumbnail_nonexistent_returns404() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-items/1"))
                           .GET()
                           .build()
        );

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"Not Found\""));
    }

    @Test
    void getMediaItemThumbnail_noThumbnail_returns404() throws Exception {
        long groupId = createTestGroup("Group", null);
        long itemId = createTestItem(groupId, "Test Item", "/test_item_does_not_exist.mkv");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-items/" + itemId))
                           .GET()
                           .build()
        );

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"Not Found\""));
    }

    @Test
    void createMediaItemThumbnail_multipart_returns201() throws Exception {
        long groupId = createTestGroup("Group", null);
        long itemId = createTestItem(groupId, "Test Item", "/test.mkv");

        // Minimal valid 1x1 PNG image
        byte[] pngBytes = java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAAAhD0kPAAAADElEQVR4nGP4z8AAAAMBAQAhD0kPAAAAAElFTkQhD0kP"
        );

        String boundary = "FormBoundary123";
        String header = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"image\"; filename=\"thumb.png\"\r\n" +
                "Content-Type: image/png\r\n\r\n";
        String trailer = "\r\n--" + boundary + "--\r\n";

        byte[] body = (header + new String(pngBytes, StandardCharsets.ISO_8859_1) + trailer).getBytes(
                StandardCharsets.ISO_8859_1);

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-items/" + itemId))
                           .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                           .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                           .build()
        );

        assertEquals(201, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        assertTrue((Boolean)json.get("success"));
        assertEquals("Thumbnail created", json.get("message"));
        assertEquals(itemId, ((Number)json.get("id")).longValue());
    }

    @Test
    void createMediaItemThumbnail_jsonBase64_returns201() throws Exception {
        long groupId = createTestGroup("Group", null);
        long itemId = createTestItem(groupId, "Test Item", "/test.mkv");

        // Simple 1x1 white JPEG image as base64
        String base64Image = "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcUFhYaHSUfGhsjHBYWICwgIyYnKSopGR8tMC0oMCUoKSj/2wBDAQcHBwoIChMKChMoGhYaKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCj/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCdABmX/9k=";

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-items/" + itemId))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"thumbnailBase64\":\"" + base64Image + "\"}"
                           ))
                           .build()
        );

        assertEquals(201, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        assertTrue((Boolean)json.get("success"));
    }

    @Test
    void createMediaItemThumbnail_jsonMissingBase64_returns400() throws Exception {
        long groupId = createTestGroup("Group", null);
        long itemId = createTestItem(groupId, "Test Item", "/test.mkv");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-items/" + itemId))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString("{\"thumbnailBase64\":\"\"}"))
                           .build()
        );

        assertEquals(400, response.statusCode());
    }

    @Test
    void createMediaItemThumbnail_nonexistentItem_returns404() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-items/99999"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString("{\"thumbnailBase64\":\"dGVzdA==\"}"))
                           .build()
        );

        assertEquals(404, response.statusCode());
    }

    @Test
    void replaceMediaItemThumbnail_returns200() throws Exception {
        long groupId = createTestGroup("Group", null);
        long itemId = createTestItem(groupId, "Test Item", "/test.mkv");

        // First create a thumbnail
        String base64Image1 = "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcUFhYaHSUfGhsjHBYWICwgIyYnKSopGR8tMC0oMCUoKSj/2wBDAQcHBwoIChMKChMoGhYaKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCj/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCdABmX/9k=";

        HttpResponse<String> createResp = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-items/" + itemId))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"thumbnailBase64\":\"" + base64Image1 + "\"}"
                           ))
                           .build()
        );
        assertEquals(201, createResp.statusCode());

        // Verify thumbnail exists via GET
        HttpResponse<String> getResp1 = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-items/" + itemId))
                           .GET()
                           .build()
        );
        assertEquals(200, getResp1.statusCode());

        // Replace with PUT (use same valid image)
        HttpResponse<String> replaceResp = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-items/" + itemId))
                           .header("Content-Type", "application/json")
                           .PUT(HttpRequest.BodyPublishers.ofString(
                                   "{\"thumbnailBase64\":\"" + base64Image1 + "\"}"
                           ))
                           .build()
        );

        assertEquals(200, replaceResp.statusCode());
        Map<String, Object> json = parseJson(replaceResp.body());
        assertTrue((Boolean)json.get("success"));
        assertEquals("Thumbnail replaced", json.get("message"));

        // Verify thumbnail still exists after replace
        HttpResponse<String> getResp2 = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-items/" + itemId))
                           .GET()
                           .build()
        );
        assertEquals(200, getResp2.statusCode());
    }

    @Test
    void deleteMediaItemThumbnail_returns204() throws Exception {
        long groupId = createTestGroup("Group", null);
        long itemId = createTestItem(groupId, "Test Item", "/test.mkv");

        // First create a thumbnail
        String base64Image = "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcUFhYaHSUfGhsjHBYWICwgIyYnKSopGR8tMC0oMCUoKSj/2wBDAQcHBwoIChMKChMoGhYaKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCj/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCdABmX/9k=";

        HttpResponse<String> createResp = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-items/" + itemId))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"thumbnailBase64\":\"" + base64Image + "\"}"
                           ))
                           .build()
        );
        assertEquals(201, createResp.statusCode());

        // Verify thumbnail exists
        HttpResponse<String> getResp1 = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-items/" + itemId))
                           .GET()
                           .build()
        );
        assertEquals(200, getResp1.statusCode());

        // Delete thumbnail
        HttpResponse<String> deleteResp = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-items/" + itemId))
                           .DELETE()
                           .build()
        );

        assertEquals(204, deleteResp.statusCode());

        // Verify thumbnail is gone
        HttpResponse<String> getResp2 = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-items/" + itemId))
                           .GET()
                           .build()
        );
        assertEquals(404, getResp2.statusCode());
    }

    @Test
    void deleteMediaItemThumbnail_nonexistentItem_returns404() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-items/99999"))
                           .DELETE()
                           .build()
        );

        assertEquals(404, response.statusCode());
    }

    // ========================================================================
    // Thumbnail - MediaGroup
    // ========================================================================

    @Test
    void getMediaGroupThumbnail_nonexistent_returns404() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-groups/1"))
                           .GET()
                           .build()
        );

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"Not Found\""));
    }

    @Test
    void getMediaGroupThumbnail_noThumbnail_returns404() throws Exception {
        long groupId = createTestGroup("Test Group", "Test desc");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-groups/" + groupId))
                           .GET()
                           .build()
        );

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"Not Found\""));
    }

    @Test
    void createMediaGroupThumbnail_multipart_returns201() throws Exception {
        long groupId = createTestGroup("Group", null);

        // Minimal valid 1x1 PNG image
        byte[] pngBytes = java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAAAhD0kPAAAADElEQVR4nGP4z8AAAAMBAQAhD0kPAAAAAElFTkQhD0kP"
        );

        String boundary = "FormBoundary456";
        String header = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"image\"; filename=\"thumb.png\"\r\n" +
                "Content-Type: image/png\r\n\r\n";
        String trailer = "\r\n--" + boundary + "--\r\n";

        byte[] body = (header + new String(pngBytes, StandardCharsets.ISO_8859_1) + trailer).getBytes(
                StandardCharsets.ISO_8859_1);

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-groups/" + groupId))
                           .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                           .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                           .build()
        );

        assertEquals(201, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        assertTrue((Boolean)json.get("success"));
        assertEquals("Thumbnail created", json.get("message"));
        assertEquals(groupId, ((Number)json.get("id")).longValue());
    }

    @Test
    void createMediaGroupThumbnail_jsonBase64_returns201() throws Exception {
        long groupId = createTestGroup("Group", null);

        String base64Image = "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcUFhYaHSUfGhsjHBYWICwgIyYnKSopGR8tMC0oMCUoKSj/2wBDAQcHBwoIChMKChMoGhYaKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCj/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCdABmX/9k=";

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-groups/" + groupId))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"thumbnailBase64\":\"" + base64Image + "\"}"
                           ))
                           .build()
        );

        assertEquals(201, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        assertTrue((Boolean)json.get("success"));
    }

    @Test
    void createMediaGroupThumbnail_nonexistentGroup_returns404() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-groups/99999"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString("{\"thumbnailBase64\":\"dGVzdA==\"}"))
                           .build()
        );

        assertEquals(404, response.statusCode());
    }

    @Test
    void replaceMediaGroupThumbnail_returns200() throws Exception {
        long groupId = createTestGroup("Group", null);

        String base64Image1 = "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcUFhYaHSUfGhsjHBYWICwgIyYnKSopGR8tMC0oMCUoKSj/2wBDAQcHBwoIChMKChMoGhYaKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCj/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCdABmX/9k=";

        HttpResponse<String> createResp = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-groups/" + groupId))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"thumbnailBase64\":\"" + base64Image1 + "\"}"
                           ))
                           .build()
        );
        assertEquals(201, createResp.statusCode());

        // Replace with PUT (use same valid image)
        HttpResponse<String> replaceResp = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-groups/" + groupId))
                           .header("Content-Type", "application/json")
                           .PUT(HttpRequest.BodyPublishers.ofString(
                                   "{\"thumbnailBase64\":\"" + base64Image1 + "\"}"
                           ))
                           .build()
        );

        assertEquals(200, replaceResp.statusCode());
        Map<String, Object> json = parseJson(replaceResp.body());
        assertTrue((Boolean)json.get("success"));
        assertEquals("Thumbnail replaced", json.get("message"));
    }

    @Test
    void deleteMediaGroupThumbnail_returns204() throws Exception {
        long groupId = createTestGroup("Group", null);

        String base64Image = "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcUFhYaHSUfGhsjHBYWICwgIyYnKSopGR8tMC0oMCUoKSj/2wBDAQcHBwoIChMKChMoGhYaKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCj/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCdABmX/9k=";

        HttpResponse<String> createResp = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-groups/" + groupId))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"thumbnailBase64\":\"" + base64Image + "\"}"
                           ))
                           .build()
        );
        assertEquals(201, createResp.statusCode());

        // Delete thumbnail
        HttpResponse<String> deleteResp = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-groups/" + groupId))
                           .DELETE()
                           .build()
        );

        assertEquals(204, deleteResp.statusCode());

        // Verify thumbnail is gone
        HttpResponse<String> getResp = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-groups/" + groupId))
                           .GET()
                           .build()
        );
        assertEquals(404, getResp.statusCode());
    }

    @Test
    void deleteMediaGroupThumbnail_nonexistentGroup_returns404() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-groups/99999"))
                           .DELETE()
                           .build()
        );

        assertEquals(404, response.statusCode());
    }

    @Test
    void thumbnailEndpoint_unsupportedMethod_returns405() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-items/1"))
                           .method("PATCH", HttpRequest.BodyPublishers.ofString("{}"))
                           .build()
        );

        assertEquals(405, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"Method Not Allowed\""));
    }

    @Test
    void thumbnailGet_returnsCorrectContentType() throws Exception {
        long groupId = createTestGroup("Group", null);
        long itemId = createTestItem(groupId, "Test Item", "/test.mkv");

        String base64Image = "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcUFhYaHSUfGhsjHBYWICwgIyYnKSopGR8tMC0oMCUoKSj/2wBDAQcHBwoIChMKChMoGhYaKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCj/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCdABmX/9k=";

        sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-items/" + itemId))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"thumbnailBase64\":\"" + base64Image + "\"}"
                           ))
                           .build()
        );

        HttpResponse<byte[]> response = sendRequestBytes(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/thumbnails/media-items/" + itemId))
                           .GET()
                           .build()
        );

        assertEquals(200, response.statusCode());
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertTrue(contentType.startsWith("image/jpeg"));
        assertTrue(response.body().length > 0);
    }

    // ========================================================================
    // Playlist - Single Media Item
    // ========================================================================

    @Test
    void playlist_singleMediaItem_returns200WithM3u() throws Exception {
        long groupId = createTestGroup("Group", null);
        long itemId = createTestItem(groupId, "Test Movie", "/test-movie.mkv");

        // Create the actual media file on disk
        Path mediaFile = tempDir.resolve("test-movie.mkv");
        Files.writeString(mediaFile, "fake video content");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/playlist/media-item/" + itemId))
                           .GET()
                           .build()
        );

        assertEquals(200, response.statusCode());
        assertEquals("audio/x-mpegurl; charset=utf-8", response.headers().firstValue("Content-Type").orElse(""));
        String body = response.body();
        assertTrue(body.startsWith("#EXTM3U"));
        assertTrue(body.contains("#EXTINF:-1,Test Movie"));
        assertTrue(body.contains("/api/stream/" + itemId));
    }

    @Test
    void playlist_singleMediaItemLocal_returns200WithM3uAndLocalFilePaths() throws Exception {
        long groupId = createTestGroup("Group", null);
        long itemId = createTestItem(groupId, "Test Movie", "/test-movie.mkv");

        // Create the actual media file on disk
        Path mediaFile = tempDir.resolve("test-movie.mkv");
        Files.writeString(mediaFile, "fake video content");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/playlist/media-item/" + itemId + "?local=true"))
                           .GET()
                           .build()
        );

        assertEquals(200, response.statusCode());
        assertEquals("audio/x-mpegurl; charset=utf-8", response.headers().firstValue("Content-Type").orElse(""));
        String body = response.body();
        assertTrue(body.startsWith("#EXTM3U"));
        assertTrue(body.contains("#EXTINF:-1,Test Movie"));
        assertTrue(body.contains(mediaFile.toAbsolutePath().toString()));
    }

    @Test
    void playlist_singleMediaItem_nonexistent_returns404() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/playlist/media-item/99999"))
                           .GET()
                           .build()
        );

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"Not Found\""));
    }

    @Test
    void playlist_singleMediaItem_missingFile_returns200WithEmptyPlaylist() throws Exception {
        long groupId = createTestGroup("Group", null);
        long itemId = createTestItem(groupId, "Ghost Movie", "/ghost.mkv");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/playlist/media-item/" + itemId))
                           .GET()
                           .build()
        );

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.startsWith("#EXTM3U"));
        assertFalse(body.contains("#EXTINF"));
    }

    @Test
    void playlist_singleMediaItem_withTitle_returns200WithTitle() throws Exception {
        long groupId = createTestGroup("Group", null);
        long itemId = createTestItem(groupId, "My Custom Title", "/custom.mkv");

        Files.writeString(tempDir.resolve("custom.mkv"), "data");

        HttpResponse<String> playlistResp = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/playlist/media-item/" + itemId))
                           .GET()
                           .build()
        );

        assertEquals(200, playlistResp.statusCode());
        assertTrue(playlistResp.body().contains("#EXTINF:-1,My Custom Title"));
    }

    @Test
    void playlist_singleMediaItem_postMethod_returns400() throws Exception {
        long groupId = createTestGroup("Group", null);
        createTestItem(groupId, "Test", "/test.mkv");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/playlist/media-item/1"))
                           .POST(HttpRequest.BodyPublishers.ofString("{}"))
                           .build()
        );

        assertEquals(400, response.statusCode());
    }

    // ========================================================================
    // Playlist - Multi Item (POST)
    // ========================================================================

    @Test
    void playlist_multiItem_returns200WithM3u() throws Exception {
        long groupId = createTestGroup("Group", null);
        long itemId1 = createTestItem(groupId, "Movie A", "/movie-a.mkv");
        long itemId2 = createTestItem(groupId, "Movie B", "/movie-b.mkv");
        long itemId3 = createTestItem(groupId, "Movie C", "/movie-c.mkv");

        Files.writeString(tempDir.resolve("movie-a.mkv"), "content a");
        Files.writeString(tempDir.resolve("movie-b.mkv"), "content b");
        Files.writeString(tempDir.resolve("movie-c.mkv"), "content c");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/playlist/media-item"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"mediaItemIds\":[" + itemId1 + "," + itemId2 + "," + itemId3 + "]}"
                           ))
                           .build()
        );

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.startsWith("#EXTM3U"));
        assertTrue(body.contains("#EXTINF:-1,Movie A"));
        assertTrue(body.contains("#EXTINF:-1,Movie B"));
        assertTrue(body.contains("#EXTINF:-1,Movie C"));
        assertTrue(body.contains("/api/stream/" + itemId1));
        assertTrue(body.contains("/api/stream/" + itemId2));
        assertTrue(body.contains("/api/stream/" + itemId3));
    }

    @Test
    void playlist_multiItem_skipsMissingFiles() throws Exception {
        long groupId = createTestGroup("Group", null);
        long itemId1 = createTestItem(groupId, "Real Movie", "/real.mkv");
        long itemId2 = createTestItem(groupId, "Ghost Movie", "/ghost.mkv");

        Files.writeString(tempDir.resolve("real.mkv"), "content");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/playlist/media-item"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"mediaItemIds\":[" + itemId1 + "," + itemId2 + "]}"
                           ))
                           .build()
        );

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.contains("#EXTINF:-1,Real Movie"));
        assertFalse(body.contains("#EXTINF:-1,Ghost Movie"));
    }

    @Test
    void playlist_multiItem_emptyArray_returns400() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/playlist/media-item"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString("{\"mediaItemIds\":[]}"))
                           .build()
        );

        assertEquals(400, response.statusCode());
    }

    @Test
    void playlist_multiItem_missingBody_returns400() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/playlist/media-item"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(""))
                           .build()
        );

        assertEquals(400, response.statusCode());
    }

    @Test
    void playlist_multiItem_getMethod_returns400() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/playlist/media-item"))
                           .GET()
                           .build()
        );

        assertEquals(400, response.statusCode());
    }

    @Test
    void playlist_multiItem_nonexistentItems_returns200WithEmptyPlaylist() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/playlist/media-item"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString("{\"mediaItemIds\":[99999,88888]}"))
                           .build()
        );

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.startsWith("#EXTM3U"));
        assertFalse(body.contains("#EXTINF"));
    }

    @Test
    void playlist_multiItem_preservesOrder() throws Exception {
        long groupId = createTestGroup("Group", null);
        long itemId1 = createTestItem(groupId, "First", "/first.mkv");
        long itemId2 = createTestItem(groupId, "Second", "/second.mkv");
        long itemId3 = createTestItem(groupId, "Third", "/third.mkv");

        Files.writeString(tempDir.resolve("first.mkv"), "a");
        Files.writeString(tempDir.resolve("second.mkv"), "b");
        Files.writeString(tempDir.resolve("third.mkv"), "c");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/playlist/media-item"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"mediaItemIds\":[" + itemId3 + "," + itemId1 + "," + itemId2 + "]}"
                           ))
                           .build()
        );

        assertEquals(200, response.statusCode());
        String body = response.body();
        int thirdIdx = body.indexOf("#EXTINF:-1,Third");
        int firstIdx = body.indexOf("#EXTINF:-1,First");
        int secondIdx = body.indexOf("#EXTINF:-1,Second");
        assertTrue(thirdIdx < firstIdx);
        assertTrue(firstIdx < secondIdx);
    }

    // ========================================================================
    // Playlist - Media Group
    // ========================================================================

    @Test
    void playlist_mediaGroup_returns200WithM3u() throws Exception {
        long groupId = createTestGroup("Group", null);
        createTestItem(groupId, "Episode 1", "/ep1.mkv");
        createTestItem(groupId, "Episode 2", "/ep2.mkv");
        createTestItem(groupId, "Episode 3", "/ep3.mkv");

        Files.writeString(tempDir.resolve("ep1.mkv"), "ep1");
        Files.writeString(tempDir.resolve("ep2.mkv"), "ep2");
        Files.writeString(tempDir.resolve("ep3.mkv"), "ep3");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/playlist/media-group/" + groupId))
                           .GET()
                           .build()
        );

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.startsWith("#EXTM3U"));
        assertTrue(body.contains("#EXTINF:-1,Episode 1"));
        assertTrue(body.contains("#EXTINF:-1,Episode 2"));
        assertTrue(body.contains("#EXTINF:-1,Episode 3"));
    }

    @Test
    void playlist_mediaGroup_skipsMissingFiles() throws Exception {
        long groupId = createTestGroup("Group", null);
        createTestItem(groupId, "Real", "/real.mkv");
        createTestItem(groupId, "Ghost", "/ghost.mkv");

        Files.writeString(tempDir.resolve("real.mkv"), "data");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/playlist/media-group/" + groupId))
                           .GET()
                           .build()
        );

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.contains("#EXTINF:-1,Real"));
        assertFalse(body.contains("#EXTINF:-1,Ghost"));
    }

    @Test
    void playlist_mediaGroup_nonexistent_returns200WithEmptyPlaylist() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/playlist/media-group/99999"))
                           .GET()
                           .build()
        );

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.startsWith("#EXTM3U"));
        assertFalse(body.contains("#EXTINF"));
    }

    @Test
    void playlist_mediaGroup_postMethod_returns405() throws Exception {
        long groupId = createTestGroup("Group", null);
        createTestItem(groupId, "Test", "/test.mkv");

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/playlist/media-group/" + groupId))
                           .POST(HttpRequest.BodyPublishers.ofString("{}"))
                           .build()
        );

        assertEquals(405, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"Method Not Allowed\""));
    }

    @Test
    void playlist_mediaGroup_emptyGroup_returns200WithEmptyPlaylist() throws Exception {
        long groupId = createTestGroup("Empty Group", null);

        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/playlist/media-group/" + groupId))
                           .GET()
                           .build()
        );

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.startsWith("#EXTM3U"));
        assertFalse(body.contains("#EXTINF"));
    }

    @Test
    void playlist_unknownRoute_returns404() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/playlist/nonexistent"))
                           .GET()
                           .build()
        );

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"Not Found\""));
    }

    @Test
    void playlist_mediaItem_invalidId_returns404() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/playlist/media-item/abc"))
                           .GET()
                           .build()
        );

        assertEquals(404, response.statusCode());
    }

    @Test
    void playlist_mediaGroup_invalidId_returns404() throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/playlist/media-group/abc"))
                           .GET()
                           .build()
        );

        assertEquals(404, response.statusCode());
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private HttpResponse<String> sendRequest(HttpRequest request) throws Exception {
        return HTTP_CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private HttpResponse<byte[]> sendRequestBytes(HttpRequest request) throws Exception {
        return HTTP_CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofByteArray()
        );
    }

    private long createTestGroup(String title, String description) throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"title\":\"" + title + "\"" +
                                           (description != null ? ",\"description\":\"" + description + "\"" : "") +
                                           "}"
                           ))
                           .build()
        );
        assertEquals(201, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        return Long.parseLong(json.get("id").toString());
    }

    private long createTestItem(long groupId, String title, String filePath) throws Exception {
        HttpResponse<String> response = sendRequest(
                HttpRequest.newBuilder()
                           .uri(URI.create(BASE_URL + "/media-groups/" + groupId + "/items"))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(
                                   "{\"title\":\"" + title + "\",\"mediaFilePath\":\"" + filePath + "\"}"
                           ))
                           .build()
        );
        assertEquals(201, response.statusCode());
        Map<String, Object> json = parseJson(response.body());
        return Long.parseLong(json.get("id").toString());
    }

    /**
     * Simple JSON parsing that returns a Map for top-level objects.
     * Uses Jackson's ObjectMapper from JsonSupport.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String body) {
        try {
            return JsonSupport.getObjectMapper().readValue(body, Map.class);
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON: " + body, e);
        }
    }
}
