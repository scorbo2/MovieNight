package ca.corbett.movienight.db;

import ca.corbett.movienight.api.util.ThumbnailUtil;
import ca.corbett.movienight.config.AppConfig;
import ca.corbett.movienight.model.MediaGroup;
import ca.corbett.movienight.model.MediaItem;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A wrapper around the SQLite database connection and access logic.
 * The intention is to provide full CRUD functionality for our two model objects,
 * with the aim of standing up a REST API on top of this class.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since MovieNight 2.0
 */
public class Database {

    private static final Logger log = Logger.getLogger(Database.class.getName());

    private final Object lockObject = new Object();
    private final AppConfig appConfig;
    private final File dbFile;
    private Connection connection;

    /**
     * Used with the update methods when the record to be updated doesn't exist in the db.
     */
    public static class NotFoundException extends Exception {
        public NotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Creates a new Database instance that will use the given file for storage.
     *
     * @param appConfig The application configuration that contains our database file path. Must not be null.
     */
    public Database(AppConfig appConfig) {
        this.appConfig = appConfig;
        if (appConfig == null || appConfig.getDbFile() == null) {
            throw new IllegalArgumentException("appConfig and appConfig.dbFile cannot be null");
        }
        this.dbFile = appConfig.getDbFile().toFile();
        if (dbFile.isDirectory() || (dbFile.exists() && !dbFile.canRead())) {
            throw new IllegalArgumentException("dbFile must be a readable file: " + dbFile.getAbsolutePath());
        }
    }

    /**
     * Attempts to open a connection to our database. If the dbFile provided to the constructor
     * does not exist, a new, blank database will be created with the appropriate schema.
     *
     * @throws SQLException If something goes wrong with the database connection or initialization.
     * @throws IOException  If something goes wrong with file creation during database initialization.
     */
    public void open() throws SQLException, IOException {
        if (connection == null) {
            if (!dbFile.exists()) {
                initializeDatabase();
            }
            else {
                openDatabase();
            }
        }
    }

    /**
     * Releases our database connection.
     * Be sure to invoke this when you're done with the database to avoid resource leaks.
     */
    public void dispose() {
        try {
            if (connection != null) {
                connection.close();
                connection = null;
            }
        }
        catch (SQLException sqe) {
            log.log(Level.SEVERE, "Problem closing database connection: " + sqe.getMessage(), sqe);
        }
    }

    /**
     * Reports whether we currently have an open connection to the database.
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        }
        catch (SQLException sqe) {
            log.log(Level.SEVERE, "Problem checking database connection status: " + sqe.getMessage(), sqe);
            return false;
        }
    }

    /**
     * Returns the underlying JDBC connection.
     * <p>
     * Intended for use in tests and integration tests.
     *
     * @return the JDBC connection
     * @throws SQLException if the connection is not open
     */
    public Connection getConnection() throws SQLException {
        requireConnected();
        return connection;
    }

    /**
     * Executes the supplied database work inside a transaction. If a transaction is already active,
     * a savepoint is used so nested transactional work can still roll back safely.
     */
    public <T> T executeInTransaction(TransactionCallback<T> callback) throws SQLException {
        requireConnected();
        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }

        boolean startedTransaction = connection.getAutoCommit();
        Savepoint savepoint = null;
        if (startedTransaction) {
            connection.setAutoCommit(false);
        }
        else {
            savepoint = connection.setSavepoint();
        }

        try {
            T result = callback.execute();
            if (startedTransaction) {
                connection.commit();
            }
            else if (savepoint != null) {
                connection.releaseSavepoint(savepoint);
            }
            return result;
        }
        catch (SQLException | RuntimeException ex) {
            if (startedTransaction) {
                connection.rollback();
            }
            else if (savepoint != null) {
                connection.rollback(savepoint);
            }
            throw ex;
        }
        finally {
            if (startedTransaction) {
                connection.setAutoCommit(true);
            }
        }
    }

    /**
     * Convenience overload for transactional work that does not return a value.
     */
    public void executeInTransaction(TransactionRunnable work) throws SQLException {
        executeInTransaction(() -> {
            work.execute();
            return null;
        });
    }

    /**
     * Retrieves a MediaItem by its ID.
     *
     * @param id The ID of the MediaItem to retrieve.
     * @return The MediaItem with the specified ID, or null if not found.
     * @throws SQLException          If something goes wrong with the database operation.
     * @throws IllegalStateException If the database is not currently connected.
     */
    public MediaItem getMediaItemById(long id) throws SQLException {
        requireConnected();
        validatePositiveId(id, "id");

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, mediaGroupId, title, description, lastWatchedDate, mediaFilePath, tags FROM MediaItem WHERE id = ?"
        )) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapMediaItem(rs);
                }
            }
        }
        return null;
    }

    /**
     * Returns a list of MediaItems for the given MediaGroup id.
     *
     * @param groupId The ID of the MediaGroup whose items we want to retrieve.
     * @return A List of MediaItems in the specified MediaGroup. Will be empty if group not found or has no items.
     * @throws SQLException          If something goes wrong with the database operation.
     * @throws IllegalStateException If the database is not currently connected.
     */
    public List<MediaItem> getMediaItemsByGroupId(long groupId) throws SQLException {
        validatePositiveId(groupId, "groupId");
        return listMediaItems(new MediaItemQuery(groupId, null, null, null, null, PageRequest.unpaged())).items();
    }

    /**
     * Returns a paged list of MediaItems matching the provided filters.
     */
    public PagedResult<MediaItem> listMediaItems(MediaItemQuery query) throws SQLException {
        requireConnected();
        MediaItemQuery effectiveQuery = query == null
                ? new MediaItemQuery(null, null, null, null, null, PageRequest.unpaged())
                : query;

        QuerySpec countQuery = buildMediaItemQuery(effectiveQuery, true);
        long totalCount = executeCount(countQuery);

        QuerySpec listQuery = buildMediaItemQuery(effectiveQuery, false);
        List<MediaItem> items = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(listQuery.sql())) {
            bindParameters(ps, listQuery.parameters());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapMediaItem(rs));
                }
            }
        }
        return new PagedResult<>(items, totalCount, effectiveQuery.pageRequest());
    }

    /**
     * Returns a specific MediaGroup by its ID.
     *
     * @param id The ID of the MediaGroup to retrieve.
     * @return The MediaGroup with the specified ID, or null if not found.
     * @throws SQLException          If something goes wrong with the database operation.
     * @throws IllegalStateException If the database is not currently connected.
     */
    public MediaGroup getMediaGroupById(long id) throws SQLException {
        requireConnected();
        validatePositiveId(id, "id");

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, parentGroupId, title, description FROM MediaGroup WHERE id = ?"
        )) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapMediaGroup(rs);
                }
            }
        }
        return null;
    }

    /**
     * Returns a list of all top-level MediaGroups (i.e. those with no parent group).
     */
    public List<MediaGroup> getTopLevelMediaGroups() throws SQLException {
        return listMediaGroups(new MediaGroupQuery(null, true, null, null, PageRequest.unpaged())).items();
    }

    /**
     * Returns a paged list of MediaGroups matching the provided filters.
     */
    public PagedResult<MediaGroup> listMediaGroups(MediaGroupQuery query) throws SQLException {
        requireConnected();
        MediaGroupQuery effectiveQuery = query == null
                ? new MediaGroupQuery(null, false, null, null, PageRequest.unpaged())
                : query;

        QuerySpec countQuery = buildMediaGroupQuery(effectiveQuery, true);
        long totalCount = executeCount(countQuery);

        QuerySpec listQuery = buildMediaGroupQuery(effectiveQuery, false);
        List<MediaGroup> groups = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(listQuery.sql())) {
            bindParameters(ps, listQuery.parameters());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    groups.add(mapMediaGroup(rs));
                }
            }
        }
        return new PagedResult<>(groups, totalCount, effectiveQuery.pageRequest());
    }

    /**
     * Deletes the MediaItem with the given id from the database.
     * If no such MediaItem exists, logs a warning and does nothing.
     *
     * @param id The ID of the MediaItem to delete.
     * @throws SQLException          If something goes wrong with the database operation.
     * @throws IllegalStateException If the database is not currently connected.
     */
    public void deleteMediaItemById(long id) throws SQLException {
        requireConnected();
        validatePositiveId(id, "id");

        MediaItem existingItem = getMediaItemById(id);

        executeInTransaction(() -> {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM MediaItem WHERE id = ?")) {
                ps.setLong(1, id);
                int deleted = ps.executeUpdate();
                if (deleted == 0) {
                    log.warning("No MediaItem found with id=" + id + "; nothing to delete");
                }
                else if (existingItem != null) {
                    // Media-item thumbnails are sidecar files, so we need the original media file path.
                    ThumbnailUtil.removeMediaItemThumbnail(existingItem.getMediaFilePath(), appConfig);
                }
            }
        });
    }

    /**
     * Deletes the MediaGroup with the given id from the database, along with all of its
     * sub-groups and MediaItems via cascading foreign-key rules.
     * <p>
     * TODO: this will leave dangling thumbnails everywhere, which is ugly...
     *       we're relying on the db to cascade this delete, but our thumbnails are stored separately...
     * </p>
     *
     * @param id The ID of the MediaGroup to delete.
     * @throws SQLException          If something goes wrong with the database operation.
     * @throws IllegalStateException If the database is not currently connected.
     */
    public void deleteMediaGroupById(long id) throws SQLException {
        requireConnected();
        validatePositiveId(id, "id");

        executeInTransaction(() -> {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM MediaGroup WHERE id = ?")) {
                ps.setLong(1, id);
                int deleted = ps.executeUpdate();
                if (deleted == 0) {
                    log.warning("No MediaGroup found with id=" + id + "; nothing to delete");
                }
            }
        });
    }

    /**
     * Updates the lastWatchedDate for a MediaItem in the database. This is a lightweight operation
     * that only modifies the watched timestamp, without touching any other fields.
     *
     * @param mediaItemId     The ID of the MediaItem to update.
     * @param lastWatchedDate The new last watched date (can be null to clear it).
     * @throws SQLException If something goes wrong with the database operation.
     */
    public void updateMediaItemLastWatchedDate(int mediaItemId, LocalDate lastWatchedDate) throws SQLException {
        requireConnected();
        validatePositiveId(mediaItemId, "mediaItemId");

        executeInTransaction(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE MediaItem SET lastWatchedDate = ? WHERE id = ?"
            )) {
                setNullableString(ps, 1,
                                  lastWatchedDate == null ? null : lastWatchedDate.toString());
                ps.setInt(2, mediaItemId);
                int updated = ps.executeUpdate();
                if (updated == 0) {
                    log.warning("No MediaItem found with id=" + mediaItemId + " to update lastWatchedDate");
                }
            }
        });
    }

    /**
     * Saves a new MediaItem to the database. The given item must have an id of 0, indicating that
     * it has not yet been saved. To save changes to an existing media item, use updateMediaItem instead.
     * This method will throw an exception if the item has a non-zero id, or if any required fields are missing/invalid.
     *
     * @param item The MediaItem to save. Must have an id of 0. Will create a new entry in the db if successful.
     * @throws SQLException If something goes wrong with the database operation.
     */
    public void createMediaItem(MediaItem item) throws SQLException {
        requireConnected();
        validateMediaItem(item);

        if (item.getId() != 0) {
            throw new IllegalArgumentException(
                    "item.id must be 0 for createMediaItem; use updateMediaItem for existing items");
        }

        executeInTransaction(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO MediaItem (mediaGroupId, title, description, lastWatchedDate, mediaFilePath, tags) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            )) {
                ps.setLong(1, item.getMediaGroupId());
                ps.setString(2, item.getTitle());
                ps.setString(3, item.getDescription());
                setNullableString(ps, 4,
                                  item.getLastWatchedDate() == null ? null : item.getLastWatchedDate().toString());
                ps.setString(5, item.getMediaFilePath());
                ps.setString(6, item.getTagsAsCommaSeparatedString());
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        item.setId(keys.getLong(1));
                    }
                }
            }
        });
    }

    /**
     * Updates the given existing MediaItem in the database. The item must have a non-zero id,
     * indicating that it has already been created in the database. To create a new MediaItem,
     * use the createMediaItem method instead. This method will throw an exception if the item has an id of 0,
     * or if any required fields are missing/invalid.
     *
     * @param item The MediaItem to update in the database. Must not be null. Must have non-zero id.
     * @throws SQLException          If something goes wrong with the database operation.
     * @throws IllegalStateException If the database is not currently connected.
     * @throws NotFoundException     if the given MediaItem is not found in the db.
     */
    public void updateMediaItem(MediaItem item) throws SQLException, NotFoundException {
        requireConnected();
        validateMediaItem(item);

        if (item.getId() == 0) {
            throw new IllegalArgumentException(
                    "item.id must be non-zero for updateMediaItem; use createMediaItem for new items");
        }

        // Avoid race conditions between checking media item's existence and updating it.
        synchronized(lockObject) {
            if (!mediaItemExists(item.getId())) {
                throw new NotFoundException("No such item with id=" + item.getId());
            }

            executeInTransaction(() -> {
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE MediaItem SET mediaGroupId = ?, title = ?, description = ?, lastWatchedDate = ?, " +
                                "mediaFilePath = ?, tags = ? WHERE id = ?"
                )) {
                    ps.setLong(1, item.getMediaGroupId());
                    ps.setString(2, item.getTitle());
                    ps.setString(3, item.getDescription());
                    setNullableString(ps, 4,
                                      item.getLastWatchedDate() == null ? null : item.getLastWatchedDate().toString());
                    ps.setString(5, item.getMediaFilePath());
                    ps.setString(6, item.getTagsAsCommaSeparatedString());
                    ps.setLong(7, item.getId());
                    int updated = ps.executeUpdate();
                    if (updated == 0) {
                        log.warning("No MediaItem found with id=" + item.getId() + " to update");
                    }
                }
            });
        }
    }

    /**
     * Creates a new MediaGroup in the database. The given group must have an id of 0, indicating that
     * it has not yet been saved. To save changes to an existing media group, use saveMediaGroup instead.
     * This method will throw an exception if the group has a non-zero id, or if any required fields are
     * missing/invalid.
     *
     * @param group The MediaGroup to create. Must have an id of 0. Will create a new entry in the db if successful.
     * @throws SQLException If something goes wrong with the database operation.
     */
    public void createMediaGroup(MediaGroup group) throws SQLException {
        requireConnected();
        validateMediaGroup(group);

        if (group.getId() != 0) {
            throw new IllegalArgumentException(
                    "group.id must be 0 for createMediaGroup; use saveMediaGroup for existing groups");
        }

        executeInTransaction(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO MediaGroup (parentGroupId, title, description) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            )) {
                setNullableLong(ps, 1, group.getParentGroupId());
                ps.setString(2, group.getTitle());
                ps.setString(3, group.getDescription());
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        group.setId(keys.getLong(1));
                    }
                }
            }
        });
    }

    /**
     * Updates the given MediaGroup in the database. The given group must have a non-zero id,
     * meaning that it has already been created in the database. To create a new media group,
     * use the createMediaGroup method instead. This method will throw an exception if the group
     * has an id of 0, or if any required fields are missing/invalid, or if the parentGroupId is
     * invalid (non-existent, self-referencing, or creates a cycle).
     *
     * @param group The MediaGroup to update in the database. Must not be null. Must have nonzero id.
     * @throws SQLException          If something goes wrong with the database operation.
     * @throws IllegalStateException If the database is not currently connected.
     * @throws NotFoundException     if the given MediaGroup is not found in the db.
     */
    public void updateMediaGroup(MediaGroup group) throws SQLException, NotFoundException {
        requireConnected();
        validateMediaGroup(group);

        if (group.getId() == 0) {
            throw new IllegalArgumentException(
                    "group.id must be non-zero for saveMediaGroup; use createMediaGroup for new groups");
        }

        // Avoid race conditions between checking media group's existence and updating it.
        synchronized(lockObject) {

            if (!mediaGroupExists(group.getId())) {
                throw new NotFoundException("No such group with id=" + group.getId());
            }

            executeInTransaction(() -> {
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE MediaGroup SET parentGroupId = ?, title = ?, description = ? WHERE id = ?"
                )) {
                    setNullableLong(ps, 1, group.getParentGroupId());
                    ps.setString(2, group.getTitle());
                    ps.setString(3, group.getDescription());
                    ps.setLong(4, group.getId());
                    int updated = ps.executeUpdate();
                    if (updated == 0) {
                        log.warning("No MediaGroup found with id=" + group.getId() + " to update");
                    }
                }
            });
        }
    }

    private void requireConnected() {
        if (!isConnected()) {
            throw new IllegalStateException("Database is not connected");
        }
    }

    private void validatePositiveId(long id, String fieldName) {
        if (id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0");
        }
    }

    private void validateMediaItem(MediaItem item) throws SQLException {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        validatePositiveId(item.getMediaGroupId(), "item.mediaGroupId");

        // Avoid race condition between checking for group existence and inserting/updating the item.
        synchronized(lockObject) {
            if (!mediaGroupExists(item.getMediaGroupId())) {
                throw new IllegalArgumentException("item.mediaGroupId must reference an existing MediaGroup");
            }

            item.setTitle(normalizeRequiredString(item.getTitle(), "item.title"));
            item.setDescription(normalizeOptionalString(item.getDescription()));
            item.setMediaFilePath(normalizeRequiredMediaPath(item.getMediaFilePath(), "item.mediaFilePath"));
            item.setTags(item.getTags());
        }
    }

    private void validateMediaGroup(MediaGroup group) throws SQLException {
        if (group == null) {
            throw new IllegalArgumentException("group cannot be null");
        }

        group.setTitle(normalizeRequiredString(group.getTitle(), "group.title"));
        group.setDescription(normalizeOptionalString(group.getDescription()));

        synchronized(lockObject) {
            Long parentGroupId = group.getParentGroupId();
            if (parentGroupId != null) {
                validatePositiveId(parentGroupId, "group.parentGroupId");
                if (!mediaGroupExists(parentGroupId)) {
                    throw new IllegalArgumentException("group.parentGroupId must reference an existing MediaGroup");
                }
                if (group.getId() != 0 && parentGroupId.equals(group.getId())) {
                    throw new IllegalArgumentException("group.parentGroupId cannot reference the group itself");
                }
                if (group.getId() != 0 && wouldCreateCycle(parentGroupId, group.getId())) {
                    throw new IllegalArgumentException("group.parentGroupId cannot create a cycle");
                }
            }
        }
    }

    private String normalizeRequiredString(String value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return normalized;
    }

    private String normalizeRequiredMediaPath(String value, String fieldName) {
        // Start with same validation as any other required string:
        String normalized = normalizeRequiredString(value, fieldName);

        // But additionally, we will trim any leading file separators, as this is a relative path:
        if (normalized.startsWith(File.separator) && normalized.length() > 1) {
            normalized = normalized.substring(1);
        }

        return normalized;
    }

    private String normalizeOptionalString(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public boolean mediaGroupExists(long id) throws SQLException {
        requireConnected();
        try (PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM MediaGroup WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean mediaItemExists(long id) throws SQLException {
        requireConnected();
        try (PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM MediaItem WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean wouldCreateCycle(long proposedParentId, long groupId) throws SQLException {
        Long currentParentId = proposedParentId;
        while (currentParentId != null) {
            if (currentParentId == groupId) {
                return true;
            }
            currentParentId = getParentGroupId(currentParentId);
        }
        return false;
    }

    private Long getParentGroupId(long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT parentGroupId FROM MediaGroup WHERE id = ?"
        )) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                long parentGroupId = rs.getLong(1);
                return rs.wasNull() ? null : parentGroupId;
            }
        }
    }

    private QuerySpec buildMediaGroupQuery(MediaGroupQuery query, boolean countOnly) {
        StringBuilder sql = new StringBuilder(
                countOnly
                        ? "SELECT COUNT(*) FROM MediaGroup"
                        : "SELECT id, parentGroupId, title, description FROM MediaGroup"
        );

        List<String> conditions = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();

        if (query.topLevelOnly()) {
            conditions.add("parentGroupId IS NULL");
        }
        else if (query.parentGroupId() != null) {
            conditions.add("parentGroupId = ?");
            parameters.add(query.parentGroupId());
        }

        addContainsCondition(conditions, parameters, "title", query.titleContains());
        addContainsCondition(conditions, parameters, "COALESCE(description, '')", query.descriptionContains());

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        if (!countOnly) {
            sql.append(" ORDER BY id");
            appendPagination(sql, parameters, query.pageRequest());
        }

        return new QuerySpec(sql.toString(), parameters);
    }

    private QuerySpec buildMediaItemQuery(MediaItemQuery query, boolean countOnly) {
        StringBuilder sql = new StringBuilder(
                countOnly
                        ? "SELECT COUNT(*) FROM MediaItem"
                        : "SELECT id, mediaGroupId, title, description, lastWatchedDate, mediaFilePath, tags FROM MediaItem"
        );

        List<String> conditions = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();

        if (query.mediaGroupId() != null) {
            conditions.add("mediaGroupId = ?");
            parameters.add(query.mediaGroupId());
        }

        addContainsCondition(conditions, parameters, "title", query.titleContains());
        addContainsCondition(conditions, parameters, "COALESCE(description, '')", query.descriptionContains());
        addContainsCondition(conditions, parameters, "mediaFilePath", query.mediaFilePathContains());
        addContainsCondition(conditions, parameters, "COALESCE(tags, '')", query.tagContains());

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        if (!countOnly) {
            sql.append(" ORDER BY id");
            appendPagination(sql, parameters, query.pageRequest());
        }

        return new QuerySpec(sql.toString(), parameters);
    }

    private void addContainsCondition(List<String> conditions, List<Object> parameters, String field, String value) {
        String normalized = normalizeFilterString(value);
        if (normalized != null) {
            conditions.add("LOWER(" + field + ") LIKE ?");
            parameters.add("%" + normalized + "%");
        }
    }

    private String normalizeFilterString(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private void appendPagination(StringBuilder sql, List<Object> parameters, PageRequest pageRequest) {
        if (!pageRequest.isPaged()) {
            return;
        }

        sql.append(" LIMIT ? OFFSET ?");
        parameters.add(pageRequest.pageSize());
        parameters.add(pageRequest.offset());
    }

    private long executeCount(QuerySpec querySpec) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(querySpec.sql())) {
            bindParameters(ps, querySpec.parameters());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private void bindParameters(PreparedStatement ps, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            Object parameter = parameters.get(i);
            int index = i + 1;
            if (parameter instanceof String value) {
                ps.setString(index, value);
            }
            else if (parameter instanceof Long value) {
                ps.setLong(index, value);
            }
            else if (parameter instanceof Integer value) {
                ps.setInt(index, value);
            }
            else {
                throw new IllegalArgumentException(
                        "Unsupported query parameter type: " + parameter.getClass().getName());
            }
        }
    }

    private void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.VARCHAR);
        }
        else {
            ps.setString(index, value);
        }
    }

    private void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.BIGINT);
        }
        else {
            ps.setLong(index, value);
        }
    }

    private MediaItem mapMediaItem(ResultSet rs) throws SQLException {
        MediaItem item = new MediaItem();
        item.setId(rs.getLong("id"));
        item.setMediaGroupId(rs.getLong("mediaGroupId"));
        item.setTitle(rs.getString("title"));
        item.setDescription(rs.getString("description"));

        String lastWatchedDate = rs.getString("lastWatchedDate");
        if (lastWatchedDate != null && !lastWatchedDate.isBlank()) {
            item.setLastWatchedDate(LocalDate.parse(lastWatchedDate));
        }

        item.setMediaFilePath(rs.getString("mediaFilePath"));

        item.setHasThumbnail(ThumbnailUtil.hasThumbnail(item, appConfig));

        item.setRecentlyWatched(MediaItem.calculateRecentlyWatched(item.getLastWatchedDate(), appConfig));

        String tags = rs.getString("tags");
        if (tags == null || tags.isBlank()) {
            item.setTags(List.of());
        }
        else {
            item.setTags(List.of(tags.split(",")));
        }
        return item;
    }

    private MediaGroup mapMediaGroup(ResultSet rs) throws SQLException {
        MediaGroup group = new MediaGroup();
        group.setId(rs.getLong("id"));

        long parentGroupId = rs.getLong("parentGroupId");
        group.setParentGroupId(rs.wasNull() ? null : parentGroupId);

        group.setHasThumbnail(ThumbnailUtil.hasThumbnail(group, appConfig));

        group.setTitle(rs.getString("title"));
        group.setDescription(rs.getString("description"));
        return group;
    }

    /**
     * Creates a new, blank database with the appropriate schema.
     * Assumes that our dbFile does not exist - will overwrite if it does.
     */
    private void initializeDatabase() throws SQLException, IOException {
        if (dbFile.exists()) {
            log.warning("Database file already exists at " + dbFile.getAbsolutePath() + " - overwriting");
            if (!dbFile.delete()) {
                throw new IOException("Unable to delete existing database file at " + dbFile.getAbsolutePath());
            }
        }
        if (!dbFile.createNewFile()) {
            throw new IOException("Unable to create new database file at " + dbFile.getAbsolutePath());
        }
        openDatabase();

        try (var stmt = connection.createStatement()) {
            stmt.execute("""
                                     CREATE TABLE IF NOT EXISTS MediaGroup (
                                         id INTEGER PRIMARY KEY AUTOINCREMENT,
                                         parentGroupId INTEGER REFERENCES MediaGroup(id) ON DELETE CASCADE,
                                         title TEXT NOT NULL CHECK (length(trim(title)) > 0),
                                         description TEXT
                                     );
                                 """);
        }

        try (var stmt = connection.createStatement()) {
            stmt.execute("""
                                     CREATE TABLE IF NOT EXISTS MediaItem (
                                         id INTEGER PRIMARY KEY AUTOINCREMENT,
                                         mediaGroupId INTEGER NOT NULL,
                                         title TEXT NOT NULL CHECK (length(trim(title)) > 0),
                                         description TEXT,
                                         lastWatchedDate TEXT,
                                         mediaFilePath TEXT NOT NULL CHECK (length(trim(mediaFilePath)) > 0),
                                         tags TEXT,
                                         FOREIGN KEY (mediaGroupId) REFERENCES MediaGroup(id) ON DELETE CASCADE
                                     );
                                 """);
        }

        try (var stmt = connection.createStatement()) {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_media_group_parent ON MediaGroup(parentGroupId);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_media_item_group ON MediaItem(mediaGroupId);");
        }
    }

    /**
     * Opens a connection to the database and ensures that foreign key support is enabled.
     * (for some reason, this is disabled by default in SQLite)
     */
    private void openDatabase() throws SQLException {
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        connection = DriverManager.getConnection(url);

        try (var stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }

        log.info("Connected to database at " + dbFile.getAbsolutePath());
    }

    @FunctionalInterface
    public interface TransactionCallback<T> {
        T execute() throws SQLException;
    }

    @FunctionalInterface
    public interface TransactionRunnable {
        void execute() throws SQLException;
    }

    public record MediaGroupQuery(
            Long parentGroupId,
            boolean topLevelOnly,
            String titleContains,
            String descriptionContains,
            PageRequest pageRequest
    ) {
        public MediaGroupQuery {
            if (topLevelOnly && parentGroupId != null) {
                throw new IllegalArgumentException("topLevelOnly cannot be combined with parentGroupId");
            }
            pageRequest = pageRequest == null ? PageRequest.unpaged() : pageRequest;
        }
    }

    public record MediaItemQuery(
            Long mediaGroupId,
            String titleContains,
            String descriptionContains,
            String mediaFilePathContains,
            String tagContains,
            PageRequest pageRequest
    ) {
        public MediaItemQuery {
            if (mediaGroupId != null && mediaGroupId <= 0) {
                throw new IllegalArgumentException("mediaGroupId must be greater than 0");
            }
            pageRequest = pageRequest == null ? PageRequest.unpaged() : pageRequest;
        }
    }

    public record PagedResult<T>(List<T> items, long totalCount, PageRequest pageRequest) {
        public PagedResult {
            items = List.copyOf(items);
            pageRequest = pageRequest == null ? PageRequest.unpaged() : pageRequest;
        }
    }

    public static final class PageRequest {
        private static final PageRequest UNPAGED = new PageRequest(1, Integer.MAX_VALUE, false);

        private final int pageNumber;
        private final int pageSize;
        private final boolean paged;

        private PageRequest(int pageNumber, int pageSize, boolean paged) {
            this.pageNumber = pageNumber;
            this.pageSize = pageSize;
            this.paged = paged;
        }

        public static PageRequest of(int pageNumber, int pageSize) {
            if (pageNumber <= 0) {
                throw new IllegalArgumentException("pageNumber must be greater than 0");
            }
            if (pageSize <= 0) {
                throw new IllegalArgumentException("pageSize must be greater than 0");
            }
            return new PageRequest(pageNumber, pageSize, true);
        }

        public static PageRequest unpaged() {
            return UNPAGED;
        }

        public int pageNumber() {
            return pageNumber;
        }

        public int pageSize() {
            return pageSize;
        }

        public boolean isPaged() {
            return paged;
        }

        public int offset() {
            return paged ? Math.multiplyExact(pageNumber - 1, pageSize) : 0;
        }
    }

    private record QuerySpec(String sql, List<Object> parameters) {
    }
}
