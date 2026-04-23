package ca.corbett.movienight.service;

import ca.corbett.movienight.model.Genre;
import ca.corbett.movienight.model.Movie;
import ca.corbett.movienight.repository.GenreRepository;
import ca.corbett.movienight.repository.MovieRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class GenreService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final ThumbnailService thumbnailService;

    public GenreService(GenreRepository genreRepository,
                        MovieRepository movieRepository,
                        ThumbnailService thumbnailService) {
        this.thumbnailService = thumbnailService;
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
    }

    public List<Genre> getAllGenres() {
        return populateHasThumbnail(genreRepository.findAll());
    }

    public Optional<Genre> getGenreById(Long id) {
        return genreRepository.findById(id).map(this::populateHasThumbnail);
    }

    public Genre requireGenre(Long id) {
        return getGenreById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                               "Genre not found with id: " + id));
    }

    public Genre saveGenre(Genre genre) {
        if (genreRepository.existsByNameIgnoreCase(genre.getName())) {
            // Throw a 409 Conflict if a genre with the same name already exists
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                                              "Genre with name '" + genre.getName() + "' already exists.");
        }
        return populateHasThumbnail(genreRepository.save(genre));
    }

    public void deleteGenre(Long id) {
        // Throw a 404 if the Genre doesn't exist:
        if (!genreRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Genre not found with id: " + id);
        }

        // Throw a 409 if any Movie currently references this Genre:
        if (movieRepository.countByGenre(requireGenre(id)) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                                              "Cannot delete genre with id: " + id
                                                      + " because it is referenced by existing movies.");
        }

        thumbnailService.deleteThumbnail("genres", id);
        genreRepository.deleteById(id);
    }

    public Genre updateGenre(Long id, Genre updatedGenre) {
        Genre existingName = genreRepository.findByNameIgnoreCase(updatedGenre.getName()).orElse(null);
        if (existingName != null && !existingName.getId().equals(id)) {
            // Throw a 409 Conflict if another genre with the same name already exists
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                                              "Genre with name '" + updatedGenre.getName() + "' already exists.");
        }
        Genre existingGenre = requireGenre(id);
        existingGenre.setName(updatedGenre.getName());
        existingGenre.setDescription(updatedGenre.getDescription());
        return populateHasThumbnail(genreRepository.save(existingGenre));
    }

    private Genre populateHasThumbnail(Genre genre) {
        genre.setHasThumbnail(thumbnailService.hasThumbnail("genres", genre.getId()));
        return genre;
    }

    private List<Genre> populateHasThumbnail(List<Genre> genres) {
        genres.forEach(this::populateHasThumbnail);
        return genres;
    }
}
