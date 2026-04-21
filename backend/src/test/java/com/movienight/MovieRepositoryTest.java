package com.movienight;

import com.movienight.model.Movie;
import com.movienight.repository.MovieRepository;
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
        Movie movie = new Movie("Test Movie", 2024, "Drama", "Test Director", 8.0, "A test movie.", false);
        movieRepository.save(movie);

        List<Movie> all = movieRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getTitle()).isEqualTo("Test Movie");
    }

    @Test
    void findByTitleContainingIgnoreCase() {
        movieRepository.save(new Movie("Inception", 2010, "Sci-Fi", "Christopher Nolan", 8.8, "Dream heist.", false));
        movieRepository.save(new Movie("Interstellar", 2014, "Sci-Fi", "Christopher Nolan", 8.7, "Space travel.", false));

        List<Movie> results = movieRepository.findByTitleContainingIgnoreCase("inter");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Interstellar");
    }

    @Test
    void findByWatched() {
        movieRepository.save(new Movie("Watched Movie", 2020, "Comedy", "Director A", 7.0, "Seen it.", true));
        movieRepository.save(new Movie("Unwatched Movie", 2021, "Thriller", "Director B", 6.5, "Not yet.", false));

        List<Movie> watched = movieRepository.findByWatched(true);
        assertThat(watched).hasSize(1);
        assertThat(watched.get(0).getTitle()).isEqualTo("Watched Movie");
    }
}
