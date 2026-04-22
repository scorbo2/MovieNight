package ca.corbett.movienight;

import ca.corbett.movienight.model.Genre;
import ca.corbett.movienight.model.Movie;
import ca.corbett.movienight.repository.GenreRepository;
import ca.corbett.movienight.repository.MovieRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
class MovieRepositoryTest {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private GenreRepository genreRepository;

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
        Movie movie = new Movie("Test Movie", 2024, drama, "A test movie.", false);
        movie.setVideoFilePath("/movies/test_movie.mkv");
        movieRepository.save(movie);

        List<Movie> all = movieRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getTitle()).isEqualTo("Test Movie");
    }

    @Test
    void findByTitleContainingIgnoreCase() {
        Movie inception = new Movie("Inception", 2010, sciFi, "Dream heist.", false);
        inception.setVideoFilePath("/movies/inception.mkv");
        movieRepository.save(inception);
        Movie interstellar = new Movie("Interstellar", 2014, sciFi, "Space travel.", false);
        interstellar.setVideoFilePath("/movies/interstellar.mkv");
        movieRepository.save(interstellar);

        List<Movie> results = movieRepository.findByTitleContainingIgnoreCase("inter");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Interstellar");
    }

    @Test
    void findByWatched() {
        Movie watchedMovie = new Movie("Watched Movie", 2020, comedy, "Seen it.", true);
        watchedMovie.setVideoFilePath("/movies/watched_movie.mkv");
        movieRepository.save(watchedMovie);
        Movie unwatchedMovie = new Movie("Unwatched Movie", 2021, thriller, "Not yet.", false);
        unwatchedMovie.setVideoFilePath("/movies/unwatched_movie.mkv");
        movieRepository.save(unwatchedMovie);

        List<Movie> watched = movieRepository.findByWatched(true);
        assertThat(watched).hasSize(1);
        assertThat(watched.get(0).getTitle()).isEqualTo("Watched Movie");
    }

    @Test
    void saveAndFindMovieWithTags() {
        Movie movie = new Movie("Tagged Movie", 2023, action, "Has tags.", false);
        movie.setVideoFilePath("/movies/tagged_movie.mkv");
        movie.setTags(List.of("Action", "Thriller", "Must-See"));
        movieRepository.save(movie);

        List<Movie> all = movieRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getTags()).containsExactlyInAnyOrder("action", "thriller", "must-see");
    }

    @Test
    void findByTagsContainingIgnoreCase() {
        Movie movie1 = new Movie("Movie One", 2021, drama, "First.", false);
        movie1.setVideoFilePath("/movies/movie_one.mkv");
        movie1.setTags(List.of("drama", "award-winner"));

        Movie movie2 = new Movie("Movie Two", 2022, comedy, "Second.", false);
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
        Movie movie = new Movie("Case Test", 2020, action, "Test normalization.", false);
        movie.setVideoFilePath("/movies/case_test.mkv");
        movie.setTags(List.of("Action", "ACTION", "action", "  Action  "));
        movieRepository.save(movie);

        List<Movie> all = movieRepository.findAll();
        assertThat(all.get(0).getTags()).containsExactly("action");
    }

    @Test
    void combinedFilterTitleAndWatchedAndTag() {
        Movie m1 = new Movie("Action Hero", 2020, action, "Hero stuff.", true);
        m1.setVideoFilePath("/movies/action_hero.mkv");
        m1.setTags(List.of("action", "blockbuster"));

        Movie m2 = new Movie("Action Zero", 2021, action, "Zero stuff.", false);
        m2.setVideoFilePath("/movies/action_zero.mkv");
        m2.setTags(List.of("action", "blockbuster"));

        Movie m3 = new Movie("Comedy King", 2022, comedy, "Funny stuff.", true);
        m3.setVideoFilePath("/movies/comedy_king.mkv");
        m3.setTags(List.of("comedy"));

        movieRepository.saveAll(List.of(m1, m2, m3));

        // title contains "action", watched=true, tag contains "blockbuster" → only m1
        Specification<Movie> spec = Specification
                .<Movie>where((root, query, cb) ->
                        cb.like(cb.lower(root.get("title")), "%action%"))
                .and((root, query, cb) ->
                        cb.equal(root.get("watched"), true))
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
        Movie m1 = new Movie("Movie 1", 2020, drama, "Drama movie.", false);
        m1.setVideoFilePath("/movies/movie1.mkv");
        Movie m2 = new Movie("Movie 2", 2021, drama, "Another drama.", false);
        m2.setVideoFilePath("/movies/movie2.mkv");
        Movie m3 = new Movie("Movie 3", 2022, comedy, "Comedy movie.", false);
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
        Movie m1 = new Movie("Movie 1", 2020, drama, "Drama movie.", false);
        m1.setVideoFilePath("/movies/movie1.mkv");
        Movie m2 = new Movie("Movie 2", 2021, drama, "Another drama.", false);
        m2.setVideoFilePath("/movies/movie2.mkv");
        Movie m3 = new Movie("Movie 3", 2022, comedy, "Comedy movie.", false);
        m3.setVideoFilePath("/movies/movie3.mkv");
        movieRepository.saveAll(List.of(m1, m2, m3));

        List<Movie> dramas = movieRepository.findByGenre(drama);
        List<Movie> comedies = movieRepository.findByGenre(comedy);
        List<Movie> sciFis = movieRepository.findByGenre(sciFi);

        assertThat(dramas).hasSize(2).extracting(Movie::getTitle).containsExactlyInAnyOrder("Movie 1", "Movie 2");
        assertThat(comedies).hasSize(1).extracting(Movie::getTitle).containsExactly("Movie 3");
        assertThat(sciFis).isEmpty();
    }
}
