package ca.corbett.movienight.controller;

import ca.corbett.movienight.model.MusicVideo;
import ca.corbett.movienight.service.MusicVideoService;
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
@RequestMapping("/api/music-videos")
@CrossOrigin(origins = "http://localhost:5173")
public class MusicVideoController {

    private final MusicVideoService musicVideoService;
    private final ThumbnailService thumbnailService;

    public MusicVideoController(MusicVideoService musicVideoService, ThumbnailService thumbnailService) {
        this.musicVideoService = musicVideoService;
        this.thumbnailService = thumbnailService;
    }

    @GetMapping
    public List<MusicVideo> getAllMusicVideos(@RequestParam(required = false) String title,
                                              @RequestParam(required = false) String tag,
                                              @RequestParam(required = false) Long artistId) {
        return musicVideoService.searchMusicVideos(title, tag, artistId);
    }

    @GetMapping("/{id}")
    public MusicVideo getMusicVideoById(@PathVariable Long id) {
        return musicVideoService.getMusicVideoById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Music video not found with id: " + id));
    }

    @PostMapping
    public ResponseEntity<MusicVideo> createMusicVideo(@Valid @RequestBody MusicVideo musicVideo) {
        MusicVideo saved = musicVideoService.saveMusicVideo(musicVideo);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public MusicVideo updateMusicVideo(@PathVariable Long id,
                                       @Valid @RequestBody MusicVideo musicVideo) {
        return musicVideoService.updateMusicVideo(id, musicVideo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMusicVideo(@PathVariable Long id) {
        musicVideoService.deleteMusicVideo(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/thumbnail")
    public ResponseEntity<Void> uploadMusicVideoThumbnail(@PathVariable Long id,
                                                          @RequestParam("file") MultipartFile file) {
        musicVideoService.requireMusicVideo(id);
        thumbnailService.saveThumbnail(file, "music-videos", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<Resource> getMusicVideoThumbnail(@PathVariable Long id) {
        musicVideoService.requireMusicVideo(id);
        Path thumbnailPath = thumbnailService.getThumbnailPath("music-videos", id);
        if (thumbnailPath == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Music video thumbnail not found for id: " + id);
        }
        String filename = thumbnailPath.getFileName().toString().toLowerCase();
        MediaType mediaType = filename.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(new PathResource(thumbnailPath));
    }

    @DeleteMapping("/{id}/thumbnail")
    public ResponseEntity<Void> deleteMusicVideoThumbnail(@PathVariable Long id) {
        musicVideoService.requireMusicVideo(id);
        thumbnailService.deleteThumbnail("music-videos", id);
        return ResponseEntity.noContent().build();
    }
}
