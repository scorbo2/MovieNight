package ca.corbett.movienight.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "episodes")
public class Episode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String seriesName;

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
    private Boolean watched = false;

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

    public Episode() {}

    public Episode(String seriesName, String episodeTitle, Integer season, Integer episode,
                   String description, Boolean watched) {
        this.seriesName = seriesName;
        this.episodeTitle = episodeTitle;
        this.season = season;
        this.episode = episode;
        this.description = description;
        this.watched = watched;
    }

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSeriesName() { return seriesName; }
    public void setSeriesName(String seriesName) { this.seriesName = seriesName; }

    public String getEpisodeTitle() { return episodeTitle; }
    public void setEpisodeTitle(String episodeTitle) { this.episodeTitle = episodeTitle; }

    public Integer getSeason() { return season; }
    public void setSeason(Integer season) { this.season = season; }

    public Integer getEpisode() { return episode; }
    public void setEpisode(Integer episode) { this.episode = episode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getWatched() { return watched; }
    public void setWatched(Boolean watched) { this.watched = watched; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) {
        if (tags == null) {
            this.tags = new ArrayList<>();
        } else {
            this.tags = tags.stream()
                    .filter(t -> t != null && !t.isBlank())
                    .map(t -> t.trim().toLowerCase())
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }

    public boolean isHasThumbnail() { return hasThumbnail; }
    public void setHasThumbnail(boolean hasThumbnail) { this.hasThumbnail = hasThumbnail; }

    public String getVideoFilePath() { return videoFilePath; }
    public void setVideoFilePath(String videoFilePath) { this.videoFilePath = videoFilePath; }
}
