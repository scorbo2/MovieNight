package ca.corbett.movienight.controller;

import ca.corbett.movienight.model.Genre;
import ca.corbett.movienight.service.GenreService;
import ca.corbett.movienight.service.ThumbnailService;
import jakarta.validation.Valid;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/genres")
@CrossOrigin(origins = "http://localhost:5173")
public class GenreController {

    private final GenreService genreService;
    private final ThumbnailService thumbnailService;

    public GenreController(GenreService genreService, ThumbnailService thumbnailService) {
        this.genreService = genreService;
        this.thumbnailService = thumbnailService;
    }

    @GetMapping
    public List<Genre> getAllGenres() {
        return genreService.getAllGenres();
    }

    @GetMapping("/{id}")
    public Genre getGenreById(@PathVariable Long id) {
        return genreService.getGenreById(id)
                           .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                                          "Genre not found with id: " + id));
    }

    @PostMapping
    public ResponseEntity<Genre> createGenre(@Valid @RequestBody Genre genre) {
        Genre saved = genreService.saveGenre(genre);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public Genre updateGenre(@PathVariable Long id,
                             @Valid @RequestBody Genre genre) {
        return genreService.updateGenre(id, genre);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGenre(@PathVariable Long id) {
        genreService.deleteGenre(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/thumbnail")
    public ResponseEntity<Void> uploadGenreThumbnail(@PathVariable Long id,
                                                     @RequestParam("file") MultipartFile file) {
        genreService.requireGenre(id);
        thumbnailService.saveThumbnail(file, "genres", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<Resource> getGenreThumbnail(@PathVariable Long id) {
        genreService.requireGenre(id);
        Path thumbnailPath = thumbnailService.getThumbnailPath("genres", id);
        if (thumbnailPath == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                              "Genre thumbnail not found for id: " + id);
        }
        String filename = thumbnailPath.getFileName().toString().toLowerCase();
        MediaType mediaType = filename.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                             .contentType(mediaType)
                             .body(new PathResource(thumbnailPath));
    }

    @DeleteMapping("/{id}/thumbnail")
    public ResponseEntity<Void> deleteGenreThumbnail(@PathVariable Long id) {
        genreService.requireGenre(id);
        thumbnailService.deleteThumbnail("genres", id);
        return ResponseEntity.noContent().build();
    }
}
