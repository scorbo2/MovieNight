package ca.corbett.movienight;

import ca.corbett.movienight.model.Movie;
import ca.corbett.movienight.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
        movieRepository.save(movie);

        List<Movie> all = movieRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getTitle()).isEqualTo("Test Movie");
    }

    @Test
    void findByTitleContainingIgnoreCase() {
        movieRepository.save(new Movie("Inception", 2010, "Sci-Fi", "Dream heist.", false));
        movieRepository.save(new Movie("Interstellar", 2014, "Sci-Fi", "Space travel.", false));

        List<Movie> results = movieRepository.findByTitleContainingIgnoreCase("inter");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Interstellar");
    }

    @Test
    void findByWatched() {
        movieRepository.save(new Movie("Watched Movie", 2020, "Comedy", "Seen it.", true));
        movieRepository.save(new Movie("Unwatched Movie", 2021, "Thriller", "Not yet.", false));

        List<Movie> watched = movieRepository.findByWatched(true);
        assertThat(watched).hasSize(1);
        assertThat(watched.get(0).getTitle()).isEqualTo("Watched Movie");
    }

    @Test
    void saveAndFindMovieWithTags() {
        Movie movie = new Movie("Tagged Movie", 2023, "Action", "Has tags.", false);
        movie.setTags(List.of("Action", "Thriller", "Must-See"));
        movieRepository.save(movie);

        List<Movie> all = movieRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getTags()).containsExactlyInAnyOrder("action", "thriller", "must-see");
    }

    @Test
    void findByTagsContainingIgnoreCase() {
        Movie movie1 = new Movie("Movie One", 2021, "Drama", "First.", false);
        movie1.setTags(List.of("drama", "award-winner"));

        Movie movie2 = new Movie("Movie Two", 2022, "Comedy", "Second.", false);
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
        movie.setTags(List.of("Action", "ACTION", "action", "  Action  "));
        movieRepository.save(movie);

        List<Movie> all = movieRepository.findAll();
        assertThat(all.get(0).getTags()).containsExactly("action");
    }
}
