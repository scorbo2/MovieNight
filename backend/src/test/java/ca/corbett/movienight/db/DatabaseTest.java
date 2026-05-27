package ca.corbett.movienight.db;

import ca.corbett.movienight.config.AppConfig;
import ca.corbett.movienight.model.MediaGroup;
import ca.corbett.movienight.model.MediaItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseTest {

    @TempDir
    private Path tempDir;

    private Database db;

    @BeforeEach
    void setUp() throws SQLException, IOException {
        db = new Database(AppConfig.withDataDir(tempDir));
        db.open();
    }

    @AfterEach
    void tearDown() {
        db.dispose();
    }

    @Test
    void constructor_withNullAppConfig_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new Database(null));
    }

    @Test
    void createMediaGroup_newGroup_shouldAssignGeneratedId() throws SQLException {
        MediaGroup group = buildGroup(null, "Movies", null);
        assertEquals(0, group.getId());

        db.createMediaGroup(group);

        assertTrue(group.getId() > 0);
    }

    @Test
    void createMediaGroup_newGroup_shouldBeRetrievableById() throws SQLException {
        MediaGroup group = buildGroup(null, "TV Shows", "All my TV shows");
        db.createMediaGroup(group);

        MediaGroup fetched = db.getMediaGroupById(group.getId());

        assertNotNull(fetched);
        assertEquals(group.getId(), fetched.getId());
        assertEquals("TV Shows", fetched.getTitle());
        assertEquals("All my TV shows", fetched.getDescription());
        assertNull(fetched.getParentGroupId());
    }

    @Test
    void createMediaGroup_nestedGroup_shouldPreserveParentLink() throws SQLException {
        MediaGroup parent = buildGroup(null, "TV Shows", null);
        db.createMediaGroup(parent);

        MediaGroup child = buildGroup(parent.getId(), "Dexter", "Crime drama");
        db.createMediaGroup(child);

        MediaGroup fetched = db.getMediaGroupById(child.getId());

        assertNotNull(fetched);
        assertEquals(parent.getId(), fetched.getParentGroupId());
    }

    @Test
    void updateMediaGroup_existingGroup_shouldUpdateFields() throws SQLException, Database.NotFoundException {
        MediaGroup group = buildGroup(null, "Old Title", "Old desc");
        db.createMediaGroup(group);

        group.setTitle("New Title");
        group.setDescription("New desc");
        db.updateMediaGroup(group);

        MediaGroup fetched = db.getMediaGroupById(group.getId());
        assertNotNull(fetched);
        assertEquals("New Title", fetched.getTitle());
        assertEquals("New desc", fetched.getDescription());
    }

    @Test
    void updateMediaGroup_withInvalidId_shouldThrowNotFoundException() throws SQLException {
        MediaGroup group = buildGroup(null, "Nonexistent Group", null);
        group.setId(9999L); // some ID that doesn't exist in the database

        assertThrows(Database.NotFoundException.class, () -> db.updateMediaGroup(group));
    }

    @Test
    void updateMediaGroup_newGroup_shouldThrowIllegalArgumentException() {
        // updateMediaGroup should reject brand-new groups - use createMediaGroup for that
        MediaGroup group = buildGroup(null, "New Group", null);
        assertThrows(IllegalArgumentException.class, () -> db.updateMediaGroup(group));
    }

    @Test
    void createMediaGroup_withBlankTitle_shouldThrowIllegalArgumentException() {
        MediaGroup group = buildGroup(null, "   ", null);

        assertThrows(IllegalArgumentException.class, () -> db.createMediaGroup(group));
    }

    @Test
    void createMediaGroup_withBlankDescription_shouldNormalizeToNull() throws SQLException {
        MediaGroup group = buildGroup(null, "Movies", "   ");

        db.createMediaGroup(group);

        MediaGroup fetched = db.getMediaGroupById(group.getId());
        assertNotNull(fetched);
        assertNull(fetched.getDescription());
    }

    @Test
    void updateMediaGroup_withCycle_shouldThrowIllegalArgumentException() throws SQLException {
        MediaGroup parent = buildGroup(null, "Parent", null);
        db.createMediaGroup(parent);

        MediaGroup child = buildGroup(parent.getId(), "Child", null);
        db.createMediaGroup(child);

        parent.setParentGroupId(child.getId());

        assertThrows(IllegalArgumentException.class, () -> db.updateMediaGroup(parent));
    }

    @Test
    void getTopLevelMediaGroups_withNoGroups_shouldReturnEmptyList() throws SQLException {
        List<MediaGroup> groups = db.getTopLevelMediaGroups();

        assertNotNull(groups);
        assertTrue(groups.isEmpty());
    }

    @Test
    void getTopLevelMediaGroups_shouldReturnOnlyTopLevelGroups() throws SQLException {
        MediaGroup parent1 = buildGroup(null, "Movies", null);
        MediaGroup parent2 = buildGroup(null, "TV Shows", null);
        db.createMediaGroup(parent1);
        db.createMediaGroup(parent2);

        MediaGroup child = buildGroup(parent1.getId(), "Action Movies", null);
        db.createMediaGroup(child);

        List<MediaGroup> topLevel = db.getTopLevelMediaGroups();

        assertEquals(2, topLevel.size());
        assertTrue(topLevel.stream().allMatch(g -> g.getParentGroupId() == null));
    }

    @Test
    void listMediaGroups_shouldSupportFilteringAndPagination() throws SQLException {
        MediaGroup action = buildGroup(null, "Action Movies", "Explosions");
        MediaGroup comedy = buildGroup(null, "Comedy", "Laughs");
        MediaGroup documentary = buildGroup(null, "Documentaries", "Learning");
        db.createMediaGroup(action);
        db.createMediaGroup(comedy);
        db.createMediaGroup(documentary);

        Database.PagedResult<MediaGroup> firstPage = db.listMediaGroups(
                new Database.MediaGroupQuery(null, true, null, null, Database.PageRequest.of(1, 2))
        );
        Database.PagedResult<MediaGroup> filtered = db.listMediaGroups(
                new Database.MediaGroupQuery(null, false, "doc", "learn", Database.PageRequest.of(1, 10))
        );

        assertEquals(2, firstPage.items().size());
        assertEquals(3, firstPage.totalCount());
        assertEquals(1, filtered.items().size());
        assertEquals("Documentaries", filtered.items().getFirst().getTitle());
    }

    @Test
    void deleteMediaGroupById_nonExistentGroup_shouldLogWarningAndNotThrow() {
        assertDoesNotThrow(() -> db.deleteMediaGroupById(9999L));
    }

    @Test
    void deleteMediaGroupById_shouldRemoveGroup() throws SQLException {
        MediaGroup group = buildGroup(null, "To Delete", null);
        db.createMediaGroup(group);
        long id = group.getId();

        db.deleteMediaGroupById(id);

        assertNull(db.getMediaGroupById(id));
    }

    @Test
    void deleteMediaGroupById_shouldRecursivelyRemoveSubGroupsAndItems() throws SQLException {
        MediaGroup parent = buildGroup(null, "Parent", null);
        db.createMediaGroup(parent);

        MediaGroup child = buildGroup(parent.getId(), "Child", null);
        db.createMediaGroup(child);

        MediaItem item = buildItem(child.getId(), "Ep1", "/files/ep1.mkv");
        db.createMediaItem(item);

        db.deleteMediaGroupById(parent.getId());

        assertNull(db.getMediaGroupById(parent.getId()));
        assertNull(db.getMediaGroupById(child.getId()));
        assertNull(db.getMediaItemById(item.getId()));
    }

    @Test
    void executeInTransaction_shouldRollbackAllWorkWhenStepFails() throws SQLException {
        MediaGroup valid = buildGroup(null, "Valid", null);
        MediaGroup invalid = buildGroup(null, "   ", null);

        assertThrows(IllegalArgumentException.class, () -> db.executeInTransaction(() -> {
            db.createMediaGroup(valid);
            db.createMediaGroup(invalid);
        }));

        assertTrue(valid.getId() > 0);
        assertNull(db.getMediaGroupById(valid.getId()));
        assertTrue(db.getTopLevelMediaGroups().isEmpty());
    }

    @Test
    void createMediaItem_newItem_shouldAssignGeneratedId() throws SQLException {
        MediaGroup group = buildGroup(null, "Movies", null);
        db.createMediaGroup(group);

        MediaItem item = buildItem(group.getId(), "Inception", "/files/inception.mkv");
        assertEquals(0, item.getId());

        db.createMediaItem(item);

        assertTrue(item.getId() > 0);
    }

    @Test
    void createMediaItem_newItem_shouldBeRetrievableById() throws SQLException {
        MediaGroup group = buildGroup(null, "Movies", null);
        db.createMediaGroup(group);

        MediaItem item = buildItem(group.getId(), "Inception", "/files/inception.mkv");
        item.setDescription("A dream within a dream");
        item.setLastWatchedDate(LocalDate.of(2024, 6, 15));
        item.setTags(List.of("sci-fi", "thriller"));
        db.createMediaItem(item);

        MediaItem fetched = db.getMediaItemById(item.getId());

        assertNotNull(fetched);
        assertEquals(item.getId(), fetched.getId());
        assertEquals("Inception", fetched.getTitle());
        assertEquals("A dream within a dream", fetched.getDescription());
        assertEquals("/files/inception.mkv", fetched.getMediaFilePath());
        assertEquals(LocalDate.of(2024, 6, 15), fetched.getLastWatchedDate());
        assertTrue(fetched.getTags().contains("sci-fi"));
        assertTrue(fetched.getTags().contains("thriller"));
    }

    @Test
    void createMediaItem_withNullLastWatchedDate_shouldRoundTrip() throws SQLException {
        MediaGroup group = buildGroup(null, "Movies", null);
        db.createMediaGroup(group);

        MediaItem item = buildItem(group.getId(), "Unwatched Movie", "/files/movie.mkv");
        db.createMediaItem(item);

        MediaItem fetched = db.getMediaItemById(item.getId());
        assertNotNull(fetched);
        assertNull(fetched.getLastWatchedDate());
    }

    @Test
    void createMediaItem_withNoTags_shouldReturnEmptyTagList() throws SQLException {
        MediaGroup group = buildGroup(null, "Movies", null);
        db.createMediaGroup(group);

        MediaItem item = buildItem(group.getId(), "No Tags Movie", "/files/movie.mkv");
        db.createMediaItem(item);

        MediaItem fetched = db.getMediaItemById(item.getId());
        assertNotNull(fetched);
        assertTrue(fetched.getTags().isEmpty());
    }

    @Test
    void createMediaItem_withBlankTitle_shouldThrowIllegalArgumentException() throws SQLException {
        MediaGroup group = buildGroup(null, "Movies", null);
        db.createMediaGroup(group);

        MediaItem item = buildItem(group.getId(), "   ", "/files/movie.mkv");

        assertThrows(IllegalArgumentException.class, () -> db.createMediaItem(item));
    }

    @Test
    void createMediaItem_withBlankMediaPath_shouldThrowIllegalArgumentException() throws SQLException {
        MediaGroup group = buildGroup(null, "Movies", null);
        db.createMediaGroup(group);

        MediaItem item = buildItem(group.getId(), "Movie", "   ");

        assertThrows(IllegalArgumentException.class, () -> db.createMediaItem(item));
    }

    @Test
    void createMediaItem_withBlankDescription_shouldNormalizeToNull() throws SQLException {
        MediaGroup group = buildGroup(null, "Movies", null);
        db.createMediaGroup(group);

        MediaItem item = buildItem(group.getId(), "Movie", "/files/movie.mkv");
        item.setDescription("   ");
        db.createMediaItem(item);

        MediaItem fetched = db.getMediaItemById(item.getId());
        assertNotNull(fetched);
        assertNull(fetched.getDescription());
    }

    @Test
    void updateMediaItem_withNewItem_shouldThrowIllegalArgumentException() {
        // updateMediaItem should reject brand-new items - use createMediaItem for that
        MediaItem item = buildItem(1L, "New Item", "/path/to/file.mkv");
        assertThrows(IllegalArgumentException.class, () -> db.updateMediaItem(item));
    }

    @Test
    void updateMediaItem_withInvalidId_shouldThrowNotFoundException() throws SQLException {
        MediaGroup validGroup = buildGroup(null, "Valid Group", null);
        db.createMediaGroup(validGroup);
        MediaItem item = buildItem(validGroup.getId(), "Nonexistent Item", "/path/to/file.mkv");
        item.setId(9999L); // some ID that doesn't exist in the database

        assertThrows(Database.NotFoundException.class, () -> db.updateMediaItem(item));
    }

    @Test
    void getMediaItemsByGroupId_withNoItems_shouldReturnEmptyList() throws SQLException {
        MediaGroup group = buildGroup(null, "Empty Group", null);
        db.createMediaGroup(group);

        List<MediaItem> items = db.getMediaItemsByGroupId(group.getId());
        assertNotNull(items);
        assertTrue(items.isEmpty());
    }

    @Test
    void getMediaItemsByGroupId_shouldReturnOnlyItemsForThatGroup() throws SQLException {
        MediaGroup g1 = buildGroup(null, "Group 1", null);
        MediaGroup g2 = buildGroup(null, "Group 2", null);
        db.createMediaGroup(g1);
        db.createMediaGroup(g2);

        MediaItem i1 = buildItem(g1.getId(), "Item A", "/a.mkv");
        MediaItem i2 = buildItem(g1.getId(), "Item B", "/b.mkv");
        MediaItem i3 = buildItem(g2.getId(), "Item C", "/c.mkv");
        db.createMediaItem(i1);
        db.createMediaItem(i2);
        db.createMediaItem(i3);

        List<MediaItem> g1Items = db.getMediaItemsByGroupId(g1.getId());
        assertEquals(2, g1Items.size());
        assertTrue(g1Items.stream().allMatch(i -> i.getMediaGroupId() == g1.getId()));
    }

    @Test
    void listMediaItems_shouldSupportFilteringAndPagination() throws SQLException {
        MediaGroup group = buildGroup(null, "Movies", null);
        db.createMediaGroup(group);

        MediaItem first = buildItem(group.getId(), "Arrival", "/media/arrival.mkv");
        first.setDescription("First contact");
        first.setTags(List.of("Sci-Fi", "Drama"));
        MediaItem second = buildItem(group.getId(), "Dream Heist", "/media/dream-heist.mkv");
        second.setDescription("A thrilling heist inside dreams");
        second.setTags(List.of("Thriller", "Sci-Fi"));
        MediaItem third = buildItem(group.getId(), "Comedy Night", "/media/comedy.mkv");
        third.setDescription("Stand-up special");
        third.setTags(List.of("Comedy"));
        db.createMediaItem(first);
        db.createMediaItem(second);
        db.createMediaItem(third);

        Database.PagedResult<MediaItem> firstPage = db.listMediaItems(
                new Database.MediaItemQuery(group.getId(), null, null, null, null, Database.PageRequest.of(1, 2))
        );
        Database.PagedResult<MediaItem> filtered = db.listMediaItems(
                new Database.MediaItemQuery(group.getId(), "dream", "heist", "dream", "thriller",
                                            Database.PageRequest.of(1, 10))
        );

        assertEquals(2, firstPage.items().size());
        assertEquals(3, firstPage.totalCount());
        assertEquals(1, filtered.items().size());
        assertEquals("Dream Heist", filtered.items().getFirst().getTitle());
    }

    @Test
    void findMediaItemByTag_withNoMatchingItems_shouldReturnEmptyList() throws SQLException {
        MediaGroup group = buildGroup(null, "Movies", null);
        db.createMediaGroup(group);

        Database.MediaItemQuery query = new Database.MediaItemQuery(group.getId(), null, null, null, "nonexistent",
                                                                    Database.PageRequest.of(1, 10));
        Database.PagedResult<MediaItem> items = db.listMediaItems(query);
        assertNotNull(items);
        assertTrue(items.items().isEmpty());
    }

    @Test
    void findMediaItemByTag_shouldReturnOnlyItemsWithThatTag() throws SQLException, Database.NotFoundException {
        MediaGroup group = buildGroup(null, "Movies", null);
        db.createMediaGroup(group);

        MediaItem i1 = buildItem(group.getId(), "Sci-Fi Movie", "/sci-fi.mkv");
        i1.setTags(List.of("sci-fi"));
        MediaItem i2 = buildItem(group.getId(), "Comedy Movie", "/comedy.mkv");
        i2.setTags(List.of("comedy"));
        db.createMediaItem(i1);
        db.createMediaItem(i2);

        Database.MediaItemQuery query = new Database.MediaItemQuery(group.getId(), null, null, null, "sci-fi",
                                                                    Database.PageRequest.of(1, 10));
        Database.PagedResult<MediaItem> items = db.listMediaItems(query);

        assertEquals(1, items.items().size());
        assertEquals("Sci-Fi Movie", items.items().getFirst().getTitle());

        i2.addTag("SCI-FI"); // test case-insensitivity and deduplication
        db.updateMediaItem(i2);

        // second item should now show up in the same query
        items = db.listMediaItems(query);
        assertEquals(2, items.items().size());
    }

    @Test
    void createMediaItem_withExistingItem_shouldThrowIllegalArgumentException() throws SQLException {
        MediaGroup group = buildGroup(null, "Movies", null);
        db.createMediaGroup(group);

        MediaItem item = buildItem(group.getId(), "Inception", "/files/inception.mkv");
        db.createMediaItem(item);

        // createMediaItem should reject items that already have an ID - use updateMediaItem for that
        item.setDescription("new description");
        assertThrows(IllegalArgumentException.class, () -> db.createMediaItem(item));
    }

    @Test
    void updateMediaItem_existingItem_shouldUpdateFields() throws SQLException, Database.NotFoundException {
        MediaGroup group = buildGroup(null, "Movies", null);
        db.createMediaGroup(group);

        MediaItem item = buildItem(group.getId(), "Original Title", "/old.mkv");
        db.createMediaItem(item);

        item.setTitle("Updated Title");
        item.setMediaFilePath("/new.mkv");
        item.setLastWatchedDate(LocalDate.of(2026, 1, 1));
        db.updateMediaItem(item);

        MediaItem fetched = db.getMediaItemById(item.getId());
        assertNotNull(fetched);
        assertEquals("Updated Title", fetched.getTitle());
        assertEquals("/new.mkv", fetched.getMediaFilePath());
        assertEquals(LocalDate.of(2026, 1, 1), fetched.getLastWatchedDate());
    }

    @Test
    void deleteMediaItemById_nonExistentItem_shouldLogWarningAndNotThrow() {
        assertDoesNotThrow(() -> db.deleteMediaItemById(9999L));
    }

    @Test
    void deleteMediaItemById_shouldRemoveItem() throws SQLException {
        MediaGroup group = buildGroup(null, "Movies", null);
        db.createMediaGroup(group);

        MediaItem item = buildItem(group.getId(), "To Delete", "/del.mkv");
        db.createMediaItem(item);
        long id = item.getId();

        db.deleteMediaItemById(id);

        assertNull(db.getMediaItemById(id));
    }

    private MediaGroup buildGroup(Long parentId, String title, String description) {
        MediaGroup group = new MediaGroup();
        group.setParentGroupId(parentId);
        group.setTitle(title);
        group.setDescription(description);
        return group;
    }

    private MediaItem buildItem(long groupId, String title, String filePath) {
        MediaItem item = new MediaItem();
        item.setMediaGroupId(groupId);
        item.setTitle(title);
        item.setMediaFilePath(filePath);
        return item;
    }
}
