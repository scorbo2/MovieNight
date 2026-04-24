package ca.corbett.movienight;

import ca.corbett.movienight.model.Episode;
import ca.corbett.movienight.model.Genre;
import ca.corbett.movienight.model.Movie;
import ca.corbett.movienight.model.Series;
import ca.corbett.movienight.service.EpisodeService;
import ca.corbett.movienight.service.MediaService;
import ca.corbett.movienight.service.MovieService;
import ca.corbett.movienight.service.MusicVideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MediaServiceTest {

    private MovieService movieService;
    private EpisodeService episodeService;
    private MusicVideoService musicVideoService;
    private MediaService mediaService;

    @BeforeEach
    void setUp() {
        movieService = mock(MovieService.class);
        episodeService = mock(EpisodeService.class);
        musicVideoService = mock(MusicVideoService.class);
        mediaService = new MediaService(movieService, episodeService, musicVideoService);
    }

    @Test
    void resolvesMovieId() {
        Genre drama = new Genre("Drama", "");

        Movie movie = new Movie("Test Movie", 2024, drama, "A test.", false);
        movie.setVideoFilePath("/movies/test_movie.mp4");
        when(movieService.requireMovie(42L)).thenReturn(movie);

        String path = mediaService.findById("M42");
        assertThat(path).isEqualTo("/movies/test_movie.mp4");
    }

    @Test
    void resolvesEpisodeId() {
        Series series = new Series("Test Series", "A test series.");
        Episode episode = new Episode(series, "Pilot", 1, 1, "First episode.", false);
        episode.setVideoFilePath("/episodes/s01e01.mp4");
        when(episodeService.requireEpisode(7L)).thenReturn(episode);

        String path = mediaService.findById("E7");
        assertThat(path).isEqualTo("/episodes/s01e01.mp4");
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
