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

    public EpisodeService(EpisodeRepository episodeRepository) {
        this.episodeRepository = episodeRepository;
    }

    public Optional<Episode> getEpisodeById(Long id) {
        return episodeRepository.findById(id);
    }

    public Episode saveEpisode(Episode episode) {
        return episodeRepository.save(episode);
    }

    public Episode updateEpisode(Long id, Episode updatedEpisode) {
        return episodeRepository.findById(id).map(ep -> {
            ep.setSeriesName(updatedEpisode.getSeriesName());
            ep.setEpisodeTitle(updatedEpisode.getEpisodeTitle());
            ep.setSeason(updatedEpisode.getSeason());
            ep.setEpisode(updatedEpisode.getEpisode());
            ep.setDescription(updatedEpisode.getDescription());
            ep.setWatched(updatedEpisode.getWatched());
            ep.setTags(updatedEpisode.getTags());
            return episodeRepository.save(ep);
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Episode not found with id: " + id));
    }

    public void deleteEpisode(Long id) {
        episodeRepository.deleteById(id);
    }

    public List<Episode> searchEpisodes(String seriesName, Integer season, Integer episode,
                                        Boolean watched, String tag) {
        Specification<Episode> spec = Specification.where(seriesNameContains(seriesName))
                .and(seasonEquals(season))
                .and(episodeEquals(episode))
                .and(watchedEquals(watched))
                .and(tagContains(tag));
        Sort sort = Sort.by(Sort.Direction.ASC, "season").and(Sort.by(Sort.Direction.ASC, "episode"));
        return episodeRepository.findAll(spec, sort);
    }

    private static Specification<Episode> seriesNameContains(String seriesName) {
        return (root, query, cb) -> {
            if (seriesName == null || seriesName.isBlank()) return null;
            return cb.like(cb.lower(root.get("seriesName")), "%" + seriesName.trim().toLowerCase() + "%");
        };
    }

    private static Specification<Episode> seasonEquals(Integer season) {
        return (root, query, cb) -> season == null ? null : cb.equal(root.get("season"), season);
    }

    private static Specification<Episode> episodeEquals(Integer episode) {
        return (root, query, cb) -> episode == null ? null : cb.equal(root.get("episode"), episode);
    }

    private static Specification<Episode> watchedEquals(Boolean watched) {
        return (root, query, cb) -> watched == null ? null : cb.equal(root.get("watched"), watched);
    }

    private static Specification<Episode> tagContains(String tag) {
        return (root, query, cb) -> {
            if (tag == null || tag.isBlank()) return null;
            query.distinct(true);
            Join<Episode, String> tagsJoin = root.join("tags", JoinType.INNER);
            return cb.like(cb.lower(tagsJoin.as(String.class)), "%" + tag.trim().toLowerCase() + "%");
        };
    }
}
