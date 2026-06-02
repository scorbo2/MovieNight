package ca.corbett.movienight.api.dto;

import ca.corbett.movienight.model.TrackMetadata;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for a single MediaItem.
 * <p>
 * Matches the JSON contract defined in the API plan.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class MediaItemResponse {

    private final long id;
    private final long mediaGroupId;
    private final String title;
    private final String description;
    private final LocalDate lastWatchedDate;
    private final String mediaFilePath;
    private final List<String> tags;
    private final boolean hasThumbnail;
    private final boolean isRecentlyWatched;
    private final List<TrackMetadata> audioTracks;
    private final List<TrackMetadata> subtitleTracks;

    public MediaItemResponse(long id, long mediaGroupId, String title, String description,
                             LocalDate lastWatchedDate, String mediaFilePath, List<String> tags,
                             boolean hasThumbnail, boolean isRecentlyWatched,
                             List<TrackMetadata> audioTracks, List<TrackMetadata> subtitleTracks) {
        this.id = id;
        this.mediaGroupId = mediaGroupId;
        this.title = title;
        this.description = description;
        this.lastWatchedDate = lastWatchedDate;
        this.mediaFilePath = mediaFilePath;
        this.tags = tags;
        this.hasThumbnail = hasThumbnail;
        this.isRecentlyWatched = isRecentlyWatched;
        this.audioTracks = audioTracks;
        this.subtitleTracks = subtitleTracks;
    }

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
        return tags;
    }

    public boolean isHasThumbnail() {
        return hasThumbnail;
    }

    public boolean isRecentlyWatched() {
        return isRecentlyWatched;
    }

    public List<TrackMetadata> getAudioTracks() {
        return new ArrayList<>(audioTracks);
    }

    public List<TrackMetadata> getSubtitleTracks() {
        return new ArrayList<>(subtitleTracks);
    }
}
