package ca.corbett.movienight.service;

import ca.corbett.movienight.model.Episode;
import ca.corbett.movienight.model.Movie;
import ca.corbett.movienight.model.MusicVideo;
import ca.corbett.movienight.repository.EpisodeRepository;
import ca.corbett.movienight.repository.MovieRepository;
import ca.corbett.movienight.repository.MusicVideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
public class MediaService {

    private static final Logger logger = LoggerFactory.getLogger(MediaService.class);

    private final MovieService movieService;
    private final EpisodeService episodeService;
    private final MusicVideoService musicVideoService;
    private final MovieRepository movieRepository;
    private final EpisodeRepository episodeRepository;
    private final MusicVideoRepository musicVideoRepository;

    public MediaService(MovieService movieService,
                        EpisodeService episodeService,
                        MusicVideoService musicVideoService,
                        MovieRepository movieRepository,
                        EpisodeRepository episodeRepository,
                        MusicVideoRepository musicVideoRepository) {
        this.movieService = movieService;
        this.episodeService = episodeService;
        this.musicVideoService = musicVideoService;
        this.movieRepository = movieRepository;
        this.episodeRepository = episodeRepository;
        this.musicVideoRepository = musicVideoRepository;
    }

    /**
     * Returns the absolute video file path for an encoded media ID.
     * The id must be "M" followed by a numeric movie ID, "E" followed by a numeric episode ID,
     * or "V" followed by a numeric music video ID.
     * <p>
     * Note that invoking this method for any existing model object will
     * update that model object's lastWatchedDate to the current date and save it back to the database.
     * This seems like a nice centralized place to do this, but it does mean
     * that this method has a side effect of updating the database, which is a bit unexpected for a "find" method.
     * </p>
     * <p>
     * Dev note: the above-mentioned saves are done via the repositories instead of via the service
     * layer, to avoid the filesystem overhead that the services will incur when they go to
     * update the thumbnail on save. Since we know the thumbnail won't change when just updating
     * the last watched date, we can skip that overhead.
     * </p>
     *
     * @param encodedId encoded media ID, e.g. "M31", "E77", or "V12"
     * @return absolute path to the video file
     * @throws ResponseStatusException 400 if the ID format is invalid, 404 if the entity is not found
     */
    public String findById(String encodedId) {
        if (encodedId == null || encodedId.length() < 2) {
            logger.warn("Invalid media id received: {}", encodedId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid media id: " + encodedId);
        }

        char type = encodedId.charAt(0);
        String numericPart = encodedId.substring(1);
        Long numericId;
        try {
            numericId = Long.parseLong(numericPart);
        } catch (NumberFormatException e) {
            logger.warn("Invalid media id received: {}", encodedId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid media id: " + encodedId);
        }

        if (type == 'M') {
            Movie movie = movieService.requireMovie(numericId);
            String path = movie.getVideoFilePath();
            movie.setLastWatchedDate(LocalDate.now());
            movie.setWatchedRecently(true); // Set manually, since we know it's "true" even without date math
            movieRepository.save(movie); // Save via repository to avoid unnecessary filesystem overhead in the service
            logger.debug("Resolved media id {} to movie file path: {}", encodedId, path);
            return path;
        } else if (type == 'E') {
            Episode episode = episodeService.requireEpisode(numericId);
            String path = episode.getVideoFilePath();
            episode.setLastWatchedDate(LocalDate.now());
            episode.setWatchedRecently(true);
            episodeRepository.save(episode);
            logger.debug("Resolved media id {} to episode file path: {}", encodedId, path);
            return path;
        } else if (type == 'V') {
            MusicVideo musicVideo = musicVideoService.requireMusicVideo(numericId);
            String path = musicVideo.getVideoFilePath();
            musicVideo.setLastWatchedDate(LocalDate.now());
            musicVideo.setWatchedRecently(true);
            musicVideoRepository.save(musicVideo);
            logger.debug("Resolved media id {} to music video file path: {}", encodedId, path);
            return path;
        } else {
            logger.warn("Unknown media type prefix '{}' in id: {}", type, encodedId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown media type in id: " + encodedId);
        }
    }
}
