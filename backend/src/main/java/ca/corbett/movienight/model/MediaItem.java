package ca.corbett.movienight.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a single streamable media item. All MediaItems have a mandatory title, an optional
 * description, an optional last watched date, and an optional list of tags. MediaItems must
 * belong to a MediaGroup - the mediaGroupId field here is a foreign key reference enforced
 * by the database.
 * <p>
 * <b>The media file path</b> - this is a relative file path! The system will store a configured
 * "data directory", and all media file paths are relative to that data directory. This allows
 * the data directory to be moved (or remounted, if it's a file share) without breaking this model.
 * The media file path is mandatory, but it is not necessary for it to be unique.
 * </p>
 * <p>
 * <b>Thumbnails</b> - MediaItems can have a thumbnail image, but we don't explicitly store the
 * location of it here. Instead, a "thumbnails" directory will be automatically created inside the
 * configured data directory, and thumbnail images will be stored using the media item's ID as the filename
 * (example: "thumbnails/MediaItem_123.jpg" for a media item with ID 123).
 * <p>
 * <b>USING TAGS:</b> tags are any arbitrary String that can serve as metadata for
 * the media item. These are used for searching purposes. This could be the name of an actor, a director, a film genre,
 * a production year, or literally anything else that might be useful for searching
 * and filtering media items. Tags are normalized before being stored - they are trimmed, lowercased,
 * and deduplicated. Tags are completely optional.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since MovieNight 2.0
 */
public class MediaItem {

    private long id; // not nullable! primary key autoincrement
    private long mediaGroupId; // foreign key to the MediaGroup this item belongs to
    private String title; // not nullable! mandatory title
    private String description; // nullable - optional description
    private LocalDate lastWatchedDate; // null means "not yet watched"
    private String mediaFilePath; // not nullable! relative to our configured data directory

    // When stored to the database, this list is serialized to a single comma-separated string.
    // It is stored in the database as a simple string (can be empty or null for "no tags")
    private List<String> tags = new ArrayList<>();

    /**
     * Not stored in the database - populated at runtime as a convenience for the UI.
     */
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

    public String getTagsAsCommaSeparatedString() {
        return String.join(",", tags);
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
     * Normalizes and adds the given tag to the list of tags, if it is not already present.
     * Normalization means that the tag is trimmed of leading and trailing whitespace, converted to lowercase,
     * and checked for duplicates. For example:
     * <pre>
     *     mediaItem.addTag(" Action ");
     *     mediaItem.addTag("ACTION");
     *     mediaItem.addTag("Action    ");
     *     System.out.println(mediaItem.getTags()); // Output: ["action"]
     * </pre>
     *
     * @param tag Any non-null, non-blank string to be added as a tag. Null or blank tags are ignored.
     */
    public void addTag(String tag) {
        if (tag != null && !tag.isBlank()) {
            String normalizedTag = tag.trim().toLowerCase();
            if (!tags.contains(normalizedTag)) {
                tags.add(normalizedTag);
            }
        }
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
