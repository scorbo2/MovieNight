package ca.corbett.movienight;

import ca.corbett.movienight.model.Episode;
import ca.corbett.movienight.repository.EpisodeRepository;
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
        ep.setVideoFilePath("/shows/dexter/s04e09.mkv");
        episodeRepository.save(ep);

        List<Episode> all = episodeRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getSeriesName()).isEqualTo("Dexter");
    }

    @Test
    void findBySeriesNameContainingIgnoreCase() {
        Episode dexterPilot = new Episode("Dexter", "Pilot", 1, 1, "Dexter pilot.", false);
        dexterPilot.setVideoFilePath("/shows/dexter/s01e01.mkv");
        episodeRepository.save(dexterPilot);
        Episode bbPilot = new Episode("Breaking Bad", "Pilot", 1, 1, "BB pilot.", false);
        bbPilot.setVideoFilePath("/shows/breakingbad/s01e01.mkv");
        episodeRepository.save(bbPilot);

        List<Episode> results = episodeRepository
                .findBySeriesNameContainingIgnoreCaseOrderBySeasonAscEpisodeAsc("dexter");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSeriesName()).isEqualTo("Dexter");
    }

    @Test
    void findBySeriesNameAndSeasonOrderedByEpisode() {
        Episode ep3 = new Episode("Dexter", "Ep 3", 4, 3, null, false);
        ep3.setVideoFilePath("/shows/dexter/s04e03.mkv");
        episodeRepository.save(ep3);
        Episode ep1 = new Episode("Dexter", "Ep 1", 4, 1, null, false);
        ep1.setVideoFilePath("/shows/dexter/s04e01.mkv");
        episodeRepository.save(ep1);
        Episode ep2 = new Episode("Dexter", "Ep 2", 4, 2, null, false);
        ep2.setVideoFilePath("/shows/dexter/s04e02.mkv");
        episodeRepository.save(ep2);
        Episode s3ep1 = new Episode("Dexter", "S3Ep1", 3, 1, null, false);
        s3ep1.setVideoFilePath("/shows/dexter/s03e01.mkv");
        episodeRepository.save(s3ep1);

        List<Episode> results = episodeRepository
                .findBySeriesNameIgnoreCaseAndSeasonOrderByEpisodeAsc("Dexter", 4);
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getEpisode()).isEqualTo(1);
        assertThat(results.get(1).getEpisode()).isEqualTo(2);
        assertThat(results.get(2).getEpisode()).isEqualTo(3);
    }

    @Test
    void findByWatched() {
        Episode watchedEp = new Episode("Dexter", "Watched Ep", 1, 1, null, true);
        watchedEp.setVideoFilePath("/shows/dexter/s01e01.mkv");
        episodeRepository.save(watchedEp);
        Episode unwatchedEp = new Episode("Dexter", "Unwatched Ep", 1, 2, null, false);
        unwatchedEp.setVideoFilePath("/shows/dexter/s01e02.mkv");
        episodeRepository.save(unwatchedEp);

        List<Episode> watched = episodeRepository.findByWatched(true);
        assertThat(watched).hasSize(1);
        assertThat(watched.get(0).getEpisodeTitle()).isEqualTo("Watched Ep");
    }

    @Test
    void saveAndFindEpisodeWithTags() {
        Episode ep = new Episode("Dexter", "Hungry Man", 4, 9, "Thanksgiving.", false);
        ep.setVideoFilePath("/shows/dexter/s04e09.mkv");
        ep.setTags(List.of("Drama", "Thriller", "Must-See"));
        episodeRepository.save(ep);

        List<Episode> all = episodeRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getTags()).containsExactlyInAnyOrder("drama", "thriller", "must-see");
    }

    @Test
    void tagsAreNormalizedToLowercase() {
        Episode ep = new Episode("Show", "Ep", 1, 1, null, false);
        ep.setVideoFilePath("/shows/show/s01e01.mkv");
        ep.setTags(List.of("Drama", "DRAMA", "drama", "  Drama  "));
        episodeRepository.save(ep);

        List<Episode> all = episodeRepository.findAll();
        assertThat(all.get(0).getTags()).containsExactly("drama");
    }

    @Test
    void findByTagsContainingIgnoreCase() {
        Episode ep1 = new Episode("Show A", "Ep1", 1, 1, null, false);
        ep1.setVideoFilePath("/shows/showa/s01e01.mkv");
        ep1.setTags(List.of("drama", "award-winner"));

        Episode ep2 = new Episode("Show B", "Ep1", 1, 1, null, false);
        ep2.setVideoFilePath("/shows/showb/s01e01.mkv");
        ep2.setTags(List.of("comedy", "family"));

        episodeRepository.saveAll(List.of(ep1, ep2));

        List<Episode> results = episodeRepository.findByTagsContainingIgnoreCase("drama");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSeriesName()).isEqualTo("Show A");
    }

    @Test
    void searchOrderedBySeasonThenEpisode() {
        Episode s4e3 = new Episode("Dexter", "S4E3", 4, 3, null, false);
        s4e3.setVideoFilePath("/shows/dexter/s04e03.mkv");
        episodeRepository.save(s4e3);
        Episode s2e1 = new Episode("Dexter", "S2E1", 2, 1, null, false);
        s2e1.setVideoFilePath("/shows/dexter/s02e01.mkv");
        episodeRepository.save(s2e1);
        Episode s4e1 = new Episode("Dexter", "S4E1", 4, 1, null, false);
        s4e1.setVideoFilePath("/shows/dexter/s04e01.mkv");
        episodeRepository.save(s4e1);
        Episode s1e1 = new Episode("Dexter", "S1E1", 1, 1, null, false);
        s1e1.setVideoFilePath("/shows/dexter/s01e01.mkv");
        episodeRepository.save(s1e1);

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
