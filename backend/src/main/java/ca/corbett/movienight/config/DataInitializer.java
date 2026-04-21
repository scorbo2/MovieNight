package ca.corbett.movienight.config;

import ca.corbett.movienight.model.Movie;
import ca.corbett.movienight.repository.MovieRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner loadSampleData(MovieRepository movieRepository) {
        return args -> {
            if (movieRepository.count() == 0) {
                movieRepository.save(new Movie("The Shawshank Redemption", 1994, "Drama",
                        "Two imprisoned men bond over a number of years.", true));
                movieRepository.save(new Movie("The Godfather", 1972, "Crime",
                        "The aging patriarch of an organized crime dynasty.", true));
                movieRepository.save(new Movie("The Dark Knight", 2008, "Action",
                        "When the Joker wreaks havoc on Gotham City.", false));
                movieRepository.save(new Movie("Pulp Fiction", 1994, "Crime",
                        "The lives of two mob hitmen intertwine.", false));
                movieRepository.save(new Movie("Interstellar", 2014, "Sci-Fi",
                        "A team of explorers travels through a wormhole.", false));
            }
        };
    }
}
