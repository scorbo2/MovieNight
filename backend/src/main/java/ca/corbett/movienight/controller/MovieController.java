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
import java.nio.file.Path;
import java.util.List;
import org.springframework.web.server.ResponseStatusException;

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
                                   @RequestParam(required = false) String tag,
                                   @RequestParam(required = false) Long genreId) {
        return movieService.searchMovies(title, watched, tag, genreId);
    }

    @GetMapping("/{id}")
    public Movie getMovieById(@PathVariable Long id) {
        return movieService.getMovieById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Movie not found with id: " + id));
    }

    @PostMapping
    public ResponseEntity<Movie> createMovie(@Valid @RequestBody Movie movie) {
        Movie saved = movieService.saveMovie(movie);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public Movie updateMovie(@PathVariable Long id,
                             @Valid @RequestBody Movie movie) {
        return movieService.updateMovie(id, movie);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/thumbnail")
    public ResponseEntity<Void> uploadMovieThumbnail(@PathVariable Long id,
                                                     @RequestParam("file") MultipartFile file) {
        movieService.requireMovie(id);
        thumbnailService.saveThumbnail(file, "movies", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<Resource> getMovieThumbnail(@PathVariable Long id) {
        movieService.requireMovie(id);
        Path thumbnailPath = thumbnailService.getThumbnailPath("movies", id);
        if (thumbnailPath == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Movie thumbnail not found for id: " + id);
        }
        String filename = thumbnailPath.getFileName().toString().toLowerCase();
        MediaType mediaType = filename.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(new PathResource(thumbnailPath));
    }

    @DeleteMapping("/{id}/thumbnail")
    public ResponseEntity<Void> deleteMovieThumbnail(@PathVariable Long id) {
        movieService.requireMovie(id);
        thumbnailService.deleteThumbnail("movies", id);
        return ResponseEntity.noContent().build();
    }
}
