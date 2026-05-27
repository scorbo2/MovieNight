package ca.corbett.movienight.service;

import ca.corbett.movienight.api.dto.MediaItemListResponse;
import ca.corbett.movienight.api.dto.MediaItemResponse;
import ca.corbett.movienight.api.dto.MediaItemUpsertRequest;
import ca.corbett.movienight.db.Database;
import ca.corbett.movienight.model.MediaItem;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic layer for MediaItem operations.
 * <p>
 * Accepts validated request DTOs and path/query params from handlers,
 * translates them into domain objects, calls the Database layer,
 * and converts results back to response DTOs.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class MediaItemService {

    private final Database database;
    private final int defaultPageSize;

    public MediaItemService(Database database, int defaultPageSize) {
        this.database = database;
        this.defaultPageSize = defaultPageSize;
    }

    /**
     * Creates a new MediaItem within the given group.
     *
     * @param groupId the parent group ID (from path, authoritative)
     * @param request the upsert request DTO
     * @return the created MediaItem as a response DTO
     * @throws SQLException if a database error occurs
     */
    public MediaItemResponse createItem(long groupId, MediaItemUpsertRequest request) throws SQLException {
        MediaItem item = new MediaItem();
        item.setMediaGroupId(groupId);
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setLastWatchedDate(request.getLastWatchedDate());
        item.setMediaFilePath(request.getMediaFilePath());
        item.setTags(request.getTags());

        database.createMediaItem(item);

        return toResponse(item);
    }

    /**
     * Lists/searches MediaItems within a specific group with pagination and filtering.
     *
     * @param groupId               the parent group ID (from path)
     * @param titleContains         title substring filter (case-insensitive)
     * @param descriptionContains   description substring filter (case-insensitive)
     * @param mediaFilePathContains media file path substring filter (case-insensitive)
     * @param tagContains           tag substring filter (case-insensitive)
     * @param pageNumber            page number (1-based)
     * @param pageSize              page size
     * @return paginated list response
     * @throws SQLException if a database error occurs
     */
    public MediaItemListResponse listItems(long groupId, String titleContains, String descriptionContains,
                                           String mediaFilePathContains, String tagContains,
                                           int pageNumber, int pageSize) throws SQLException {
        Database.MediaItemQuery query = new Database.MediaItemQuery(
                groupId,
                titleContains,
                descriptionContains,
                mediaFilePathContains,
                tagContains,
                Database.PageRequest.of(pageNumber, pageSize)
        );

        Database.PagedResult<MediaItem> result = database.listMediaItems(query);

        List<MediaItemResponse> items = result.items().stream()
                                              .map(this::toResponse)
                                              .collect(Collectors.toList());

        return new MediaItemListResponse(
                items,
                result.totalCount(),
                result.pageRequest().pageNumber(),
                result.pageRequest().pageSize()
        );
    }

    /**
     * Retrieves a MediaItem by its ID.
     *
     * @param id the item ID
     * @return the MediaItem as a response DTO
     * @throws SQLException               if a database error occurs
     * @throws Database.NotFoundException if the item does not exist
     */
    public MediaItemResponse getItemById(long id) throws SQLException, Database.NotFoundException {
        MediaItem item = database.getMediaItemById(id);
        if (item == null) {
            throw new Database.NotFoundException("No MediaItem found with id=" + id);
        }
        return toResponse(item);
    }

    /**
     * Updates a MediaItem by its ID.
     *
     * @param id      the item ID (from path, authoritative)
     * @param request the upsert request DTO
     * @return the updated MediaItem as a response DTO
     * @throws SQLException               if a database error occurs
     * @throws Database.NotFoundException if the item does not exist
     */
    public MediaItemResponse updateItem(long id, MediaItemUpsertRequest request)
            throws SQLException, Database.NotFoundException {
        MediaItem item = new MediaItem();
        item.setId(id);
        item.setMediaGroupId(request.getMediaGroupId());
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setLastWatchedDate(request.getLastWatchedDate());
        item.setMediaFilePath(request.getMediaFilePath());
        item.setTags(request.getTags());

        try {
            database.updateMediaItem(item);
        }
        catch (Database.NotFoundException e) {
            throw new Database.NotFoundException("No MediaItem found with id=" + id);
        }

        return toResponse(item);
    }

    /**
     * Deletes a MediaItem by ID.
     *
     * @param id the item ID
     * @return {@code true} if deleted, {@code false} if not found
     * @throws SQLException if a database error occurs
     */
    public boolean deleteItem(long id) throws SQLException {
        if (!database.mediaItemExists(id)) {
            return false;
        }
        database.deleteMediaItemById(id);
        return true;
    }

    /**
     * Converts a domain MediaItem object to a response DTO.
     */
    private MediaItemResponse toResponse(MediaItem item) {
        return new MediaItemResponse(
                item.getId(),
                item.getMediaGroupId(),
                item.getTitle(),
                item.getDescription(),
                item.getLastWatchedDate(),
                item.getMediaFilePath(),
                item.getTags(),
                item.isHasThumbnail()
        );
    }
}
