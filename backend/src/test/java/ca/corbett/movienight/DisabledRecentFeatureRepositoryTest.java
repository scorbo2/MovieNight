package ca.corbett.movienight;

import ca.corbett.movienight.model.Artist;
import ca.corbett.movienight.model.Episode;
import ca.corbett.movienight.model.Genre;
import ca.corbett.movienight.model.Movie;
import ca.corbett.movienight.model.MusicVideo;
import ca.corbett.movienight.model.Series;
import ca.corbett.movienight.repository.ArtistRepository;
import ca.corbett.movienight.repository.EpisodeRepository;
import ca.corbett.movienight.repository.GenreRepository;
import ca.corbett.movienight.repository.MovieRepository;
import ca.corbett.movienight.repository.MusicVideoRepository;
import ca.corbett.movienight.repository.SeriesRepository;
import ca.corbett.movienight.service.EpisodeService;
import ca.corbett.movienight.service.MovieService;
import ca.corbett.movienight.service.MusicVideoService;
import ca.corbett.movienight.service.ThumbnailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "movienight.data-dir=",
        "movienight.recently-watched-days=0" // explicitly disables this feature
})
public class DisabledRecentFeatureRepositoryTest {

    @Autowired
    private EpisodeRepository episodeRepository;

    @Autowired
    private SeriesRepository seriesRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private MusicVideoRepository musicVideoRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @MockBean
    private ThumbnailService thumbnailService;

    @Test
    public void recentlyWatchedMovie_returnsFalse() {
        MovieService movieService = new MovieService(movieRepository, thumbnailService);
        movieService.setRecentlyWatchedDays(0); // explicitly disable the feature for this test

        // GIVEN a Movie that was recently watched:
        Genre genre = new Genre("Test Genre", "A genre for testing.");
        genreRepository.save(genre);
        Movie movie = new Movie("Test Movie", 2024, genre, "A test movie.", LocalDate.now());
        movie.setVideoFilePath("/movies/test_movie.mkv");
        Movie savedMovie = movieService.saveMovie(movie);

        // WHEN we query for "is recently watched":
        boolean isRecentlyWatched = savedMovie.isWatchedRecently();

        // THEN it should be false, because the feature is explicitly disabled:
        assertFalse(isRecentlyWatched,
                    "Expected isWatchedRecently to return false when the feature is disabled, but it returned true.");
    }

    @Test
    public void recentlyWatchedEpisode_returnsFalse() {
        EpisodeService episodeService = new EpisodeService(episodeRepository, thumbnailService);
        episodeService.setRecentlyWatchedDays(0); // explicitly disable the feature for this test

        // GIVEN an Episode that was recently watched:
        Series series = new Series("Test Series", "A series for testing.");
        seriesRepository.save(series);
        Episode episode = new Episode(series, "Test Episode", 1, 1, "A test episode.", LocalDate.now());
        episode.setVideoFilePath("/episodes/test_series/s01e01.mkv");
        Episode savedEpisode = episodeService.saveEpisode(episode);

        // WHEN we query for "is recently watched":
        boolean isRecentlyWatched = savedEpisode.isWatchedRecently();

        // THEN it should be false, because the feature is explicitly disabled:
        assertFalse(isRecentlyWatched,
                    "Expected isWatchedRecently to return false when the feature is disabled, but it returned true.");
    }

    @Test
    public void recentlyWatchedMusicVideo_returnsFalse() {
        MusicVideoService musicVideoService = new MusicVideoService(musicVideoRepository, thumbnailService);
        musicVideoService.setRecentlyWatchedDays(0); // explicitly disable the feature for this test

        // GIVEN a Music Video that was recently watched:
        Artist artist = new Artist("Test Artist", "An artist for testing.");
        artistRepository.save(artist);
        MusicVideo musicVideo = new MusicVideo("Test Music Video", artist, "album", 2024, "A test music video.",
                                               LocalDate.now());
        musicVideo.setVideoFilePath("/music-videos/test_artist/test_music_video.mkv");
        MusicVideo savedMusicVideo = musicVideoService.saveMusicVideo(musicVideo);

        // WHEN we query for "is recently watched":
        boolean isRecentlyWatched = savedMusicVideo.isWatchedRecently();

        // THEN it should be false, because the feature is explicitly disabled:
        assertFalse(isRecentlyWatched,
                    "Expected isWatchedRecently to return false when the feature is disabled, but it returned true.");
    }
}
