package ca.corbett.movienight.api.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating a MediaItem.
 * <p>
 * Used by POST /api/media-groups/{groupId}/items.
 * <p>
 * The mediaGroupId comes from the path, not the request body.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class MediaItemUpsertRequest {

    private Long mediaGroupId;
    private String title;
    private String description;
    private LocalDate lastWatchedDate;
    private String mediaFilePath;
    private List<String> tags;

    public MediaItemUpsertRequest() {
        // Jackson needs a no-arg constructor
    }

    public Long getMediaGroupId() {
        return mediaGroupId;
    }

    public void setMediaGroupId(Long mediaGroupId) {
        this.mediaGroupId = mediaGroupId;
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

    public LocalDate getLastWatchedDate() {
        return lastWatchedDate;
    }

    public void setLastWatchedDate(LocalDate lastWatchedDate) {
        this.lastWatchedDate = lastWatchedDate;
    }

    public String getMediaFilePath() {
        return mediaFilePath;
    }

    public void setMediaFilePath(String mediaFilePath) {
        this.mediaFilePath = mediaFilePath;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
