package ca.corbett.movienight.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Central application configuration.
 * <p>
 * Holds the settings the server needs to bootstrap itself: listening port,
 * database file path, pagination defaults, and so on.
 * </p>
 * <p>
 * The easiest way to get started is the defaults() factory method, which supplies reasonable defaults.
 * The data directory defaults to the current working directory.
 * </p>
 * <p>
 * Alternatively, you can specify the path to the configuration file using the fromFile() factory method,
 * which will read settings from a Java properties file. The expected properties are:
 * </p>
 * <ul>
 *     <li><b>port</b>: the port number the server should listen on</li>
 *     <li><b>dbFile</b>: the path the our SQLite db file (defaults to "MovieNight.db" in current dir)</li>
 *     <li><b>mediaDir</b>: the directory where the media files are stored</li>
 *     <li><b>thumbnailDir</b>: the directory where media group thumbnail images will be stored</li>
 *     <li><b>pageSize</b>: the number of items to return in paginated API responses
 *         (e.g. for GET /api/media/groups)</li>
 *     <li><b>apiBasePath</b>: the base path for all API endpoints (default: "/api/")</li>
 *     <li><b>rangeLimitMB</b>: the maximum length of an HTTP Range request in megabytes (default: 32)</li>
 *     <li><b>logFile</b>: an optional file for file-based logging. Can be null.</li>
 *     <li><b>threadCount</b>: how many threads to allocate for handling HTTP requests (default: 5)</li>
 * </ul>
 * <p>
 *     <b>IMPORTANT ENVIRONMENT VARIABLES:</b>
 * </p>
 * <ul>
 *     <li><b>MOVIENIGHT_CONFIG_FILE</b>: points to a valid config file. Fatal if the file can't be read.</li>
 *     <li><b>MOVIENIGHT_LOG_FILE</b>: optional path to a log file. If not set, logs will only go to the console.</li>
 *     <li><b>MOVIENIGHT_DB_FILE</b>: if set, overrides the value from the config file.</li>
 *     <li><b>MOVIENIGHT_MEDIA_DIR</b>: if set, overrides the value from the config file.</li>
 *     <li><b>MOVIENIGHT_THUMBNAIL_DIR</b>: if set, overrides the value from the config file.</li>
 * </ul>
 * <p>
 *     Note that your mediaDir and your thumbnailDir can be the same directory, or you can keep them separate.
 *     But be careful - all media file paths in our database are relative to the given mediaDir, but all
 *     thumbnail images are stored as direct children of the given thumbnailDir. This might lead to an awkward
 *     structure if you set them to the same value. Recommended structure is vaguely like this:
 * </p>
 * <pre>
 * MyMediaDir/
 *   Movies/
 *     Bladerunner.mkv
 *     StarTrek2.mkv
 *   TVShows/
 *      TheOffice/
 *        Season1/
 *          etc...
 * Thumbnails/
 *   (flat list of all thumbs here)
 * </pre>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class AppConfig {

    public static final String ENV_VAR_CONFIG = "MOVIENIGHT_CONFIG_FILE";
    public static final String ENV_VAR_LOG_FILE = "MOVIENIGHT_LOG_FILE";
    public static final String ENV_VAR_DB_FILE = "MOVIENIGHT_DB_FILE";
    public static final String ENV_VAR_MEDIA_DIR = "MOVIENIGHT_MEDIA_DIR";
    public static final String ENV_VAR_THUMBNAIL_DIR = "MOVIENIGHT_THUMBNAIL_DIR";

    private static final Logger log = Logger.getLogger(AppConfig.class.getName());

    public static final String DEFAULT_CONFIG_FILE_NAME = "MovieNight.conf";
    public static final int DEFAULT_PORT = 8080;
    public static final Path DEFAULT_DB_FILE = Path.of("MovieNight.db");
    public static final Path DEFAULT_MEDIA_DIR = Path.of(".");
    public static final Path DEFAULT_THUMBNAIL_DIR = Path.of("thumbnails");
    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final String DEFAULT_API_BASE_PATH = "/api/";
    public static final int DEFAULT_RANGE_LIMIT_MB = 32;
    public static final int DEFAULT_RECENTLY_WATCHED_DAYS = 3;
    public static final int DEFAULT_THREAD_COUNT = 5;

    private Path logFile;
    private final int port;
    private Path mediaDir;
    private Path thumbnailDir;
    private Path dbFile;
    private final int pageSize;
    private final String apiBasePath;
    private final int rangeLimitMB;
    private final int recentlyWatchedDays;
    private final int threadCount;

    private AppConfig(int port, Path mediaDir, Path thumbnailDir, Path dbFile,
                      int pageSize, String apiBasePath, int rangeLimitMB, Path logFile,
                      int recentlyWatchedDays, int threadCount) {
        this.port = port;
        this.mediaDir = mediaDir;
        this.dbFile = dbFile;
        this.thumbnailDir = thumbnailDir;
        this.pageSize = pageSize;
        this.apiBasePath = apiBasePath;
        this.rangeLimitMB = rangeLimitMB;
        this.logFile = logFile;
        this.recentlyWatchedDays = recentlyWatchedDays;
        this.threadCount = threadCount;
    }

    /**
     * Creates an AppConfig by reading settings from the given file.
     * Any setting that is not specified in the file will fall back to its default value.
     * Unrecognized settings are simply ignored.
     * <p>
     * Note that the three path properties (mediaDir, thumbnailDir, and dbFile) can all be overridden
     * by environment variables (MOVIENIGHT_MEDIA_DIR, MOVIENIGHT_THUMBNAIL_DIR, and MOVIENIGHT_DB_FILE, respectively).
     * So, even if the given file contains valid values for these properties, those values might get ignored
     * if the environment variables are set.
     * </p>
     * <p>
     * The mediaDir and thumbnailDir must exist and be readable!
     * This method will throw IOException if either path does not exist or can't be read.
     * </p>
     * <p>
     * It is not an error to specify a dbFile that does not exist. It will be created,
     * as long as the parent directory exists and is writable.
     * </p>
     */
    public static AppConfig fromFile(File inFile) throws IOException {
        int port = DEFAULT_PORT;
        Path mediaDir = DEFAULT_MEDIA_DIR;
        Path thumbnailDir = DEFAULT_THUMBNAIL_DIR;
        Path dbFile = DEFAULT_DB_FILE;
        int pageSize = DEFAULT_PAGE_SIZE;
        String apiBasePath = DEFAULT_API_BASE_PATH;
        int rangeLimitMB = DEFAULT_RANGE_LIMIT_MB;
        int recentlyWatchedDays = DEFAULT_RECENTLY_WATCHED_DAYS;
        int threadCount = DEFAULT_THREAD_COUNT;
        Path logFile = null;
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(inFile.toPath())) { props.load(in); }
        if (props.containsKey("port")) {
            try {
                port = requireValidPort(Integer.parseInt(props.getProperty("port")));
            }
            catch (NumberFormatException ignored) {
                log.warning("Invalid port number in config file, using default: " + DEFAULT_PORT);
            }
        }
        if (props.containsKey("mediaDir")) {
            mediaDir = requireValidDirectory(Path.of(props.getProperty("mediaDir")));
        }
        if (props.containsKey("thumbnailDir")) {
            thumbnailDir = requireValidDirectory(Path.of(props.getProperty("thumbnailDir")));
        }
        if (props.containsKey("dbFile")) {
            dbFile = requireValidWritableFile(Path.of(props.getProperty("dbFile")));
        }
        if (props.containsKey("pageSize")) {
            try {
                pageSize = requireInteger(Integer.parseInt(props.getProperty("pageSize")));
            }
            catch (NumberFormatException ignored) {
                log.warning("Invalid pageSize in config file, using default: " + DEFAULT_PAGE_SIZE);
            }
        }
        if (props.containsKey("apiBasePath")) {
            // apiBasePath can be whatever arbitrary path you want, but it must begin
            // and end with a slash, like "/api/" or "/v1/" or "/hello/my/lovely/".
            // Note that "/" by itself is also a valid value if you just want to stick
            // everything at the root level. If leading or trailing slashes are missing,
            // we add them here and log a warning.
            apiBasePath = props.getProperty("apiBasePath");
            if (!apiBasePath.startsWith("/")) {
                log.warning("apiBasePath should start with '/', adding it automatically");
                apiBasePath = "/" + apiBasePath;
            }
            if (!apiBasePath.endsWith("/")) {
                log.warning("apiBasePath should end with '/', adding it automatically");
                apiBasePath = apiBasePath + "/";
            }
        }
        if (props.containsKey("rangeLimitMB")) {
            try {
                rangeLimitMB = requireValidMBValue(Integer.parseInt(props.getProperty("rangeLimitMB")));
            }
            catch (NumberFormatException ignored) {
                log.warning("Invalid rangeLimitMB in config file, using default: " + rangeLimitMB);
            }
        }
        if (props.containsKey("logFile")) {
            logFile = requireValidWritableFile(Path.of(props.getProperty("logFile")));
        }
        if (props.containsKey("recentlyWatchedDays")) {
            try {
                recentlyWatchedDays = requireInteger(Integer.parseInt(props.getProperty("recentlyWatchedDays")), true);
            }
            catch (NumberFormatException ignored) {
                log.warning("Invalid recentlyWatchedDays in config file, using default: "
                                    + DEFAULT_RECENTLY_WATCHED_DAYS);
            }
        }
        if (props.containsKey("threadCount")) {
            try {
                threadCount = requireInteger(Integer.parseInt(props.getProperty("threadCount")));
                if (threadCount <= 2) {
                    log.warning("threadCount in config file is very low (" + threadCount + ")! " +
                                        "You may experience performance issues. Consider increasing it.");
                }
                int procCount = Runtime.getRuntime().availableProcessors();
                if (threadCount > procCount * 2) {
                    log.warning("threadCount in config file is very high (" + threadCount + ")! " +
                                        "(You only have " + procCount + " CPU cores.) " +
                                        "You may experience performance issues. Consider decreasing it.");
                }
            }
            catch (NumberFormatException ignored) {
                log.warning("Invalid threadCount in config file, using default: " + DEFAULT_THREAD_COUNT);
            }
        }

        AppConfig newConfig = new AppConfig(port, mediaDir, thumbnailDir, dbFile, pageSize, apiBasePath, rangeLimitMB,
                                            logFile, recentlyWatchedDays, threadCount);
        newConfig.applyEnvVarOverrides(); // Allow environment vars to override what we just built above
        return newConfig;
    }

    /**
     * Creates an {@link AppConfig} using all defaults.
     * If our environment variables for mediaDir, thumbnailDir, or dbFile are set, those will override the defaults..
     */
    public static AppConfig create() {
        AppConfig newConfig = new AppConfig(DEFAULT_PORT, DEFAULT_MEDIA_DIR, DEFAULT_THUMBNAIL_DIR, DEFAULT_DB_FILE,
                                            DEFAULT_PAGE_SIZE, DEFAULT_API_BASE_PATH, DEFAULT_RANGE_LIMIT_MB,
                                            null, DEFAULT_RECENTLY_WATCHED_DAYS, DEFAULT_THREAD_COUNT);
        newConfig.applyEnvVarOverrides(); // Allow environment vars to override defaults
        return newConfig;
    }

    /**
     * Factory method for creating a fully customized AppConfig instance.
     * Mainly here for testing. Usually better to use fromFile().
     * <p>
     * Note: this method ignores environment variable overrides in favor of the supplied values.
     * </p>
     */
    public static AppConfig of(int port, Path mediaDir, Path thumbnailDir, Path dbFile,
                               int pageSize, String apiBasePath, int rangeLimitMB, Path logFile,
                               int recentlyWatchedDays, int threadCount)
            throws IOException {
        return new AppConfig(
                requireValidPort(port),
                requireValidDirectory(mediaDir),
                requireValidDirectory(thumbnailDir),
                requireValidWritableFile(dbFile),
                requireInteger(pageSize),
                apiBasePath,
                requireValidMBValue(rangeLimitMB),
                logFile,
                requireInteger(recentlyWatchedDays, true),
                threadCount
        );
    }

    /**
     * Creates an {@link AppConfig} with custom media and thumbnail directories and a custom dbFile.
     * All other values are set to defaults.
     * <p>
     * Note: this method ignores environment variable overrides in favor of the supplied values.
     * </p>
     *
     * @throws IOException if any of the given paths are invalid (e.g. mediaDir doesn't exist, or dbFile is not writable)
     */
    public static AppConfig withCustomPaths(Path mediaDir, Path thumbnailDir, Path dbFile) throws IOException {
        Path validMediaDir = requireValidDirectory(mediaDir);
        Path validThumbnailDir = requireValidDirectory(thumbnailDir);
        Path validDbFile = requireValidWritableFile(dbFile);
        return new AppConfig(DEFAULT_PORT, validMediaDir, validThumbnailDir, validDbFile,
                             DEFAULT_PAGE_SIZE, DEFAULT_API_BASE_PATH, DEFAULT_RANGE_LIMIT_MB, null,
                             DEFAULT_RECENTLY_WATCHED_DAYS, DEFAULT_THREAD_COUNT);
    }

    /**
     * Returns the port number the server should listen on.
     * This applies both to the web UI and to the API endpoints.
     * Note: a value of "0" has a special meaning of "bind to any available port",
     * which is useful for tests. To find out which port was actually assigned in that case,
     * you can call apiServer.getPort() after starting the server.
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the effective mediaDir, which is the directory where media files are stored.
     * Note that the returned value may not match what you passed in, depending on how this
     * instance was created. If the environment variable MOVIENIGHT_MEDIA_DIR is set, then
     * that value might override whatever value was in the config file.
     */
    public Path getMediaDir() {
        return mediaDir;
    }

    /**
     * Returns the effective dbFile path, which is the path to our SQLite database file.
     * Note that the returned value may not match what you passed in, depending on how this
     * instance was created. If the environment variable MOVIENIGHT_DB_FILE is set, then that
     * value might override whatever value was in the config file.
     */
    public Path getDbFile() {
        return dbFile;
    }

    /**
     * Returns the effective thumbnailDir, which is the directory where media group thumbnail images are stored.
     * Note that the returned value may not match what you passed in, depending on how this
     * instance was created. If the environment variable MOVIENIGHT_THUMBNAIL_DIR is set, then that
     * value might override whatever value was in the config file.
     * <p>
     * Note that media item thumbnails are not stored in this directory!
     * Media item thumbnails are stored as sidecar files alongside the media file itself.
     * See ThumbnailUtil for more details.
     * </p>
     */
    public Path getThumbnailDir() {
        return thumbnailDir;
    }

    /**
     * Returns the effective logFile path, which is the path to our log file, or null if no log file is configured.
     */
    public Path getLogFile() {
        return logFile;
    }

    /**
     * Returns the default page size for paginated API responses, such as GET /api/media-groups.
     * This is the number of items that will be returned in each page of results when the client
     * does not specify a page size.
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Returns the base path for all API endpoints, which is prepended to all API paths.
     * The web UI is always served on "/", so it's a good idea to keep this as something like
     * "/api/" or "/MovieNight/" to avoid conflicts with the UI paths.
     */
    public String getApiBasePath() {
        return apiBasePath;
    }

    /**
     * Returns the maximum length of an HTTP Range request in megabytes.
     * If your local network is very fast, and you regularly stream very large media files,
     * you can increase this value.
     */
    public int getRangeLimitMB() {
        return rangeLimitMB;
    }

    /**
     * Returns the threshold, in days, for considering a media item as "recently watched".
     * Media that has been streamed within this threshold will be marked in the UI as "recently watched".
     * Set to 0 to disable this feature (no media item will be so marked in the UI at all).
     */
    public int getRecentlyWatchedDays() {
        return recentlyWatchedDays;
    }

    /**
     * Shorthand for checking if recentlyWatchedDays is greater than zero.
     * 0 means "turns this feature off entirely".
     */
    public boolean isRecentlyWatchedFeatureEnabled() {
        return recentlyWatchedDays > 0;
    }

    /**
     * Returns the configured number of threads to allocate for handling HTTP requests.
     */
    public int getThreadCount() {
        return threadCount;
    }

    /**
     * Invoked internally to check for our environment variable overrides,
     * and will update any property that has been overridden.
     */
    private void applyEnvVarOverrides() {
        try {
            String envDbFile = System.getenv(ENV_VAR_DB_FILE);
            if (envDbFile != null && !envDbFile.trim().isEmpty()) {
                log.info("Overriding dbFile from environment variable " + ENV_VAR_DB_FILE);
                dbFile = requireValidWritableFile(Path.of(envDbFile));
            }
            String mediaDirEnv = System.getenv(ENV_VAR_MEDIA_DIR);
            if (mediaDirEnv != null && !mediaDirEnv.trim().isEmpty()) {
                log.info("Overriding mediaDir from environment variable " + ENV_VAR_MEDIA_DIR);
                mediaDir = requireValidDirectory(Path.of(mediaDirEnv));
            }
            String thumbnailDirEnv = System.getenv(ENV_VAR_THUMBNAIL_DIR);
            if (thumbnailDirEnv != null && !thumbnailDirEnv.trim().isEmpty()) {
                log.info("Overriding thumbnailDir from environment variable " + ENV_VAR_THUMBNAIL_DIR);
                thumbnailDir = requireValidDirectory(Path.of(thumbnailDirEnv));
            }
            String logFileEnv = System.getenv(ENV_VAR_LOG_FILE);
            if (logFileEnv != null && !logFileEnv.trim().isEmpty()) {
                log.info("Overriding logFile from environment variable " + ENV_VAR_LOG_FILE);
                logFile = requireValidWritableFile(Path.of(logFileEnv));
            }
        }
        catch (IOException ioe) {
            log.severe("Failed to apply environment variable overrides: " + ioe.getMessage());
        }
    }

    private static int requireValidPort(int port) throws NumberFormatException {
        // "0" has a special meaning of "bind to any available port", so we allow it (useful for tests),
        // but otherwise the port must be a positive integer less than or equal to 65535:
        if (port < 0 || port > 65535) {
            throw new NumberFormatException("Port number out of valid range");
        }
        return port;
    }

    private static Path requireValidDirectory(Path dir) throws IOException {
        if (dir == null || !dir.toFile().exists() || !dir.toFile().isDirectory() || !dir.toFile().canRead()) {
            String pathStr = (dir == null) ? "null" : dir.toAbsolutePath().toString();
            throw new IOException("Directory does not exist or is not readable: " + pathStr);
        }
        return dir;
    }

    private static Path requireValidWritableFile(Path dbFile) throws IOException {
        if (dbFile == null) {
            throw new IOException("dbFile path cannot be null");
        }

        // If the file exists, then it must be readable and writable.
        if (dbFile.toFile().exists() && (!dbFile.toFile().canRead() || !dbFile.toFile().canWrite())) {
            throw new IOException("dbFile exists but is not read/write: " + dbFile.toAbsolutePath());
        }

        // If the file doesn't exist, that's fine, but its parent dir must exist and be writable.
        Path parent = dbFile.getParent();
        if (!dbFile.toFile().exists() &&
                (parent == null || !parent.toFile().exists() ||
                        !parent.toFile().isDirectory() || !parent.toFile().canWrite())) {
            String pathStr = (parent == null) ? "null" : parent.toAbsolutePath().toString();
            throw new IOException("Parent directory of dbFile does not exist or is not writable: " + pathStr);
        }

        return dbFile;
    }

    private static int requireInteger(int value) throws NumberFormatException {
        return requireInteger(value, false);
    }

    private static int requireInteger(int value, boolean allowZero) throws NumberFormatException {
        if (allowZero && value == 0) {
            return value;
        }
        if (value <= 0) {
            throw new NumberFormatException("Value must be a positive integer");
        }
        return value;
    }

    private static int requireValidMBValue(int mbValue) throws NumberFormatException {
        if (mbValue <= 0) {
            throw new NumberFormatException("MB value must be a positive integer");
        }
        return mbValue;
    }

    @Override
    public String toString() {
        return "AppConfig {\n" +
                "  port=" + port +
                ",\n  mediaDir=" + mediaDir.toAbsolutePath() +
                ",\n  dbFile=" + dbFile.toAbsolutePath() +
                ",\n  thumbnailDir=" + thumbnailDir.toAbsolutePath() +
                ",\n  defaultPageSize=" + pageSize +
                ",\n  apiBasePath='" + apiBasePath + '\'' +
                ",\n  rangeLimitMB=" + rangeLimitMB +
                ",\n  logFile=" + (logFile != null ? logFile.toAbsolutePath() : "null") +
                ",\n  recentlyWatchedDays=" + recentlyWatchedDays +
                ",\n  threadCount=" + threadCount +
                "\n}";
    }

    /**
     * Writes all configuration properties to the given file as a Java properties file
     * (simple name=value format). Overwrites the file if it already exists.
     * <p>
     * Properties written: port, mediaDir, dbFile, thumbnailDir, pageSize, apiBasePath,
     * rangeLimitMB, logFile (if non-null), and recentlyWatchedDays.
     * </p>
     *
     * @param destination the file to write to; created or overwritten
     * @throws IOException if the file cannot be written
     */
    public void writeToFile(File destination) throws IOException {
        Properties props = new Properties();
        props.setProperty("port", String.valueOf(port));
        props.setProperty("mediaDir", mediaDir.toString());
        props.setProperty("dbFile", dbFile.toString());
        props.setProperty("thumbnailDir", thumbnailDir.toString());
        props.setProperty("pageSize", String.valueOf(pageSize));
        props.setProperty("apiBasePath", apiBasePath);
        props.setProperty("rangeLimitMB", String.valueOf(rangeLimitMB));
        if (logFile != null) {
            props.setProperty("logFile", logFile.toString());
        }
        props.setProperty("recentlyWatchedDays", String.valueOf(recentlyWatchedDays));
        props.setProperty("threadCount", String.valueOf(threadCount));
        try (FileOutputStream out = new FileOutputStream(destination)) {
            props.store(out, "MovieNight configuration");
        }
    }
}
