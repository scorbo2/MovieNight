package ca.corbett.movienight.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

@Entity
@Table(name = "genres")
public class Genre {

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

    public Genre() {}

    public Genre(String name) {
        this(name, null);
    }

    public Genre(String name, String description) {
        setName(name);
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) {
        // Normalize:
        this.name = name == null ? null : name.trim().toLowerCase(Locale.ROOT);
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isHasThumbnail() { return hasThumbnail; }
    public void setHasThumbnail(boolean hasThumbnail) { this.hasThumbnail = hasThumbnail; }

}
