package ca.corbett.movienight;

import ca.corbett.movienight.model.Movie;
import ca.corbett.movienight.repository.MovieRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
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

    @Test
    void saveAndFindMovie() {
        Movie movie = new Movie("Test Movie", 2024, "Drama", "A test movie.", false);
        movie.setVideoFilePath("/movies/test_movie.mkv");
        movieRepository.save(movie);

        List<Movie> all = movieRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getTitle()).isEqualTo("Test Movie");
    }

    @Test
    void findByTitleContainingIgnoreCase() {
        Movie inception = new Movie("Inception", 2010, "Sci-Fi", "Dream heist.", false);
        inception.setVideoFilePath("/movies/inception.mkv");
        movieRepository.save(inception);
        Movie interstellar = new Movie("Interstellar", 2014, "Sci-Fi", "Space travel.", false);
        interstellar.setVideoFilePath("/movies/interstellar.mkv");
        movieRepository.save(interstellar);

        List<Movie> results = movieRepository.findByTitleContainingIgnoreCase("inter");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Interstellar");
    }

    @Test
    void findByWatched() {
        Movie watchedMovie = new Movie("Watched Movie", 2020, "Comedy", "Seen it.", true);
        watchedMovie.setVideoFilePath("/movies/watched_movie.mkv");
        movieRepository.save(watchedMovie);
        Movie unwatchedMovie = new Movie("Unwatched Movie", 2021, "Thriller", "Not yet.", false);
        unwatchedMovie.setVideoFilePath("/movies/unwatched_movie.mkv");
        movieRepository.save(unwatchedMovie);

        List<Movie> watched = movieRepository.findByWatched(true);
        assertThat(watched).hasSize(1);
        assertThat(watched.get(0).getTitle()).isEqualTo("Watched Movie");
    }

    @Test
    void saveAndFindMovieWithTags() {
        Movie movie = new Movie("Tagged Movie", 2023, "Action", "Has tags.", false);
        movie.setVideoFilePath("/movies/tagged_movie.mkv");
        movie.setTags(List.of("Action", "Thriller", "Must-See"));
        movieRepository.save(movie);

        List<Movie> all = movieRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getTags()).containsExactlyInAnyOrder("action", "thriller", "must-see");
    }

    @Test
    void findByTagsContainingIgnoreCase() {
        Movie movie1 = new Movie("Movie One", 2021, "Drama", "First.", false);
        movie1.setVideoFilePath("/movies/movie_one.mkv");
        movie1.setTags(List.of("drama", "award-winner"));

        Movie movie2 = new Movie("Movie Two", 2022, "Comedy", "Second.", false);
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
        Movie movie = new Movie("Case Test", 2020, "Action", "Test normalization.", false);
        movie.setVideoFilePath("/movies/case_test.mkv");
        movie.setTags(List.of("Action", "ACTION", "action", "  Action  "));
        movieRepository.save(movie);

        List<Movie> all = movieRepository.findAll();
        assertThat(all.get(0).getTags()).containsExactly("action");
    }

    @Test
    void combinedFilterTitleAndWatchedAndTag() {
        Movie m1 = new Movie("Action Hero", 2020, "Action", "Hero stuff.", true);
        m1.setVideoFilePath("/movies/action_hero.mkv");
        m1.setTags(List.of("action", "blockbuster"));

        Movie m2 = new Movie("Action Zero", 2021, "Action", "Zero stuff.", false);
        m2.setVideoFilePath("/movies/action_zero.mkv");
        m2.setTags(List.of("action", "blockbuster"));

        Movie m3 = new Movie("Comedy King", 2022, "Comedy", "Funny stuff.", true);
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
}
