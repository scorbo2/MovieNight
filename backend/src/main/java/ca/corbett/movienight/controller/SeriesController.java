package ca.corbett.movienight.controller;

import ca.corbett.movienight.model.Series;
import ca.corbett.movienight.service.SeriesService;
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
@RequestMapping("/api/series")
@CrossOrigin(origins = "http://localhost:5173")
public class SeriesController {
    private final SeriesService seriesService;
    private final ThumbnailService thumbnailService;

    public SeriesController(SeriesService seriesService, ThumbnailService thumbnailService) {
        this.seriesService = seriesService;
        this.thumbnailService = thumbnailService;
    }

    @GetMapping
    public List<Series> getAllSeries() {
        return seriesService.getAllSeries();
    }

    @GetMapping("/{id}")
    public Series getSeriesById(@PathVariable Long id) {
        return seriesService.getSeriesById(id)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                                           "Series not found with id: " + id));
    }

    @PostMapping
    public ResponseEntity<Series> createSeries(@Valid @RequestBody Series series) {
        Series saved = seriesService.saveSeries(series);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public Series updateSeries(@PathVariable Long id,
                               @Valid @RequestBody Series series) {
        return seriesService.updateSeries(id, series);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSeries(@PathVariable Long id) {
        seriesService.deleteSeries(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/thumbnail")
    public ResponseEntity<Void> uploadSeriesThumbnail(@PathVariable Long id,
                                                      @RequestParam("file") MultipartFile file) {
        seriesService.requireSeries(id);
        thumbnailService.saveThumbnail(file, "series", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<Resource> getSeriesThumbnail(@PathVariable Long id) {
        seriesService.requireSeries(id);
        Path thumbnailPath = thumbnailService.getThumbnailPath("series", id);
        if (thumbnailPath == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                              "Series thumbnail not found for id: " + id);
        }
        String filename = thumbnailPath.getFileName().toString().toLowerCase();
        MediaType mediaType = filename.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                             .contentType(mediaType)
                             .body(new PathResource(thumbnailPath));
    }

    @DeleteMapping("/{id}/thumbnail")
    public ResponseEntity<Void> deleteSeriesThumbnail(@PathVariable Long id) {
        seriesService.requireSeries(id);
        thumbnailService.deleteThumbnail("series", id);
        return ResponseEntity.noContent().build();
    }

}
