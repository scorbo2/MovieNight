package ca.corbett.movienight.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * An Episode has a mandatory Series association.
 * A Series has a mandatory name, and an optional description.
 * The main TV Series tab in the UI starts with a list of Series,
 * and clicking on a Series shows the episodes in that series.
 * A Series can optionally have a year associated with it.
 * <p>
 * If a data directory is set, a thumbnail image can be associated with a series. This is optional.
 * If present, the thumbnail will be presented in the UI.
 * </p>
 */
@Entity
@Table(name = "series")
public class Series {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(unique = true, nullable = false)
    private String name;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    @Column
    private Integer year;

    @Transient
    @JsonProperty(value = "hasThumbnail", access = JsonProperty.Access.READ_ONLY)
    private boolean hasThumbnail = false;

    @Transient
    @JsonProperty(value = "episodeCount", access = JsonProperty.Access.READ_ONLY)
    private long episodeCount = 0;

    public Series() {
    }

    public Series(String name) {
        this(name, null);
    }

    public Series(String name, String description) {
        setName(name);
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isHasThumbnail() {
        return hasThumbnail;
    }

    public void setHasThumbnail(boolean hasThumbnail) {
        this.hasThumbnail = hasThumbnail;
    }

    public long getEpisodeCount() {
        return episodeCount;
    }

    public void setEpisodeCount(long episodeCount) {
        this.episodeCount = episodeCount;
    }
}
