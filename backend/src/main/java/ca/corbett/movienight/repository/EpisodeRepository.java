package ca.corbett.movienight.repository;

import ca.corbett.movienight.model.Episode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EpisodeRepository extends JpaRepository<Episode, Long>, JpaSpecificationExecutor<Episode> {

    List<Episode> findBySeriesNameContainingIgnoreCaseOrderBySeasonAscEpisodeAsc(String seriesName);

    List<Episode> findBySeriesNameIgnoreCaseAndSeasonOrderByEpisodeAsc(String seriesName, Integer season);

    List<Episode> findByWatched(Boolean watched);

    List<Episode> findByTagsContainingIgnoreCase(String tag);
}
