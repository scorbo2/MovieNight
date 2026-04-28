package ca.corbett.movienight.service;

import ca.corbett.movienight.model.Episode;
import ca.corbett.movienight.model.Movie;
import ca.corbett.movienight.model.MusicVideo;
import ca.corbett.movienight.repository.EpisodeRepository;
import ca.corbett.movienight.repository.MovieRepository;
import ca.corbett.movienight.repository.MusicVideoRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
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

    @Value("${movienight.prefix.movies:/}")
    private String movieDirectory;
    private File resolvedMovieDirectory;

    @Value("${movienight.prefix.episodes:/}")
    private String episodeDirectory;
    private File resolvedEpisodeDirectory;

    @Value("${movienight.prefix.music:/}")
    private String musicVideoDirectory;
    private File resolvedMusicVideoDirectory;

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
     * We'll check to ensure we got valid values for our media directories,
     * and resolve them to actual filesystem paths for use later.
     * This allows us to fail fast if our configuration is invalid,
     * and also allows us to avoid having to do this on every findById request.
     */
    @PostConstruct
    public void resolveMediaDirectories() {
        resolvedMovieDirectory = resolveMediaDirectory(movieDirectory, "movienight.prefix.movies");
        resolvedEpisodeDirectory = resolveMediaDirectory(episodeDirectory, "movienight.prefix.episodes");
        resolvedMusicVideoDirectory = resolveMediaDirectory(musicVideoDirectory, "movienight.prefix.music");
        logger.info("Resolved media directories - movies: {}, episodes: {}, music videos: {}",
                    resolvedMovieDirectory, resolvedEpisodeDirectory, resolvedMusicVideoDirectory);
    }

    /**
     * Given a media directory and a video file path, returns the resolved file path relative to the media directory.
     * Example: "/my/movies/Bladerunner.mp4" -> "Bladerunner.mp4" if mediaDirectory is "/my/movies/"
     */
    public static String resolveFilePath(String mediaDirectory, String videoFilePath) {
        if (videoFilePath != null && !videoFilePath.isBlank()) {
            if (mediaDirectory == null || mediaDirectory.isBlank()) {
                mediaDirectory = "/";
            }
            if (videoFilePath.startsWith(mediaDirectory)) {
                videoFilePath = videoFilePath.substring(mediaDirectory.length());
            }
        }
        return videoFilePath;
    }


    /**
     * Returns the resolved absolute video file path for an encoded media ID.
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
     * <p>
     * Model objects store media file paths relative to the configured media directory.
     * For example, a model object file path might be "bladerunner.mp4". We resolve this by
     * using the configured media directory for that type of media. For example, if our movie
     * prefix is "/mnt/storage/videos/movies", then the resolved path for "bladerunner.mp4" would be
     * "/mnt/storage/videos/movies/bladerunner.mp4".
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
            return new File(resolvedMovieDirectory, path).getAbsolutePath();
        } else if (type == 'E') {
            Episode episode = episodeService.requireEpisode(numericId);
            String path = episode.getVideoFilePath();
            episode.setLastWatchedDate(LocalDate.now());
            episode.setWatchedRecently(true);
            episodeRepository.save(episode);
            logger.debug("Resolved media id {} to episode file path: {}", encodedId, path);
            return new File(resolvedEpisodeDirectory, path).getAbsolutePath();
        } else if (type == 'V') {
            MusicVideo musicVideo = musicVideoService.requireMusicVideo(numericId);
            String path = musicVideo.getVideoFilePath();
            musicVideo.setLastWatchedDate(LocalDate.now());
            musicVideo.setWatchedRecently(true);
            musicVideoRepository.save(musicVideo);
            logger.debug("Resolved media id {} to music video file path: {}", encodedId, path);
            return new File(resolvedMusicVideoDirectory, path).getAbsolutePath();
        } else {
            logger.warn("Unknown media type prefix '{}' in id: {}", type, encodedId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown media type in id: " + encodedId);
        }
    }

    /**
     * Verifies that the named directory exists and is readable, and returns a File object
     * representing it if so. The propName parameter is used for logging purposes if something goes wrong.
     * <p>
     * Note: if the given directory is null or blank, we default to "/" as a fallback.
     * A warning will be logged in this case.
     * </p>
     */
    public static File resolveMediaDirectory(String directory, String propName) {
        if (directory == null || directory.isBlank()) {
            logger.warn("{} is not set or is blank (value: {}). Defaulting to root directory.", propName, directory);
            directory = "/";
        }
        File resolvedDirectory = new File(directory).getAbsoluteFile();
        if (!resolvedDirectory.exists() || !resolvedDirectory.isDirectory() || !resolvedDirectory.canRead()) {
            logger.error("Resolved {} directory does not exist or is not a readable directory: {}",
                         propName, resolvedDirectory);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                              "Can't read " + propName + " directory: " + resolvedDirectory);
        }
        return resolvedDirectory;
    }

    public void setMovieDirectory(String movieDirectory) {
        this.movieDirectory = movieDirectory;
        resolveMediaDirectories();
    }

    public void setEpisodeDirectory(String episodeDirectory) {
        this.episodeDirectory = episodeDirectory;
        resolveMediaDirectories();
    }

    public void setMusicVideoDirectory(String musicVideoDirectory) {
        this.musicVideoDirectory = musicVideoDirectory;
        resolveMediaDirectories();
    }
}
