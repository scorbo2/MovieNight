package ca.corbett.movienight.api.dto;

/**
 * Response DTO for a single MediaGroup.
 * <p>
 * Matches the JSON contract defined in the API plan.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class MediaGroupResponse {

    private final long id;
    private final Long parentGroupId;
    private final String title;
    private final String description;
    private final boolean hasThumbnail;

    public MediaGroupResponse(long id, Long parentGroupId, String title, String description, boolean hasThumbnail) {
        this.id = id;
        this.parentGroupId = parentGroupId;
        this.title = title;
        this.description = description;
        this.hasThumbnail = hasThumbnail;
    }

    public long getId() {
        return id;
    }

    public Long getParentGroupId() {
        return parentGroupId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isHasThumbnail() {
        return hasThumbnail;
    }
}
