package ca.corbett.movienight.service;

import ca.corbett.movienight.model.Episode;
import ca.corbett.movienight.model.Movie;
import ca.corbett.movienight.model.MusicVideo;
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

    public MediaService(MovieService movieService, EpisodeService episodeService,
                        MusicVideoService musicVideoService) {
        this.movieService = movieService;
        this.episodeService = episodeService;
        this.musicVideoService = musicVideoService;
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
            movieService.saveMovie(movie);
            logger.debug("Resolved media id {} to movie file path: {}", encodedId, path);
            return path;
        } else if (type == 'E') {
            Episode episode = episodeService.requireEpisode(numericId);
            String path = episode.getVideoFilePath();
            episode.setLastWatchedDate(LocalDate.now());
            episodeService.saveEpisode(episode);
            logger.debug("Resolved media id {} to episode file path: {}", encodedId, path);
            return path;
        } else if (type == 'V') {
            MusicVideo musicVideo = musicVideoService.requireMusicVideo(numericId);
            String path = musicVideo.getVideoFilePath();
            musicVideo.setLastWatchedDate(LocalDate.now());
            musicVideoService.saveMusicVideo(musicVideo);
            logger.debug("Resolved media id {} to music video file path: {}", encodedId, path);
            return path;
        } else {
            logger.warn("Unknown media type prefix '{}' in id: {}", type, encodedId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown media type in id: " + encodedId);
        }
    }
}
