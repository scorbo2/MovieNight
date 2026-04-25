package ca.corbett.movienight;

import ca.corbett.movienight.model.Episode;
import ca.corbett.movienight.model.Series;
import ca.corbett.movienight.repository.EpisodeRepository;
import ca.corbett.movienight.repository.SeriesRepository;
import ca.corbett.movienight.service.EpisodeService;
import ca.corbett.movienight.service.ThumbnailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "movienight.recently-watched-days=5" // This is ignored because we create the service ourselves here
})
class EpisodeRepositoryTest {

    @Autowired
    private EpisodeRepository episodeRepository;

    @Autowired
    private SeriesRepository seriesRepository;

    @MockBean
    private ThumbnailService thumbnailService;

    @Test
    void saveAndFindEpisode() {
        Series dexter = new Series("Dexter", "A blood spatter analyst leads a secret life as a vigilante.");
        seriesRepository.save(dexter);
        Episode ep = new Episode(dexter, "Hungry Man", 4, 9, "Thanksgiving.", null);
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
        Episode ep = new Episode(dexter, "Hungry Man", 4, 9, "Thanksgiving.", null);
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
        Episode ep = new Episode(dexter, "Ep", 1, 1, null, null);
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
        Episode ep1 = new Episode(showA, "Ep1", 1, 1, null, null);
        ep1.setVideoFilePath("/shows/showa/s01e01.mkv");
        ep1.setTags(List.of("drama", "award-winner"));

        Series showB = new Series("Show B");
        seriesRepository.save(showB);
        Episode ep2 = new Episode(showB, "Ep1", 1, 1, null, null);
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
        Episode s4e3 = new Episode(dexter, "S4E3", 4, 3, null, null);
        s4e3.setVideoFilePath("/shows/dexter/s04e03.mkv");
        episodeRepository.save(s4e3);
        Episode s2e1 = new Episode(dexter, "S2E1", 2, 1, null, null);
        s2e1.setVideoFilePath("/shows/dexter/s02e01.mkv");
        episodeRepository.save(s2e1);
        Episode s4e1 = new Episode(dexter, "S4E1", 4, 1, null, null);
        s4e1.setVideoFilePath("/shows/dexter/s04e01.mkv");
        episodeRepository.save(s4e1);
        Episode s1e1 = new Episode(dexter, "S1E1", 1, 1, null, null);
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

    @Test
    void isWatchedRecently_withRecentWatch_shouldReturnTrue() {
        EpisodeService episodeService = new EpisodeService(episodeRepository, thumbnailService);
        episodeService.setRecentlyWatchedDays(5); // ensure our threshold is 5 days for this test

        // GIVEN an episode that was watched recently:
        Series series = new Series("Test Series", "A series for testing.");
        seriesRepository.save(series);
        Episode episode = new Episode(series, "Test Episode", 1, 1, "A test episode.", LocalDate.now());
        episode.setVideoFilePath("/episodes/test_series/s01e01.mkv");
        Episode savedEpisode = episodeService.saveEpisode(episode);

        // WHEN we query for "is recently watched":
        boolean isRecentlyWatched = savedEpisode.isWatchedRecently();

        // THEN it should return true, because it's within our five-day threshold from properties:
        assertTrue(isRecentlyWatched,
                   "Expected isWatchedRecently to return true for an episode watched within the recent threshold, but it returned false.");
    }

    @Test
    void isWatchedRecently_withNullWatchedDate_shouldReturnFalse() {
        EpisodeService episodeService = new EpisodeService(episodeRepository, thumbnailService);
        episodeService.setRecentlyWatchedDays(5); // ensure our threshold is 5 days for this test

        // GIVEN an episode that has never been watched:
        Series series = new Series("Test Series", "A series for testing.");
        seriesRepository.save(series);
        Episode episode = new Episode(series, "Test Episode", 1, 1, "A test episode.", null);
        episode.setVideoFilePath("/episodes/test_series/s01e01.mkv");
        Episode savedEpisode = episodeService.saveEpisode(episode);

        // WHEN we query for "is recently watched":
        boolean isRecentlyWatched = savedEpisode.isWatchedRecently();

        // THEN it should return false, because "never been watched" means "not watched recently" by definition.
        assertFalse(isRecentlyWatched,
                    "Expected isWatchedRecently to return false for an episode that has never been watched.");
    }

    @Test
    void isWatchedRecently_withOldWatchedDate_shouldReturnFalse() {
        EpisodeService episodeService = new EpisodeService(episodeRepository, thumbnailService);
        episodeService.setRecentlyWatchedDays(5); // ensure our threshold is 5 days for this test

        // GIVEN an episode that was watched a long time ago:
        Series series = new Series("Test Series", "A series for testing.");
        seriesRepository.save(series);
        Episode episode = new Episode(series, "Test Episode", 1, 1, "A test episode.", LocalDate.now().minusDays(10));
        episode.setVideoFilePath("/episodes/test_series/s01e01.mkv");
        Episode savedEpisode = episodeService.saveEpisode(episode);

        // WHEN we query for "is recently watched":
        boolean isRecentlyWatched = savedEpisode.isWatchedRecently();

        // THEN it should return false, because it's outside our five-day threshold from properties.
        assertFalse(isRecentlyWatched,
                    "Expected isWatchedRecently to return false for an episode watched outside the recent threshold, but it returned true.");
    }
}
