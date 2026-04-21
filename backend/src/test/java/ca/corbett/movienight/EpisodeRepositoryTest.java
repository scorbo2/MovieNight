package ca.corbett.movienight;

import ca.corbett.movienight.model.Episode;
import ca.corbett.movienight.repository.EpisodeRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class EpisodeRepositoryTest {

    @Autowired
    private EpisodeRepository episodeRepository;

    @Test
    void saveAndFindEpisode() {
        Episode ep = new Episode("Dexter", "Hungry Man", 4, 9, "Thanksgiving.", false);
        episodeRepository.save(ep);

        List<Episode> all = episodeRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getSeriesName()).isEqualTo("Dexter");
    }

    @Test
    void findBySeriesNameContainingIgnoreCase() {
        episodeRepository.save(new Episode("Dexter", "Pilot", 1, 1, "Dexter pilot.", false));
        episodeRepository.save(new Episode("Breaking Bad", "Pilot", 1, 1, "BB pilot.", false));

        List<Episode> results = episodeRepository
                .findBySeriesNameContainingIgnoreCaseOrderBySeasonAscEpisodeAsc("dexter");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSeriesName()).isEqualTo("Dexter");
    }

    @Test
    void findBySeriesNameAndSeasonOrderedByEpisode() {
        episodeRepository.save(new Episode("Dexter", "Ep 3", 4, 3, null, false));
        episodeRepository.save(new Episode("Dexter", "Ep 1", 4, 1, null, false));
        episodeRepository.save(new Episode("Dexter", "Ep 2", 4, 2, null, false));
        episodeRepository.save(new Episode("Dexter", "S3Ep1", 3, 1, null, false));

        List<Episode> results = episodeRepository
                .findBySeriesNameIgnoreCaseAndSeasonOrderByEpisodeAsc("Dexter", 4);
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getEpisode()).isEqualTo(1);
        assertThat(results.get(1).getEpisode()).isEqualTo(2);
        assertThat(results.get(2).getEpisode()).isEqualTo(3);
    }

    @Test
    void findByWatched() {
        episodeRepository.save(new Episode("Dexter", "Watched Ep", 1, 1, null, true));
        episodeRepository.save(new Episode("Dexter", "Unwatched Ep", 1, 2, null, false));

        List<Episode> watched = episodeRepository.findByWatched(true);
        assertThat(watched).hasSize(1);
        assertThat(watched.get(0).getEpisodeTitle()).isEqualTo("Watched Ep");
    }

    @Test
    void saveAndFindEpisodeWithTags() {
        Episode ep = new Episode("Dexter", "Hungry Man", 4, 9, "Thanksgiving.", false);
        ep.setTags(List.of("Drama", "Thriller", "Must-See"));
        episodeRepository.save(ep);

        List<Episode> all = episodeRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getTags()).containsExactlyInAnyOrder("drama", "thriller", "must-see");
    }

    @Test
    void tagsAreNormalizedToLowercase() {
        Episode ep = new Episode("Show", "Ep", 1, 1, null, false);
        ep.setTags(List.of("Drama", "DRAMA", "drama", "  Drama  "));
        episodeRepository.save(ep);

        List<Episode> all = episodeRepository.findAll();
        assertThat(all.get(0).getTags()).containsExactly("drama");
    }

    @Test
    void findByTagsContainingIgnoreCase() {
        Episode ep1 = new Episode("Show A", "Ep1", 1, 1, null, false);
        ep1.setTags(List.of("drama", "award-winner"));

        Episode ep2 = new Episode("Show B", "Ep1", 1, 1, null, false);
        ep2.setTags(List.of("comedy", "family"));

        episodeRepository.saveAll(List.of(ep1, ep2));

        List<Episode> results = episodeRepository.findByTagsContainingIgnoreCase("drama");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSeriesName()).isEqualTo("Show A");
    }

    @Test
    void searchOrderedBySeasonThenEpisode() {
        episodeRepository.save(new Episode("Dexter", "S4E3", 4, 3, null, false));
        episodeRepository.save(new Episode("Dexter", "S2E1", 2, 1, null, false));
        episodeRepository.save(new Episode("Dexter", "S4E1", 4, 1, null, false));
        episodeRepository.save(new Episode("Dexter", "S1E1", 1, 1, null, false));

        Specification<Episode> spec = (root, query, cb) ->
                cb.like(cb.lower(root.get("seriesName")), "%dexter%");
        Sort sort = Sort.by(Sort.Direction.ASC, "season")
                .and(Sort.by(Sort.Direction.ASC, "episode"));

        List<Episode> results = episodeRepository.findAll(spec, sort);
        assertThat(results).hasSize(4);
        assertThat(results.get(0).getEpisodeTitle()).isEqualTo("S1E1");
        assertThat(results.get(1).getEpisodeTitle()).isEqualTo("S2E1");
        assertThat(results.get(2).getEpisodeTitle()).isEqualTo("S4E1");
        assertThat(results.get(3).getEpisodeTitle()).isEqualTo("S4E3");
    }
}
