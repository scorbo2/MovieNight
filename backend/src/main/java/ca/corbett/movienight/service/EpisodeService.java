package ca.corbett.movienight.service;

import ca.corbett.movienight.model.Episode;
import ca.corbett.movienight.repository.EpisodeRepository;
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
public class EpisodeService {

    @Value("${movienight.recently-watched-days:3}")
    private int recentlyWatchedDays;

    private final EpisodeRepository episodeRepository;
    private final ThumbnailService thumbnailService;

    public EpisodeService(EpisodeRepository episodeRepository, ThumbnailService thumbnailService) {
        this.episodeRepository = episodeRepository;
        this.thumbnailService = thumbnailService;
    }

    public Optional<Episode> getEpisodeById(Long id) {
        return episodeRepository.findById(id).map(this::populateTransientFields);
    }

    public Episode saveEpisode(Episode episode) {
        return populateTransientFields(episodeRepository.save(episode));
    }

    public Episode updateEpisode(Long id, Episode updatedEpisode) {
        // Reject request if the episode's series is null:
        if (updatedEpisode.getSeries() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Episode series is required.");
        }

        return episodeRepository.findById(id).map(ep -> {
            ep.setSeries(updatedEpisode.getSeries());
            ep.setEpisodeTitle(updatedEpisode.getEpisodeTitle());
            ep.setSeason(updatedEpisode.getSeason());
            ep.setEpisode(updatedEpisode.getEpisode());
            ep.setDescription(updatedEpisode.getDescription());
            ep.setTags(updatedEpisode.getTags());
            ep.setVideoFilePath(updatedEpisode.getVideoFilePath());
            return populateTransientFields(episodeRepository.save(ep));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Episode not found with id: " + id));
    }

    public void deleteEpisode(Long id) {
        requireEpisode(id);
        thumbnailService.deleteThumbnail("episodes", id);
        episodeRepository.deleteById(id);
    }

    public Episode requireEpisode(Long id) {
        return getEpisodeById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                               "Episode not found with id: " + id));
    }

    public List<Episode> searchEpisodes(Long seriesId, String seriesName, Integer season, Integer episode, String tag) {
        Specification<Episode> spec = Specification.where(seasonEquals(season))
                                                   .and(episodeEquals(episode))
                                                   .and(tagContains(tag))
                                                   .and(seriesEquals(seriesId))
                                                   .and(seriesNameContains(seriesName));
        Sort sort = Sort.by(
                Sort.Order.asc("series.name"),
                Sort.Order.asc("season").nullsLast(),
                Sort.Order.asc("episode").nullsLast(),
                Sort.Order.asc("episodeTitle").nullsLast()
        );
        return populateTransientFields(episodeRepository.findAll(spec, sort));
    }

    private Episode populateHasThumbnail(Episode episode) {
        episode.setHasThumbnail(thumbnailService.hasThumbnail("episodes", episode.getId()));
        return episode;
    }

    private Episode populateWatchedRecently(Episode episode) {
        int recentlyWatchedDays = Math.max(this.recentlyWatchedDays, 0);
        
        // This feature can be explicitly disabled by setting day count to 0.
        // It's also possible this video has never been watched.
        if (recentlyWatchedDays == 0 || episode.getLastWatchedDate() == null) {
            episode.setWatchedRecently(false);
            return episode;
        }

        // Otherwise, do the math on the last watch date to determine if it's recent:
        episode.setWatchedRecently(
                episode.getLastWatchedDate().isAfter(LocalDate.now().minusDays(recentlyWatchedDays)));

        return episode;
    }

    private static Specification<Episode> seasonEquals(Integer season) {
        return (root, query, cb) -> season == null ? null : cb.equal(root.get("season"), season);
    }

    private static Specification<Episode> seriesEquals(Long seriesId) {
        return (root, query, cb) -> seriesId == null ? null : cb.equal(root.get("series").get("id"), seriesId);
    }

    private static Specification<Episode> seriesNameContains(String seriesName) {
        return (root, query, cb) -> {
            if (seriesName == null || seriesName.isBlank()) return null;
            return cb.like(cb.lower(root.get("series").get("name")),
                           "%" + seriesName.trim().toLowerCase() + "%");
        };
    }

    private static Specification<Episode> episodeEquals(Integer episode) {
        return (root, query, cb) -> episode == null ? null : cb.equal(root.get("episode"), episode);
    }

    private static Specification<Episode> tagContains(String tag) {
        return (root, query, cb) -> {
            if (tag == null || tag.isBlank()) { return null; }
            query.distinct(true);
            Join<Episode, String> tagsJoin = root.join("tags", JoinType.INNER);
            return cb.like(cb.lower(tagsJoin.as(String.class)), "%" + tag.trim().toLowerCase() + "%");
        };
    }

    private Episode populateTransientFields(Episode episode) {
        populateHasThumbnail(episode);
        populateWatchedRecently(episode);
        return episode;
    }

    private List<Episode> populateTransientFields(List<Episode> episodes) {
        episodes.forEach(this::populateTransientFields);
        return episodes;
    }

    /**
     * Visible only for testing, because Spring is dumb.
     */
    public void setRecentlyWatchedDays(int number) {
        this.recentlyWatchedDays = Math.max(number, 0);
    }
}
