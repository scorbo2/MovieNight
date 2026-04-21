package ca.corbett.movienight.controller;

import ca.corbett.movienight.model.Movie;
import ca.corbett.movienight.service.MovieService;
import ca.corbett.movienight.service.ThumbnailService;
import jakarta.validation.Valid;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/movies")
@CrossOrigin(origins = "http://localhost:5173")
public class MovieController {

    private final MovieService movieService;
    private final ThumbnailService thumbnailService;

    public MovieController(MovieService movieService, ThumbnailService thumbnailService) {
        this.movieService = movieService;
        this.thumbnailService = thumbnailService;
    }

    @GetMapping
    public List<Movie> getAllMovies(@RequestParam(required = false) String title,
                                   @RequestParam(required = false) Boolean watched,
                                   @RequestParam(required = false) String tag) {
        return movieService.searchMovies(title, watched, tag);
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

    @PostMapping("/{id}/thumbnail")
    public ResponseEntity<Void> uploadMovieThumbnail(@PathVariable Long id,
                                                     @RequestParam("file") MultipartFile file) {
        if (movieService.getMovieById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!thumbnailService.isEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        try {
            thumbnailService.saveThumbnail(file, "movies", id);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<Resource> getMovieThumbnail(@PathVariable Long id) {
        if (movieService.getMovieById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Path thumbnailPath = thumbnailService.getThumbnailPath("movies", id);
        if (thumbnailPath == null) {
            return ResponseEntity.notFound().build();
        }
        String filename = thumbnailPath.getFileName().toString().toLowerCase();
        MediaType mediaType = filename.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(new PathResource(thumbnailPath));
    }

    @DeleteMapping("/{id}/thumbnail")
    public ResponseEntity<Void> deleteMovieThumbnail(@PathVariable Long id) {
        if (movieService.getMovieById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        thumbnailService.deleteThumbnail("movies", id);
        return ResponseEntity.noContent().build();
    }
}
