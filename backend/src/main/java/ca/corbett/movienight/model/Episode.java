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
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a single episode of a TV series (or web series, or any kind of episodic content).
 * An Episode has a mandatory title, and a mandatory Series association. It can optionally
 * have a season and episode number, and a description. For additional metadata, use the
 * tags list, and attach any arbitrary string tags to describe the episode.
 * <p>
 * If a data directory is set, a thumbnail image can be associated with an episode.
 * This is optional. If present, the thumbnail will be presented in the UI.
 * </p>
 */
@Entity
@Table(name = "episodes")
public class Episode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "series_id", nullable = false)
    private Series series;

    @Size(max = 255)
    @Column
    private String episodeTitle;

    @Column
    private Integer season;

    @Column
    private Integer episode;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    @Column
    private LocalDate lastWatchedDate;

    @Transient
    @JsonProperty(value = "watchedRecently", access = JsonProperty.Access.READ_ONLY)
    private boolean watchedRecently;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "episode_tags", joinColumns = @JoinColumn(name = "episode_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @NotBlank
    @Column(nullable = false)
    private String videoFilePath;

    @Transient
    @JsonProperty(value = "hasThumbnail", access = JsonProperty.Access.READ_ONLY)
    private boolean hasThumbnail = false;

    public Episode() {
    }

    public Episode(Series series, String episodeTitle, Integer season, Integer episode,
                   String description, LocalDate lastWatchedDate) {
        this.series = series;
        this.episodeTitle = episodeTitle;
        this.season = season;
        this.episode = episode;
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

    public Series getSeries() {
        return series;
    }

    public void setSeries(Series series) {
        this.series = series;
    }

    public String getEpisodeTitle() {
        return episodeTitle;
    }

    public void setEpisodeTitle(String episodeTitle) {
        this.episodeTitle = episodeTitle;
    }

    public Integer getSeason() {
        return season;
    }

    public void setSeason(Integer season) {
        this.season = season;
    }

    public Integer getEpisode() {
        return episode;
    }

    public void setEpisode(Integer episode) {
        this.episode = episode;
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

    public String getVideoFilePath() {
        return videoFilePath;
    }

    public void setVideoFilePath(String videoFilePath) {
        this.videoFilePath = videoFilePath;
    }
}
