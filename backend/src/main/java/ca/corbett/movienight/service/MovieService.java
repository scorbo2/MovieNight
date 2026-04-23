package ca.corbett.movienight.service;

import ca.corbett.movienight.model.Genre;
import ca.corbett.movienight.model.Movie;
import ca.corbett.movienight.repository.MovieRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final ThumbnailService thumbnailService;

    public MovieService(MovieRepository movieRepository, ThumbnailService thumbnailService) {
        this.movieRepository = movieRepository;
        this.thumbnailService = thumbnailService;
    }

    public List<Movie> getAllMovies() {
        return populateHasThumbnail(movieRepository.findAll());
    }

    public Optional<Movie> getMovieById(Long id) {
        return movieRepository.findById(id).map(this::populateHasThumbnail);
    }

    public Movie saveMovie(Movie movie) {
        // Reject request if the movie's genre is null:
        if (movie.getGenre() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Movie genre is required.");
        }

        return populateHasThumbnail(movieRepository.save(movie));
    }

    public Movie updateMovie(Long id, Movie updatedMovie) {
        // Reject request if the movie's genre is null:
        if (updatedMovie.getGenre() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Movie genre is required.");
        }

        return movieRepository.findById(id).map(movie -> {
            movie.setTitle(updatedMovie.getTitle());
            movie.setYear(updatedMovie.getYear());
            movie.setGenre(updatedMovie.getGenre());
            movie.setDescription(updatedMovie.getDescription());
            movie.setWatched(updatedMovie.getWatched());
            movie.setTags(updatedMovie.getTags());
            movie.setVideoFilePath(updatedMovie.getVideoFilePath());
            return populateHasThumbnail(movieRepository.save(movie));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found with id: " + id));
    }

    public void deleteMovie(Long id) {
        requireMovie(id);
        thumbnailService.deleteThumbnail("movies", id);
        movieRepository.deleteById(id);
    }

    public Movie requireMovie(Long id) {
        return getMovieById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Movie not found with id: " + id));
    }

    public List<Movie> searchByTitle(String title) {
        return populateHasThumbnail(movieRepository.findByTitleContainingIgnoreCase(title));
    }

    public List<Movie> getByWatched(Boolean watched) {
        return populateHasThumbnail(movieRepository.findByWatched(watched));
    }

    public List<Movie> searchByTag(String tag) {
        return populateHasThumbnail(movieRepository.findByTagsContainingIgnoreCase(tag));
    }

    public List<Movie> searchMovies(Genre genre) {
        return populateHasThumbnail(movieRepository.findByGenre(genre));
    }

    public List<Movie> searchMovies(String title, Boolean watched, String tag, Long genreId) {
        Specification<Movie> spec = Specification.where(titleContains(title))
                .and(watchedEquals(watched))
                .and(tagContains(tag))
                .and(genreEquals(genreId));
        return populateHasThumbnail(movieRepository.findAll(spec));
    }

    private Movie populateHasThumbnail(Movie movie) {
        movie.setHasThumbnail(thumbnailService.hasThumbnail("movies", movie.getId()));
        return movie;
    }

    private List<Movie> populateHasThumbnail(List<Movie> movies) {
        movies.forEach(this::populateHasThumbnail);
        return movies;
    }

    private static Specification<Movie> titleContains(String title) {
        return (root, query, cb) -> {
            if (title == null || title.isBlank()) return null;
            return cb.like(cb.lower(root.get("title")), "%" + title.trim().toLowerCase() + "%");
        };
    }

    private static Specification<Movie> watchedEquals(Boolean watched) {
        return (root, query, cb) -> watched == null ? null : cb.equal(root.get("watched"), watched);
    }

    private static Specification<Movie> tagContains(String tag) {
        return (root, query, cb) -> {
            if (tag == null || tag.isBlank()) return null;
            query.distinct(true);
            Join<Movie, String> tagsJoin = root.join("tags", JoinType.INNER);
            return cb.like(cb.lower(tagsJoin.as(String.class)), "%" + tag.trim().toLowerCase() + "%");
        };
    }

    private static Specification<Movie> genreEquals(Long genreId) {
        return (root, query, cb) -> genreId == null ? null : cb.equal(root.get("genre").get("id"), genreId);
    }
}
