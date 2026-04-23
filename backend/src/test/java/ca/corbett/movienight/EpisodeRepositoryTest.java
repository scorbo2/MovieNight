package ca.corbett.movienight;

import ca.corbett.movienight.model.Episode;
import ca.corbett.movienight.model.Series;
import ca.corbett.movienight.repository.EpisodeRepository;
import ca.corbett.movienight.repository.SeriesRepository;
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

    @Autowired
    private SeriesRepository seriesRepository;

    @Test
    void saveAndFindEpisode() {
        Series dexter = new Series("Dexter", "A blood spatter analyst leads a secret life as a vigilante.");
        seriesRepository.save(dexter);
        Episode ep = new Episode(dexter, "Hungry Man", 4, 9, "Thanksgiving.", false);
        ep.setVideoFilePath("/shows/dexter/s04e09.mkv");
        episodeRepository.save(ep);

        List<Episode> all = episodeRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getSeries().getName()).isEqualTo("Dexter");
        assertThat(all.get(0).getSeries().getDescription()).contains("vigilante");
    }

    @Test
    void saveAndFindEpisodeWithTags() {
        Series dexter = new Series("Dexter");
        seriesRepository.save(dexter);
        Episode ep = new Episode(dexter, "Hungry Man", 4, 9, "Thanksgiving.", false);
        ep.setVideoFilePath("/shows/dexter/s04e09.mkv");
        ep.setTags(List.of("Drama", "Thriller", "Must-See"));
        episodeRepository.save(ep);

        List<Episode> all = episodeRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getTags()).containsExactlyInAnyOrder("drama", "thriller", "must-see");
    }

    @Test
    void tagsAreNormalizedToLowercase() {
        Series dexter = new Series("Dexter");
        seriesRepository.save(dexter);
        Episode ep = new Episode(dexter, "Ep", 1, 1, null, false);
        ep.setVideoFilePath("/shows/show/s01e01.mkv");
        ep.setTags(List.of("Drama", "DRAMA", "drama", "  Drama  "));
        episodeRepository.save(ep);

        List<Episode> all = episodeRepository.findAll();
        assertThat(all.get(0).getTags()).containsExactly("drama");
    }

    @Test
    void findByTagsContainingIgnoreCase() {
        Series showA = new Series("Show A");
        seriesRepository.save(showA);
        Episode ep1 = new Episode(showA, "Ep1", 1, 1, null, false);
        ep1.setVideoFilePath("/shows/showa/s01e01.mkv");
        ep1.setTags(List.of("drama", "award-winner"));

        Series showB = new Series("Show B");
        seriesRepository.save(showB);
        Episode ep2 = new Episode(showB, "Ep1", 1, 1, null, false);
        ep2.setVideoFilePath("/shows/showb/s01e01.mkv");
        ep2.setTags(List.of("comedy", "family"));

        episodeRepository.saveAll(List.of(ep1, ep2));

        List<Episode> results = episodeRepository.findByTagsContainingIgnoreCase("drama");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSeries().getName()).isEqualTo("Show A");
    }

    @Test
    void searchOrderedBySeasonThenEpisode() {
        Series dexter = new Series("Dexter");
        seriesRepository.save(dexter);
        Episode s4e3 = new Episode(dexter, "S4E3", 4, 3, null, false);
        s4e3.setVideoFilePath("/shows/dexter/s04e03.mkv");
        episodeRepository.save(s4e3);
        Episode s2e1 = new Episode(dexter, "S2E1", 2, 1, null, false);
        s2e1.setVideoFilePath("/shows/dexter/s02e01.mkv");
        episodeRepository.save(s2e1);
        Episode s4e1 = new Episode(dexter, "S4E1", 4, 1, null, false);
        s4e1.setVideoFilePath("/shows/dexter/s04e01.mkv");
        episodeRepository.save(s4e1);
        Episode s1e1 = new Episode(dexter, "S1E1", 1, 1, null, false);
        s1e1.setVideoFilePath("/shows/dexter/s01e01.mkv");
        episodeRepository.save(s1e1);

        Specification<Episode> spec = (root, query, cb) ->
                cb.equal(root.get("series"), dexter);
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
