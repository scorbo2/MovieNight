package ca.corbett.movienight.controller;

import ca.corbett.movienight.model.Movie;
import ca.corbett.movienight.service.MovieService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@CrossOrigin(origins = "http://localhost:5173")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public List<Movie> getAllMovies(@RequestParam(required = false) String title,
                                   @RequestParam(required = false) Boolean watched,
                                   @RequestParam(required = false) String tag) {
        if (title != null && !title.isBlank()) {
            return movieService.searchByTitle(title);
        }
        if (watched != null) {
            return movieService.getByWatched(watched);
        }
        if (tag != null && !tag.isBlank()) {
            return movieService.searchByTag(tag);
        }
        return movieService.getAllMovies();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Movie> getMovieById(@PathVariable Long id) {
        return movieService.getMovieById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Movie> createMovie(@Valid @RequestBody Movie movie) {
        Movie saved = movieService.saveMovie(movie);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Movie> updateMovie(@PathVariable Long id,
                                             @Valid @RequestBody Movie movie) {
        if (movieService.getMovieById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Movie updated = movieService.updateMovie(id, movie);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        if (movieService.getMovieById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }
}
