package ca.corbett.movienight.repository;

import ca.corbett.movienight.model.Episode;
import ca.corbett.movienight.model.Series;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EpisodeRepository extends JpaRepository<Episode, Long>, JpaSpecificationExecutor<Episode> {

    List<Episode> findBySeriesOrderBySeasonAscEpisodeAsc(Series series);

    List<Episode> findByEpisodeTitleContainingIgnoreCase(String episodeTitle);

    List<Episode> findByTagsContainingIgnoreCase(String tag);

    long countBySeries(Series series);
}
