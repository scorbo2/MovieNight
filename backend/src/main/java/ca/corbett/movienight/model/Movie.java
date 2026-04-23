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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a single movie (or any standalone long-form video content really).
 * A Movie has a mandatory title, and a mandatory Genre association.
 * It can optionally have a release year, and a description. For additional metadata,
 * use the tags list, and attach any arbitrary string tags to describe the movie.
 * <p>
 * If a data directory is set, a thumbnail image can be associated with a movie.
 * This is optional. If present, the thumbnail will be presented in the UI.
 * </p>
 */
@Entity
@Table(name = "movies")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String title;

    @Column
    private Integer year;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "genre_id", nullable = false)
    private Genre genre;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    @Column
    private Boolean watched = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "movie_tags", joinColumns = @JoinColumn(name = "movie_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @NotBlank
    @Column(nullable = false)
    private String videoFilePath;

    @Transient
    @JsonProperty(value = "hasThumbnail", access = JsonProperty.Access.READ_ONLY)
    private boolean hasThumbnail = false;

    public Movie() {
    }

    public Movie(String title, Integer year, Genre genre,
                 String description, Boolean watched) {
        this.title = title;
        this.year = year;
        this.genre = genre;
        this.description = description;
        this.watched = watched;
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

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Genre getGenre() {
        return genre;
    }

    public void setGenre(Genre genre) {
        this.genre = genre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getWatched() {
        return watched;
    }

    public void setWatched(Boolean watched) {
        this.watched = watched;
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

    public String getVideoFilePath() {
        return videoFilePath;
    }

    public void setVideoFilePath(String videoFilePath) {
        this.videoFilePath = videoFilePath;
    }
}
