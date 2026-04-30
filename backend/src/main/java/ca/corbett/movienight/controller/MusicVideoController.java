package ca.corbett.movienight.controller;

import ca.corbett.movienight.model.MusicVideo;
import ca.corbett.movienight.service.MediaService;
import ca.corbett.movienight.service.MusicVideoService;
import ca.corbett.movienight.service.ThumbnailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
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
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/music-videos")
@CrossOrigin(origins = "http://localhost:5173")
public class MusicVideoController {

    private final MediaService mediaService;
    private final MusicVideoService musicVideoService;
    private final ThumbnailService thumbnailService;

    public MusicVideoController(MediaService mediaService, MusicVideoService musicVideoService, ThumbnailService thumbnailService) {
        this.mediaService = mediaService;
        this.musicVideoService = musicVideoService;
        this.thumbnailService = thumbnailService;
    }

    /**
     * Returns a list of all music videos that match the provided (optional) search criteria.
     */
    @GetMapping
    public List<MusicVideo> getAllMusicVideos(@RequestParam(required = false) String title,
                                              @RequestParam(required = false) String tag,
                                              @RequestParam(required = false) Long artistId) {
        return musicVideoService.searchMusicVideos(title, tag, artistId);
    }

    /**
     * Returns an m3u playlist containing all musicVideos that match the provided (optional) search criteria.
     */
    @GetMapping("/playlist")
    public ResponseEntity<String> getPlaylist(@RequestParam(required = false) String title,
                                              @RequestParam(required = false) String tag,
                                              @RequestParam(required = false) Long artistId,
                                              HttpServletRequest request) {
        List<MusicVideo> musicVideos = musicVideoService.searchMusicVideos(title, tag, artistId);
        StringBuilder m3u = new StringBuilder();
        m3u.append("#EXTM3U\n");
        for (MusicVideo musicVideo : musicVideos) {
            String filePath = mediaService.findById(Long.toString(musicVideo.getId()));
            Path videoPath = Paths.get(filePath);
            String fileName = videoPath.getFileName().toString();

            // Build the stream URL pointing back to our existing streaming endpoint:
            String streamUrl = request.getScheme() + "://" +
                    request.getServerName() + ":" +
                    request.getServerPort() +
                    "/api/stream/" + musicVideo.getId();

            // The M3U format is very straightforward:
            m3u.append("#EXTINF:-1,");
            m3u.append(fileName);
            m3u.append("\n");
            m3u.append(streamUrl);
            m3u.append("\n");
        }

        // VLC will be able to stream directly from our existing streaming endpoint:
        return ResponseEntity.ok()
                             .header(HttpHeaders.CONTENT_TYPE, "audio/x-mpegurl")
                             .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"stream.m3u\"")
                             .body(m3u.toString());
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
