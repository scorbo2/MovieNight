package ca.corbett.movienight;

import ca.corbett.movienight.model.Genre;
import ca.corbett.movienight.model.Movie;
import ca.corbett.movienight.repository.GenreRepository;
import ca.corbett.movienight.repository.MovieRepository;
import ca.corbett.movienight.service.MovieService;
import ca.corbett.movienight.service.ThumbnailService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
class MovieRepositoryTest {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private GenreRepository genreRepository;

    @MockBean
    private ThumbnailService thumbnailService;

    private Genre drama;
    private Genre comedy;
    private Genre sciFi;
    private Genre thriller;
    private Genre action;

    @BeforeEach
    public void setup() {
        drama = new Genre("Drama", "Dramatic movies.");
        comedy = new Genre("Comedy", "Funny movies.");
        sciFi = new Genre("Sci-Fi", "Science fiction movies.");
        thriller = new Genre("Thriller", "Thrilling movies.");
        action = new Genre("Action", "Action-packed movies.");
        genreRepository.save(drama);
        genreRepository.save(comedy);
        genreRepository.save(sciFi);
        genreRepository.save(thriller);
        genreRepository.save(action);
    }

    @Test
    void saveAndFindMovie() {
        Movie movie = new Movie("Test Movie", 2024, drama, "A test movie.", null);
        movie.setVideoFilePath("/movies/test_movie.mkv");
        movieRepository.save(movie);

        List<Movie> all = movieRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getTitle()).isEqualTo("Test Movie");
    }

    @Test
    void findByTitleContainingIgnoreCase() {
        Movie inception = new Movie("Inception", 2010, sciFi, "Dream heist.", null);
        inception.setVideoFilePath("/movies/inception.mkv");
        movieRepository.save(inception);
        Movie interstellar = new Movie("Interstellar", 2014, sciFi, "Space travel.", null);
        interstellar.setVideoFilePath("/movies/interstellar.mkv");
        movieRepository.save(interstellar);

        List<Movie> results = movieRepository.findByTitleContainingIgnoreCase("inter");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Interstellar");
    }

    @Test
    void saveAndFindMovieWithTags() {
        Movie movie = new Movie("Tagged Movie", 2023, action, "Has tags.", null);
        movie.setVideoFilePath("/movies/tagged_movie.mkv");
        movie.setTags(List.of("Action", "Thriller", "Must-See"));
        movieRepository.save(movie);

        List<Movie> all = movieRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getTags()).containsExactlyInAnyOrder("action", "thriller", "must-see");
    }

    @Test
    void findByTagsContainingIgnoreCase() {
        Movie movie1 = new Movie("Movie One", 2021, drama, "First.", null);
        movie1.setVideoFilePath("/movies/movie_one.mkv");
        movie1.setTags(List.of("drama", "award-winner"));

        Movie movie2 = new Movie("Movie Two", 2022, comedy, "Second.", null);
        movie2.setVideoFilePath("/movies/movie_two.mkv");
        movie2.setTags(List.of("comedy", "family"));

        movieRepository.save(movie1);
        movieRepository.save(movie2);

        List<Movie> results = movieRepository.findByTagsContainingIgnoreCase("drama");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Movie One");
    }

    @Test
    void tagsAreNormalizedToLowercase() {
        Movie movie = new Movie("Case Test", 2020, action, "Test normalization.", null);
        movie.setVideoFilePath("/movies/case_test.mkv");
        movie.setTags(List.of("Action", "ACTION", "action", "  Action  "));
        movieRepository.save(movie);

        List<Movie> all = movieRepository.findAll();
        assertThat(all.get(0).getTags()).containsExactly("action");
    }

    @Test
    void combinedFilterTitleAndTag() {
        Movie m1 = new Movie("Action Hero", 2020, action, "Hero stuff.", null);
        m1.setVideoFilePath("/movies/action_hero.mkv");
        m1.setTags(List.of("action", "blockbuster"));

        Movie m2 = new Movie("Action Zero", 2021, action, "Zero stuff.", null);
        m2.setVideoFilePath("/movies/action_zero.mkv");
        m2.setTags(List.of("action"));

        Movie m3 = new Movie("Comedy King", 2022, comedy, "Funny stuff.", null);
        m3.setVideoFilePath("/movies/comedy_king.mkv");
        m3.setTags(List.of("comedy"));

        movieRepository.saveAll(List.of(m1, m2, m3));

        // title contains "action", tag contains "blockbuster" → only m1
        Specification<Movie> spec = Specification
                .<Movie>where((root, query, cb) ->
                        cb.like(cb.lower(root.get("title")), "%action%"))
                .and((root, query, cb) -> {
                    query.distinct(true);
                    Join<Movie, String> tagsJoin = root.join("tags", JoinType.INNER);
                    return cb.like(cb.lower(tagsJoin.as(String.class)), "%blockbuster%");
                });

        List<Movie> results = movieRepository.findAll(spec);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Action Hero");
    }

    @Test
    void countByGenre() {
        Movie m1 = new Movie("Movie 1", 2020, drama, "Drama movie.", null);
        m1.setVideoFilePath("/movies/movie1.mkv");
        Movie m2 = new Movie("Movie 2", 2021, drama, "Another drama.", null);
        m2.setVideoFilePath("/movies/movie2.mkv");
        Movie m3 = new Movie("Movie 3", 2022, comedy, "Comedy movie.", null);
        m3.setVideoFilePath("/movies/movie3.mkv");
        movieRepository.saveAll(List.of(m1, m2, m3));

        long dramaCount = movieRepository.countByGenre(drama);
        long comedyCount = movieRepository.countByGenre(comedy);
        long sciFiCount = movieRepository.countByGenre(sciFi);

        assertThat(dramaCount).isEqualTo(2);
        assertThat(comedyCount).isEqualTo(1);
        assertThat(sciFiCount).isEqualTo(0);
    }

    @Test
    void findByGenre() {
        Movie m1 = new Movie("Movie 1", 2020, drama, "Drama movie.", null);
        m1.setVideoFilePath("/movies/movie1.mkv");
        Movie m2 = new Movie("Movie 2", 2021, drama, "Another drama.", null);
        m2.setVideoFilePath("/movies/movie2.mkv");
        Movie m3 = new Movie("Movie 3", 2022, comedy, "Comedy movie.", null);
        m3.setVideoFilePath("/movies/movie3.mkv");
        movieRepository.saveAll(List.of(m1, m2, m3));

        List<Movie> dramas = movieRepository.findByGenre(drama);
        List<Movie> comedies = movieRepository.findByGenre(comedy);
        List<Movie> sciFis = movieRepository.findByGenre(sciFi);

        assertThat(dramas).hasSize(2).extracting(Movie::getTitle).containsExactlyInAnyOrder("Movie 1", "Movie 2");
        assertThat(comedies).hasSize(1).extracting(Movie::getTitle).containsExactly("Movie 3");
        assertThat(sciFis).isEmpty();
    }

    @Test
    void isWatchedRecently_withRecentWatch_shouldReturnTrue() {
        MovieService movieService = new MovieService(movieRepository, thumbnailService);
        movieService.setRecentlyWatchedDays(5); // ensure our threshold is 5 days for this test

        // GIVEN a movie that was watched recently:
        Genre genre = new Genre("Test Genre", "A genre for testing.");
        genreRepository.save(genre);
        Movie movie = new Movie("Test Movie", 2024, genre, "A test movie.", LocalDate.now().minusDays(2));
        movie.setVideoFilePath("/movies/test_movie.mkv");
        Movie savedMovie = movieService.saveMovie(movie);

        // WHEN we query for "is recently watched":
        boolean isRecentlyWatched = savedMovie.isWatchedRecently();

        // THEN it should return true, because it's within our five-day threshold from properties:
        assertTrue(isRecentlyWatched,
                   "Expected isWatchedRecently to return true for a movie watched within the recent threshold, but it returned false.");
    }

    @Test
    void isWatchedRecently_withNullWatchedDate_shouldReturnFalse() {
        MovieService movieService = new MovieService(movieRepository, thumbnailService);
        movieService.setRecentlyWatchedDays(5); // ensure our threshold is 5 days for this test

        // GIVEN a movie that has never been watched:
        Genre genre = new Genre("Test Genre", "A genre for testing.");
        genreRepository.save(genre);
        Movie movie = new Movie("Test Movie", 2024, genre, "A test movie.", null);
        movie.setVideoFilePath("/movies/test_movie.mkv");
        Movie savedMovie = movieService.saveMovie(movie);

        // WHEN we query for "is recently watched":
        boolean isRecentlyWatched = savedMovie.isWatchedRecently();

        // THEN it should return false, because "never been watched" means "not watched recently" by definition.
        assertFalse(isRecentlyWatched,
                    "Expected isWatchedRecently to return false for a movie that has never been watched.");
    }

    @Test
    void isWatchedRecently_withOldWatchedDate_shouldReturnFalse() {
        MovieService movieService = new MovieService(movieRepository, thumbnailService);
        movieService.setRecentlyWatchedDays(5); // ensure our threshold is 5 days for this test

        // GIVEN a movie that was watched a long time ago:
        Genre genre = new Genre("Test Genre", "A genre for testing.");
        genreRepository.save(genre);
        Movie movie = new Movie("Test Movie", 2024, genre, "A test movie.", LocalDate.now().minusDays(10));
        movie.setVideoFilePath("/movies/test_movie.mkv");
        Movie savedMovie = movieService.saveMovie(movie);

        // WHEN we query for "is recently watched":
        boolean isRecentlyWatched = savedMovie.isWatchedRecently();

        // THEN it should return false, because it's outside our five-day threshold from properties.
        assertFalse(isRecentlyWatched,
                    "Expected isWatchedRecently to return false for a movie watched outside the recent threshold, but it returned true.");
    }
}
