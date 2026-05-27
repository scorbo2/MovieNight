package ca.corbett.movienight.service;

import ca.corbett.movienight.api.dto.MediaGroupListResponse;
import ca.corbett.movienight.api.dto.MediaGroupResponse;
import ca.corbett.movienight.api.dto.MediaGroupUpsertRequest;
import ca.corbett.movienight.db.Database;
import ca.corbett.movienight.model.MediaGroup;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic layer for MediaGroup operations.
 * <p>
 * Accepts validated request DTOs and path/query params from handlers,
 * translates them into domain objects, calls the Database layer,
 * and converts results back to response DTOs.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class MediaGroupService {

    private final Database database;
    private final int defaultPageSize;

    public MediaGroupService(Database database, int defaultPageSize) {
        this.database = database;
        this.defaultPageSize = defaultPageSize;
    }

    /**
     * Creates a new MediaGroup from the request DTO.
     *
     * @param request the upsert request DTO
     * @return the created MediaGroup as a response DTO
     * @throws SQLException if a database error occurs
     */
    public MediaGroupResponse createGroup(MediaGroupUpsertRequest request) throws SQLException {
        MediaGroup group = new MediaGroup();
        group.setParentGroupId(request.getParentGroupId());
        group.setTitle(request.getTitle());
        group.setDescription(request.getDescription());

        database.createMediaGroup(group);

        return toResponse(group);
    }

    /**
     * Retrieves a MediaGroup by its ID.
     *
     * @param id the group ID
     * @return the MediaGroup as a response DTO
     * @throws SQLException               if a database error occurs
     * @throws Database.NotFoundException if the group does not exist
     */
    public MediaGroupResponse getGroupById(long id) throws SQLException, Database.NotFoundException {
        MediaGroup group = database.getMediaGroupById(id);
        if (group == null) {
            throw new Database.NotFoundException("No MediaGroup found with id=" + id);
        }
        return toResponse(group);
    }

    /**
     * Lists/searches MediaGroups with pagination and filtering.
     *
     * @param parentGroupId       optional parent group filter
     * @param topLevelOnly        whether to return only top-level groups
     * @param titleContains       title substring filter (case-insensitive)
     * @param descriptionContains description substring filter (case-insensitive)
     * @param pageNumber          page number (1-based)
     * @param pageSize            page size
     * @return paginated list response
     * @throws SQLException if a database error occurs
     */
    public MediaGroupListResponse listGroups(Long parentGroupId, boolean topLevelOnly,
                                             String titleContains, String descriptionContains,
                                             int pageNumber, int pageSize) throws SQLException {
        Database.MediaGroupQuery query = new Database.MediaGroupQuery(
                parentGroupId,
                topLevelOnly,
                titleContains,
                descriptionContains,
                Database.PageRequest.of(pageNumber, pageSize)
        );

        Database.PagedResult<MediaGroup> result = database.listMediaGroups(query);

        List<MediaGroupResponse> items = result.items().stream()
                                               .map(this::toResponse)
                                               .collect(Collectors.toList());

        return new MediaGroupListResponse(
                items,
                result.totalCount(),
                result.pageRequest().pageNumber(),
                result.pageRequest().pageSize()
        );
    }

    /**
     * Updates an existing MediaGroup.
     *
     * @param id      the group ID (from path, authoritative)
     * @param request the upsert request DTO
     * @return the updated MediaGroup as a response DTO
     * @throws SQLException               if a database error occurs
     * @throws Database.NotFoundException if the group does not exist
     */
    public MediaGroupResponse updateGroup(long id, MediaGroupUpsertRequest request)
            throws SQLException, Database.NotFoundException {
        MediaGroup group = new MediaGroup();
        group.setId(id);
        group.setParentGroupId(request.getParentGroupId());
        group.setTitle(request.getTitle());
        group.setDescription(request.getDescription());

        try {
            database.updateMediaGroup(group);
        }
        catch (Database.NotFoundException e) {
            throw new Database.NotFoundException("No MediaGroup found with id=" + id);
        }

        return toResponse(group);
    }

    /**
     * Deletes a MediaGroup by ID.
     * <p>
     * Returns {@code true} if the group was deleted, {@code false} if it did not exist.
     *
     * @param id the group ID
     * @return {@code true} if deleted, {@code false} if not found
     * @throws SQLException if a database error occurs
     */
    public boolean deleteGroup(long id) throws SQLException {
        if (!database.mediaGroupExists(id)) {
            return false;
        }
        database.deleteMediaGroupById(id);
        return true;
    }

    /**
     * Converts a domain MediaGroup object to a response DTO.
     */
    private MediaGroupResponse toResponse(MediaGroup group) {
        return new MediaGroupResponse(
                group.getId(),
                group.getParentGroupId(),
                group.getTitle(),
                group.getDescription(),
                group.isHasThumbnail()
        );
    }
}
