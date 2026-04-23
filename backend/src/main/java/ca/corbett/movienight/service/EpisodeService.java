package ca.corbett.movienight.service;

import ca.corbett.movienight.model.Episode;
import ca.corbett.movienight.repository.EpisodeRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class EpisodeService {

    private final EpisodeRepository episodeRepository;
    private final ThumbnailService thumbnailService;

    public EpisodeService(EpisodeRepository episodeRepository, ThumbnailService thumbnailService) {
        this.episodeRepository = episodeRepository;
        this.thumbnailService = thumbnailService;
    }

    public Optional<Episode> getEpisodeById(Long id) {
        return episodeRepository.findById(id).map(this::populateHasThumbnail);
    }

    public Episode saveEpisode(Episode episode) {
        return populateHasThumbnail(episodeRepository.save(episode));
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
            ep.setWatched(updatedEpisode.getWatched());
            ep.setTags(updatedEpisode.getTags());
            ep.setVideoFilePath(updatedEpisode.getVideoFilePath());
            return populateHasThumbnail(episodeRepository.save(ep));
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

    public List<Episode> searchEpisodes(Long seriesId, Integer season, Integer episode,
                                        Boolean watched, String tag) {
        Specification<Episode> spec = Specification.where(seasonEquals(season)).and(episodeEquals(episode))
                                                   .and(watchedEquals(watched)).and(tagContains(tag))
                                                   .and(seriesEquals(seriesId));
        Sort sort = Sort.by(
                Sort.Order.asc("series.name"),
                Sort.Order.asc("season").nullsLast(),
                Sort.Order.asc("episode")
        );
        return populateHasThumbnail(episodeRepository.findAll(spec, sort));
    }

    private Episode populateHasThumbnail(Episode episode) {
        episode.setHasThumbnail(thumbnailService.hasThumbnail("episodes", episode.getId()));
        return episode;
    }

    private List<Episode> populateHasThumbnail(List<Episode> episodes) {
        episodes.forEach(this::populateHasThumbnail);
        return episodes;
    }

    private static Specification<Episode> seasonEquals(Integer season) {
        return (root, query, cb) -> season == null ? null : cb.equal(root.get("season"), season);
    }

    private static Specification<Episode> seriesEquals(Long seriesId) {
        return (root, query, cb) -> seriesId == null ? null : cb.equal(root.get("series").get("id"), seriesId);
    }

    private static Specification<Episode> episodeEquals(Integer episode) {
        return (root, query, cb) -> episode == null ? null : cb.equal(root.get("episode"), episode);
    }

    private static Specification<Episode> watchedEquals(Boolean watched) {
        return (root, query, cb) -> watched == null ? null : cb.equal(root.get("watched"), watched);
    }

    private static Specification<Episode> tagContains(String tag) {
        return (root, query, cb) -> {
            if (tag == null || tag.isBlank()) { return null; }
            query.distinct(true);
            Join<Episode, String> tagsJoin = root.join("tags", JoinType.INNER);
            return cb.like(cb.lower(tagsJoin.as(String.class)), "%" + tag.trim().toLowerCase() + "%");
        };
    }
}
