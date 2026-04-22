package ca.corbett.movienight.config;

import ca.corbett.movienight.model.Genre;
import ca.corbett.movienight.model.Movie;
import ca.corbett.movienight.repository.GenreRepository;
import ca.corbett.movienight.repository.MovieRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner loadSampleData(MovieRepository movieRepository, GenreRepository genreRepository) {
        return args -> {
            if (movieRepository.count() == 0) {
                Genre drama = new Genre("Drama", "");
                Genre crime = new Genre("Crime", "");
                Genre action = new Genre("Action", "");
                Genre sciFi = new Genre("Sci-Fi", "");
                genreRepository.save(drama);
                genreRepository.save(crime);
                genreRepository.save(action);
                genreRepository.save(sciFi);

                Movie m1 = new Movie("The Shawshank Redemption", 1994, drama,
                        "Two imprisoned men bond over a number of years.", true);
                m1.setVideoFilePath("/movies/shawshank_redemption.mkv");
                movieRepository.save(m1);

                Movie m2 = new Movie("The Godfather", 1972, crime,
                        "The aging patriarch of an organized crime dynasty.", true);
                m2.setVideoFilePath("/movies/the_godfather.mkv");
                movieRepository.save(m2);

                Movie m3 = new Movie("The Dark Knight", 2008, action,
                        "When the Joker wreaks havoc on Gotham City.", false);
                m3.setVideoFilePath("/movies/the_dark_knight.mkv");
                movieRepository.save(m3);

                Movie m4 = new Movie("Pulp Fiction", 1994, crime,
                        "The lives of two mob hitmen intertwine.", false);
                m4.setVideoFilePath("/movies/pulp_fiction.mkv");
                movieRepository.save(m4);

                Movie m5 = new Movie("Interstellar", 2014, sciFi,
                        "A team of explorers travels through a wormhole.", false);
                m5.setVideoFilePath("/movies/interstellar.mkv");
                movieRepository.save(m5);
            }
        };
    }
}
