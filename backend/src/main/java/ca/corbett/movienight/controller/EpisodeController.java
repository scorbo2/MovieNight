package ca.corbett.movienight.controller;

import ca.corbett.movienight.model.Episode;
import ca.corbett.movienight.service.EpisodeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/episodes")
@CrossOrigin(origins = "http://localhost:5173")
public class EpisodeController {

    private final EpisodeService episodeService;

    public EpisodeController(EpisodeService episodeService) {
        this.episodeService = episodeService;
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
}
