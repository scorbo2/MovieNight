package ca.corbett.movienight.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MediaService {

    private static final Logger logger = LoggerFactory.getLogger(MediaService.class);

    private final MovieService movieService;
    private final EpisodeService episodeService;

    public MediaService(MovieService movieService, EpisodeService episodeService) {
        this.movieService = movieService;
        this.episodeService = episodeService;
    }

    /**
     * Returns the absolute video file path for an encoded media ID.
     * The id must be "M" followed by a numeric movie ID, or "E" followed by a numeric episode ID.
     *
     * @param encodedId encoded media ID, e.g. "M31" or "E77"
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
            String path = movieService.requireMovie(numericId).getVideoFilePath();
            logger.debug("Resolved media id {} to movie file path: {}", encodedId, path);
            return path;
        } else if (type == 'E') {
            String path = episodeService.requireEpisode(numericId).getVideoFilePath();
            logger.debug("Resolved media id {} to episode file path: {}", encodedId, path);
            return path;
        } else {
            logger.warn("Unknown media type prefix '{}' in id: {}", type, encodedId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown media type in id: " + encodedId);
        }
    }
}
