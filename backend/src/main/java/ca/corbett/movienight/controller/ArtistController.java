package ca.corbett.movienight.controller;

import ca.corbett.movienight.model.Artist;
import ca.corbett.movienight.service.ArtistService;
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
@RequestMapping("/api/artists")
@CrossOrigin(origins = "http://localhost:5173")
public class ArtistController {

    private final ArtistService artistService;
    private final ThumbnailService thumbnailService;

    public ArtistController(ArtistService artistService, ThumbnailService thumbnailService) {
        this.artistService = artistService;
        this.thumbnailService = thumbnailService;
    }

    @GetMapping
    public List<Artist> getAllArtists() {
        return artistService.getAllArtists();
    }

    @GetMapping("/{id}")
    public Artist getArtistById(@PathVariable Long id) {
        return artistService.getArtistById(id)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                                           "Artist not found with id: " + id));
    }

    @PostMapping
    public ResponseEntity<Artist> createArtist(@Valid @RequestBody Artist artist) {
        Artist saved = artistService.saveArtist(artist);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public Artist updateArtist(@PathVariable Long id,
                               @Valid @RequestBody Artist artist) {
        return artistService.updateArtist(id, artist);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArtist(@PathVariable Long id) {
        artistService.deleteArtist(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/thumbnail")
    public ResponseEntity<Void> uploadArtistThumbnail(@PathVariable Long id,
                                                      @RequestParam("file") MultipartFile file) {
        artistService.requireArtist(id);
        thumbnailService.saveThumbnail(file, "artists", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<Resource> getArtistThumbnail(@PathVariable Long id) {
        artistService.requireArtist(id);
        Path thumbnailPath = thumbnailService.getThumbnailPath("artists", id);
        if (thumbnailPath == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                              "Artist thumbnail not found for id: " + id);
        }
        String filename = thumbnailPath.getFileName().toString().toLowerCase();
        MediaType mediaType = filename.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                             .contentType(mediaType)
                             .body(new PathResource(thumbnailPath));
    }

    @DeleteMapping("/{id}/thumbnail")
    public ResponseEntity<Void> deleteArtistThumbnail(@PathVariable Long id) {
        artistService.requireArtist(id);
        thumbnailService.deleteThumbnail("artists", id);
        return ResponseEntity.noContent().build();
    }
}
