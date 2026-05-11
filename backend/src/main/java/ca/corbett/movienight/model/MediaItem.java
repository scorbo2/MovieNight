package ca.corbett.movienight.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Transient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a single streamable media item. All MediaItems have a mandatory title, an optional
 * description, an optional last watched date, and an optional list of tags. The media file path
 * stored here is a relative path to the media file, relative to our configured data directory.
 * This allows the data directory to be moved or mounted to a different location without breaking
 * anything in this model. MediaItems can also have a thumbnail image, but these are stored
 * externally to this codebase - if a data directory is configured, thumbnail images can be
 * stored there and will be discovered automatically.
 * <p>
 *     <b>USING TAGS:</b> tags are any arbitrary String that can serve as metadata for
 *     the media item. This could be the name of an actor, a director, a film genre,
 *     a production year, or literally anything else that might be useful for searching
 *     and filtering media items. Tags are normalized before being stored - they are trimmed, lowercased,
 *     and deduplicated. Tags are completely optional.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since MovieNight 2.0
 */
public class MediaItem {

    private long id;
    private long mediaGroupId; // foreign key to the MediaGroup this item belongs to
    private String title;
    private String description;
    private LocalDate lastWatchedDate;
    private String mediaFilePath; // relative to our configured data directory
    private List<String> tags = new ArrayList<>();

    /**
     * Not stored in the database - populated at runtime as a convenience for the UI.
     */
    @Transient
    @JsonProperty(value = "hasThumbnail", access = JsonProperty.Access.READ_ONLY)
    private boolean hasThumbnail = false;

    public long getId() {
        return id;
    }

    public long getMediaGroupId() {
        return mediaGroupId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getLastWatchedDate() {
        return lastWatchedDate;
    }

    public String getMediaFilePath() {
        return mediaFilePath;
    }

    public List<String> getTags() {
        return new ArrayList<>(tags);
    }

    public boolean isHasThumbnail() {
        return hasThumbnail;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setMediaGroupId(long mediaGroupId) {
        this.mediaGroupId = mediaGroupId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLastWatchedDate(LocalDate lastWatchedDate) {
        this.lastWatchedDate = lastWatchedDate;
    }

    public void setMediaFilePath(String mediaFilePath) {
        this.mediaFilePath = mediaFilePath;
    }

    /**
     * All given tags are normalized (trimmed, lowercased, and deduplicated) before being stored.
     * Empty or null list as input is fine - empty list will be stored.
     */
    public void setTags(List<String> tags) {
        if (tags == null) {
            this.tags = new ArrayList<>();
        }
        else {
            this.tags = tags.stream()
                            .filter(t -> t != null && !t.isBlank())
                            .map(t -> t.trim().toLowerCase())
                            .distinct()
                            .collect(Collectors.toCollection(ArrayList::new));
        }
    }

    public void setHasThumbnail(boolean hasThumbnail) {
        this.hasThumbnail = hasThumbnail;
    }
}
