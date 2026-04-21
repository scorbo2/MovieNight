package com.movienight.service;

import com.movienight.model.Movie;
import com.movienight.repository.MovieRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    public Optional<Movie> getMovieById(Long id) {
        return movieRepository.findById(id);
    }

    public Movie saveMovie(Movie movie) {
        return movieRepository.save(movie);
    }

    public Movie updateMovie(Long id, Movie updatedMovie) {
        return movieRepository.findById(id).map(movie -> {
            movie.setTitle(updatedMovie.getTitle());
            movie.setYear(updatedMovie.getYear());
            movie.setGenre(updatedMovie.getGenre());
            movie.setDirector(updatedMovie.getDirector());
            movie.setRating(updatedMovie.getRating());
            movie.setDescription(updatedMovie.getDescription());
            movie.setWatched(updatedMovie.getWatched());
            return movieRepository.save(movie);
        }).orElseThrow(() -> new RuntimeException("Movie not found with id: " + id));
    }

    public void deleteMovie(Long id) {
        movieRepository.deleteById(id);
    }

    public List<Movie> searchByTitle(String title) {
        return movieRepository.findByTitleContainingIgnoreCase(title);
    }

    public List<Movie> getByWatched(Boolean watched) {
        return movieRepository.findByWatched(watched);
    }
}
