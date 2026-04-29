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
import ca.corbett.movienight.service.MediaService;
import ca.corbett.movienight.service.MovieService;
import ca.corbett.movienight.service.MusicVideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "movienight.prefix.movies=/movies",
        "movienight.prefix.episodes=/episodes",
        "movienight.prefix.music=/music"
})
class MediaServiceTest {

    private MovieService movieService;
    private EpisodeService episodeService;
    private MusicVideoService musicVideoService;
    private MediaService mediaService;

    @Autowired
    private SeriesRepository seriesRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private EpisodeRepository episodeRepository;

    @Autowired
    private MusicVideoRepository musicVideoRepository;

    @TempDir
    File tempDir;

    @BeforeEach
    void setUp() {
        movieService = mock(MovieService.class);
        episodeService = mock(EpisodeService.class);
        musicVideoService = mock(MusicVideoService.class);
        mediaService = new MediaService(movieService, episodeService, musicVideoService,
                                        movieRepository, episodeRepository, musicVideoRepository);
        File moviesDir = new File(tempDir, "movies");
        File episodesDir = new File(tempDir, "episodes");
        File musicDir = new File(tempDir, "music");
        moviesDir.mkdirs();
        episodesDir.mkdirs();
        musicDir.mkdirs();
        mediaService.setMovieDirectory(moviesDir.getAbsolutePath());
        mediaService.setEpisodeDirectory(episodesDir.getAbsolutePath());
        mediaService.setMusicVideoDirectory(musicDir.getAbsolutePath());
    }

    @Test
    void resolvesMovieId() {
        Genre drama = new Genre("Drama", "");
        genreRepository.save(drama);
        Movie movie = new Movie("Test Movie", 2024, drama, "A test.", null);
        movie.setVideoFilePath("test_movie.mp4");
        movie = movieRepository.save(movie);
        when(movieService.requireMovie(movie.getId())).thenReturn(movie);

        String path = mediaService.findById("M" + movie.getId());
        assertThat(path).isEqualTo(new File(tempDir, "movies/test_movie.mp4").getAbsolutePath());

        // Also verify that the side effect of the mediaService.findById set a lastWatchedDate and saved it.
        Movie actual = movieRepository.findById(movie.getId()).orElseThrow();
        assertThat(actual.getLastWatchedDate()).isNotNull();
    }

    @Test
    void resolvesEpisodeId() {
        Series series = new Series("Test Series", "A test series.");
        seriesRepository.save(series);
        Episode episode = new Episode(series, "Pilot", 1, 1, "First episode.", null);
        episode.setVideoFilePath("s01e01.mp4");
        episode = episodeRepository.save(episode);
        when(episodeService.requireEpisode(episode.getId())).thenReturn(episode);

        String path = mediaService.findById("E" + episode.getId());
        assertThat(path).isEqualTo(new File(tempDir, "episodes/s01e01.mp4").getAbsolutePath());

        // Also verify that the side effect of the mediaService.findById set a lastWatchedDate and saved it.
        Episode actual = episodeRepository.findById(episode.getId()).orElseThrow();
        assertThat(actual.getLastWatchedDate()).isNotNull();
    }

    @Test
    void resolvesMusicVideoId() {
        Artist artist = new Artist("Test Artist", "A test artist.");
        artistRepository.save(artist);
        MusicVideo musicVideo = new MusicVideo("Test Song", artist, "Test Album", 1995, "A test song.", null);
        musicVideo.setVideoFilePath("test_song.mp4");
        musicVideo = musicVideoRepository.save(musicVideo);
        when(musicVideoService.requireMusicVideo(musicVideo.getId())).thenReturn(musicVideo);

        String path = mediaService.findById("V" + musicVideo.getId());
        assertThat(path).isEqualTo(new File(tempDir, "music/test_song.mp4").getAbsolutePath());

        // Also verify that the side effect of the mediaService.findById set a lastWatchedDate and saved it.
        MusicVideo actual = musicVideoRepository.findById(musicVideo.getId()).orElseThrow();
        assertThat(actual.getLastWatchedDate()).isNotNull();
    }

    @Test
    void throwsBadRequestForNullId() {
        assertThatThrownBy(() -> mediaService.findById(null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value())
                        .isEqualTo(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    void throwsBadRequestForTooShortId() {
        assertThatThrownBy(() -> mediaService.findById("M"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value())
                        .isEqualTo(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    void throwsBadRequestForNonNumericId() {
        assertThatThrownBy(() -> mediaService.findById("Mabc"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value())
                        .isEqualTo(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    void throwsBadRequestForUnknownTypePrefix() {
        assertThatThrownBy(() -> mediaService.findById("X99"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value())
                        .isEqualTo(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    void propagatesNotFoundFromMovieService() {
        when(movieService.requireMovie(999L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found with id: 999"));

        assertThatThrownBy(() -> mediaService.findById("M999"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value())
                        .isEqualTo(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void propagatesNotFoundFromEpisodeService() {
        when(episodeService.requireEpisode(888L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Episode not found with id: 888"));

        assertThatThrownBy(() -> mediaService.findById("E888"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value())
                        .isEqualTo(HttpStatus.NOT_FOUND.value()));
    }
}
