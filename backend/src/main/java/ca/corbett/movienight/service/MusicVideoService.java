package ca.corbett.movienight.service;

import ca.corbett.movienight.model.MusicVideo;
import ca.corbett.movienight.repository.MusicVideoRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class MusicVideoService {

    @Value("${movienight.recently-watched-days:3}")
    private int recentlyWatchedDays;

    private final MusicVideoRepository musicVideoRepository;
    private final ThumbnailService thumbnailService;

    public MusicVideoService(MusicVideoRepository musicVideoRepository, ThumbnailService thumbnailService) {
        this.musicVideoRepository = musicVideoRepository;
        this.thumbnailService = thumbnailService;
    }

    public List<MusicVideo> getAllMusicVideos() {
        return populateTransientFields(musicVideoRepository.findAll());
    }

    public Optional<MusicVideo> getMusicVideoById(Long id) {
        return musicVideoRepository.findById(id).map(this::populateTransientFields);
    }

    public MusicVideo saveMusicVideo(MusicVideo musicVideo) {
        if (musicVideo.getArtist() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Music video artist is required.");
        }
        return populateTransientFields(musicVideoRepository.save(musicVideo));
    }

    public MusicVideo updateMusicVideo(Long id, MusicVideo updatedMusicVideo) {
        if (updatedMusicVideo.getArtist() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Music video artist is required.");
        }

        return musicVideoRepository.findById(id).map(mv -> {
            mv.setTitle(updatedMusicVideo.getTitle());
            mv.setArtist(updatedMusicVideo.getArtist());
            mv.setAlbum(updatedMusicVideo.getAlbum());
            mv.setYear(updatedMusicVideo.getYear());
            mv.setDescription(updatedMusicVideo.getDescription());
            mv.setTags(updatedMusicVideo.getTags());
            mv.setVideoFilePath(updatedMusicVideo.getVideoFilePath());
            return populateTransientFields(musicVideoRepository.save(mv));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                         "Music video not found with id: " + id));
    }

    public void deleteMusicVideo(Long id) {
        requireMusicVideo(id);
        thumbnailService.deleteThumbnail("music-videos", id);
        musicVideoRepository.deleteById(id);
    }

    public MusicVideo requireMusicVideo(Long id) {
        return getMusicVideoById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                               "Music video not found with id: " + id));
    }

    public List<MusicVideo> searchMusicVideos(String title, String tag, Long artistId) {
        Specification<MusicVideo> spec = Specification.where(titleContains(title))
                .and(tagContains(tag))
                .and(artistEquals(artistId));
        Sort sort = Sort.by(
                Sort.Order.asc("artist.name"),
                Sort.Order.asc("album").nullsLast(),
                Sort.Order.asc("title").nullsLast()
        );
        return populateTransientFields(musicVideoRepository.findAll(spec, sort));
    }

    private MusicVideo populateHasThumbnail(MusicVideo musicVideo) {
        musicVideo.setHasThumbnail(thumbnailService.hasThumbnail("music-videos", musicVideo.getId()));
        return musicVideo;
    }

    private MusicVideo populateWatchedRecently(MusicVideo musicVideo) {
        int recentlyWatchedDays = Math.max(this.recentlyWatchedDays, 0);

        // This feature can be explicitly disabled by setting day count to 0.
        // It's also possible this video has never been watched.
        if (recentlyWatchedDays == 0 || musicVideo.getLastWatchedDate() == null) {
            musicVideo.setWatchedRecently(false);
            return musicVideo;
        }

        // Otherwise, do the math on the last watch date to determine if it's recent:
        musicVideo.setWatchedRecently(
                musicVideo.getLastWatchedDate().isAfter(LocalDate.now().minusDays(recentlyWatchedDays)));

        return musicVideo;
    }

    private static Specification<MusicVideo> titleContains(String title) {
        return (root, query, cb) -> {
            if (title == null || title.isBlank()) return null;
            return cb.like(cb.lower(root.get("title")), "%" + title.trim().toLowerCase() + "%");
        };
    }

    private static Specification<MusicVideo> tagContains(String tag) {
        return (root, query, cb) -> {
            if (tag == null || tag.isBlank()) return null;
            query.distinct(true);
            Join<MusicVideo, String> tagsJoin = root.join("tags", JoinType.INNER);
            return cb.like(cb.lower(tagsJoin.as(String.class)), "%" + tag.trim().toLowerCase() + "%");
        };
    }

    private static Specification<MusicVideo> artistEquals(Long artistId) {
        return (root, query, cb) -> artistId == null ? null : cb.equal(root.get("artist").get("id"), artistId);
    }

    private MusicVideo populateTransientFields(MusicVideo musicVideo) {
        populateHasThumbnail(musicVideo);
        populateWatchedRecently(musicVideo);
        return musicVideo;
    }

    private List<MusicVideo> populateTransientFields(List<MusicVideo> musicVideos) {
        musicVideos.forEach(this::populateTransientFields);
        return musicVideos;
    }

    /**
     * Visible only for testing, because Spring is dumb.
     */
    public void setRecentlyWatchedDays(int number) {
        this.recentlyWatchedDays = Math.max(number, 0);
    }
}
