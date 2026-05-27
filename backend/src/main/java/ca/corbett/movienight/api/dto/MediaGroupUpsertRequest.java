package ca.corbett.movienight.api.dto;

/**
 * Request DTO for creating or updating a MediaGroup.
 * <p>
 * Used by both POST /api/media-groups and PUT /api/media-groups/{id}.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class MediaGroupUpsertRequest {

    private Long parentGroupId;
    private String title;
    private String description;

    public MediaGroupUpsertRequest() {
        // Jackson needs a no-arg constructor
    }

    public Long getParentGroupId() {
        return parentGroupId;
    }

    public void setParentGroupId(Long parentGroupId) {
        this.parentGroupId = parentGroupId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
