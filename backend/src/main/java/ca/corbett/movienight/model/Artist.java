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
 * Each MusicVideo has a mandatory Artist association. An Artist has a mandatory name,
 * and an optional description.
 * The main Music Videos tab in the UI starts with a list of Artists, and clicking on
 * an Artist shows the music videos by that artist.
 * <p>
 * If a data directory is set, a thumbnail image can be associated with an artist. This is optional.
 * If present, the thumbnail will be presented in the UI.
 * </p>
 */
@Entity
@Table(name = "artists")
public class Artist {

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

    @Transient
    @JsonProperty(value = "hasThumbnail", access = JsonProperty.Access.READ_ONLY)
    private boolean hasThumbnail = false;

    @Transient
    @JsonProperty(value = "videoCount", access = JsonProperty.Access.READ_ONLY)
    private long videoCount = 0;

    public Artist() {
    }

    public Artist(String name) {
        this(name, null);
    }

    public Artist(String name, String description) {
        setName(name);
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
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

    public long getVideoCount() {
        return videoCount;
    }

    public void setVideoCount(long videoCount) {
        this.videoCount = videoCount;
    }
}
