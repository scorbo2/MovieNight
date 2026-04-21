package com.movienight.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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

    @Size(max = 100)
    @Column
    private String genre;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    @Column
    private Boolean watched = false;

    public Movie() {}

    public Movie(String title, Integer year, String genre,
                 String description, Boolean watched) {
        this.title = title;
        this.year = year;
        this.genre = genre;
        this.description = description;
        this.watched = watched;
    }

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getWatched() { return watched; }
    public void setWatched(Boolean watched) { this.watched = watched; }
}
