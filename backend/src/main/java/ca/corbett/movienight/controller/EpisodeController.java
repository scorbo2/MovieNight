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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

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
    public ResponseEntity<Episode> getEpisodeById(@PathVariable Long id) {
        return episodeService.getEpisodeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Episode> createEpisode(@Valid @RequestBody Episode episode) {
        Episode saved = episodeService.saveEpisode(episode);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Episode> updateEpisode(@PathVariable Long id,
                                                  @Valid @RequestBody Episode episode) {
        if (episodeService.getEpisodeById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Episode updated = episodeService.updateEpisode(id, episode);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEpisode(@PathVariable Long id) {
        if (episodeService.getEpisodeById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        episodeService.deleteEpisode(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/thumbnail")
    public ResponseEntity<Void> uploadEpisodeThumbnail(@PathVariable Long id,
                                                       @RequestParam("file") MultipartFile file) {
        if (episodeService.getEpisodeById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!thumbnailService.isEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        try {
            thumbnailService.saveThumbnail(file, "episodes", id);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<Resource> getEpisodeThumbnail(@PathVariable Long id) {
        if (episodeService.getEpisodeById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Path thumbnailPath = thumbnailService.getThumbnailPath("episodes", id);
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
    public ResponseEntity<Void> deleteEpisodeThumbnail(@PathVariable Long id) {
        if (episodeService.getEpisodeById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        thumbnailService.deleteThumbnail("episodes", id);
        return ResponseEntity.noContent().build();
    }
}
