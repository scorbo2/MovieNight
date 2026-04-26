package ca.corbett.movienight.service;

import ca.corbett.movienight.model.Genre;
import ca.corbett.movienight.model.Movie;
import ca.corbett.movienight.repository.MovieRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class MovieService {

    @Value("${movienight.recently-watched-days:3}")
    private int recentlyWatchedDays;

    private final MovieRepository movieRepository;
    private final ThumbnailService thumbnailService;

    public MovieService(MovieRepository movieRepository, ThumbnailService thumbnailService) {
        this.movieRepository = movieRepository;
        this.thumbnailService = thumbnailService;
    }

    public List<Movie> getAllMovies() {
        return populateTransientFields(movieRepository.findAll());
    }

    public Optional<Movie> getMovieById(Long id) {
        return movieRepository.findById(id).map(this::populateTransientFields);
    }

    public Movie saveMovie(Movie movie) {
        // Reject request if the movie's genre is null:
        if (movie.getGenre() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Movie genre is required.");
        }

        return populateTransientFields(movieRepository.save(movie));
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
            movie.setTags(updatedMovie.getTags());
            movie.setVideoFilePath(updatedMovie.getVideoFilePath());
            return populateTransientFields(movieRepository.save(movie));
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
        return populateTransientFields(movieRepository.findByTitleContainingIgnoreCase(title));
    }

    public List<Movie> searchByTag(String tag) {
        return populateTransientFields(movieRepository.findByTagsContainingIgnoreCase(tag));
    }

    public List<Movie> searchMovies(Genre genre) {
        return populateTransientFields(movieRepository.findByGenre(genre));
    }

    public List<Movie> searchMovies(String title, String tag, Long genreId) {
        Specification<Movie> spec = Specification.where(titleContains(title))
                .and(tagContains(tag))
                .and(genreEquals(genreId));
        Sort sort = Sort.by(
                Sort.Order.asc("title").nullsLast(),
                Sort.Order.asc("year").nullsLast() // Example: "Ghostbusters" (1984) and "Ghostbusters" (2016)
        );
        return populateTransientFields(movieRepository.findAll(spec, sort));
    }

    private Movie populateHasThumbnail(Movie movie) {
        movie.setHasThumbnail(thumbnailService.hasThumbnail("movies", movie.getId()));
        return movie;
    }

    private Movie populateWatchedRecently(Movie movie) {
        int recentlyWatchedDays = Math.max(this.recentlyWatchedDays, 0);
        
        // This feature can be explicitly disabled by setting day count to 0.
        // It's also possible this video has never been watched.
        if (recentlyWatchedDays == 0 || movie.getLastWatchedDate() == null) {
            movie.setWatchedRecently(false);
            return movie;
        }

        // Otherwise, do the math on the last watch date to determine if it's recent:
        movie.setWatchedRecently(movie.getLastWatchedDate().isAfter(LocalDate.now().minusDays(recentlyWatchedDays)));

        return movie;
    }

    private static Specification<Movie> titleContains(String title) {
        return (root, query, cb) -> {
            if (title == null || title.isBlank()) return null;
            return cb.like(cb.lower(root.get("title")), "%" + title.trim().toLowerCase() + "%");
        };
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

    private Movie populateTransientFields(Movie movie) {
        populateHasThumbnail(movie);
        populateWatchedRecently(movie);
        return movie;
    }

    private List<Movie> populateTransientFields(List<Movie> movies) {
        movies.forEach(this::populateTransientFields);
        return movies;
    }

    /**
     * Visible only for testing, because Spring is dumb.
     */
    public void setRecentlyWatchedDays(int number) {
        this.recentlyWatchedDays = Math.max(number, 0);
    }
}
