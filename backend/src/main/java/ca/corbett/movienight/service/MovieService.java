package ca.corbett.movienight.service;

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
            movie.setDescription(updatedMovie.getDescription());
            movie.setWatched(updatedMovie.getWatched());
            movie.setTags(updatedMovie.getTags());
            return movieRepository.save(movie);
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found with id: " + id));
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

    public List<Movie> searchByTag(String tag) {
        return movieRepository.findByTagsContainingIgnoreCase(tag);
    }

    public List<Movie> searchMovies(String title, Boolean watched, String tag) {
        Specification<Movie> spec = Specification.where(titleContains(title))
                .and(watchedEquals(watched))
                .and(tagContains(tag));
        return movieRepository.findAll(spec);
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
}
