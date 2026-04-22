package ca.corbett.movienight.controller;

import ca.corbett.movienight.model.Episode;
import ca.corbett.movienight.service.EpisodeService;
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
@RequestMapping("/api/episodes")
@CrossOrigin(origins = "http://localhost:5173")
public class EpisodeController {

    private final EpisodeService episodeService;
    private final ThumbnailService thumbnailService;

    public EpisodeController(EpisodeService episodeService, ThumbnailService thumbnailService) {
        this.episodeService = episodeService;
        this.thumbnailService = thumbnailService;
    }

    @GetMapping
    public List<Episode> getAllEpisodes(@RequestParam(required = false) String seriesName,
                                        @RequestParam(required = false) Integer season,
                                        @RequestParam(required = false) Integer episode,
                                        @RequestParam(required = false) Boolean watched,
                                        @RequestParam(required = false) String tag) {
        return episodeService.searchEpisodes(seriesName, season, episode, watched, tag);
    }

    @GetMapping("/{id}")
    public Episode getEpisodeById(@PathVariable Long id) {
        return episodeService.getEpisodeById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Episode not found with id: " + id));
    }

    @PostMapping
    public ResponseEntity<Episode> createEpisode(@Valid @RequestBody Episode episode) {
        Episode saved = episodeService.saveEpisode(episode);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public Episode updateEpisode(@PathVariable Long id,
                                 @Valid @RequestBody Episode episode) {
        return episodeService.updateEpisode(id, episode);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEpisode(@PathVariable Long id) {
        episodeService.deleteEpisode(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/thumbnail")
    public ResponseEntity<Void> uploadEpisodeThumbnail(@PathVariable Long id,
                                                       @RequestParam("file") MultipartFile file) {
        episodeService.requireEpisode(id);
        thumbnailService.saveThumbnail(file, "episodes", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<Resource> getEpisodeThumbnail(@PathVariable Long id) {
        episodeService.requireEpisode(id);
        Path thumbnailPath = thumbnailService.getThumbnailPath("episodes", id);
        if (thumbnailPath == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Episode thumbnail not found for id: " + id);
        }
        String filename = thumbnailPath.getFileName().toString().toLowerCase();
        MediaType mediaType = filename.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(new PathResource(thumbnailPath));
    }

    @DeleteMapping("/{id}/thumbnail")
    public ResponseEntity<Void> deleteEpisodeThumbnail(@PathVariable Long id) {
        episodeService.requireEpisode(id);
        thumbnailService.deleteThumbnail("episodes", id);
        return ResponseEntity.noContent().build();
    }
}
