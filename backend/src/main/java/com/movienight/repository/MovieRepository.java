package com.movienight.repository;

import com.movienight.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    List<Movie> findByTitleContainingIgnoreCase(String title);

    List<Movie> findByWatched(Boolean watched);

    List<Movie> findByGenreIgnoreCase(String genre);
}
