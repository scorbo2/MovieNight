package ca.corbett.movienight.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MediaGroups are used to group MediaItems together.
 * They have a mandatory title and an optional description.
 * MediaGroups can contain other MediaGroups and/or MediaItems.
 * This structure allows the user to create arbitrary groups
 * of media items, such as movies by genre, or TV episodes by series,
 * or music videos by artist, or whatever else they want.
 * <p>
 * One of many possible examples:
 * </p>
 * <pre>
 * Group: "TV Shows"
 *   - Group: "Dexter"
 *     - Group: "Season 1"
 *       - MediaItem: "Dexter S01E01 - Dexter"
 *       - ...
 *       - Group: "DVD extras"
 *         - MediaItem: "Season 1 Blooper reel"
 *         - ...
 * </pre>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since MovieNight 2.0
 */
public class MediaGroup {

    private long id; // not nullable! primary key autoincrement
    private Long parentGroupId; // null means "top level group" (no parent)
    private String title; // not nullable! mandatory title
    private String description; // nullable - optional description

    /**
     * Not stored in the database - populated at runtime as a convenience for the UI.
     */
    @JsonProperty(value = "hasThumbnail", access = JsonProperty.Access.READ_ONLY)
    private boolean hasThumbnail = false;

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

    public void setId(long id) {
        this.id = id;
    }

    public void setParentGroupId(Long parentGroupId) {
        this.parentGroupId = parentGroupId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setHasThumbnail(boolean hasThumbnail) {
        this.hasThumbnail = hasThumbnail;
    }

    public boolean isTopLevelGroup() {
        return parentGroupId == null;
    }
}
