package ca.corbett.movienight.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a single music video.
 * A MusicVideo has a mandatory title and a mandatory Artist association.
 * It can optionally have an album, a release year, and a description.
 * For additional metadata, use the tags list.
 * <p>
 * If a data directory is set, a thumbnail image can be associated with a music video.
 * This is optional. If present, the thumbnail will be presented in the UI.
 * </p>
 */
@Entity
@Table(name = "music_videos")
public class MusicVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String title;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @Size(max = 255)
    @Column
    private String album;

    @Column
    private Integer year;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    @Column
    private LocalDate lastWatchedDate;

    @Transient
    @JsonProperty(value = "watchedRecently", access = JsonProperty.Access.READ_ONLY)
    private boolean watchedRecently;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "music_video_tags", joinColumns = @JoinColumn(name = "music_video_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @NotBlank
    @Column(nullable = false)
    private String videoFilePath;

    @Transient
    @JsonProperty(value = "hasThumbnail", access = JsonProperty.Access.READ_ONLY)
    private boolean hasThumbnail = false;

    public MusicVideo() {
    }

    public MusicVideo(String title, Artist artist, String album,
                      Integer year, String description, LocalDate lastWatchedDate) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.year = year;
        this.description = description;
        this.lastWatchedDate = lastWatchedDate;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Artist getArtist() {
        return artist;
    }

    public void setArtist(Artist artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
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

    public List<String> getTags() {
        return tags;
    }

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

    public String getVideoFilePath() {
        return videoFilePath;
    }

    public void setVideoFilePath(String videoFilePath) {
        this.videoFilePath = videoFilePath;
    }

    public boolean isHasThumbnail() {
        return hasThumbnail;
    }

    public void setHasThumbnail(boolean hasThumbnail) {
        this.hasThumbnail = hasThumbnail;
    }

    public boolean isWatchedRecently() {
        return watchedRecently;
    }

    public void setWatchedRecently(boolean watchedRecently) {
        this.watchedRecently = watchedRecently;
    }
}
