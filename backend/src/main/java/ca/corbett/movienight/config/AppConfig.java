package ca.corbett.movienight.config;

import java.io.File;
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
 * database file path, and pagination defaults.
 * </p>
 * <p>
 * The easiest way to get started is the defaults() factory method, which supplies reasonable defaults.
 * The data directory defaults to the current working directory.
 * </p>
 * <p>
 *     Alternatively you can specify the path to the configuration file using the fromFile() factory method,
 *     which will read settings from a Java properties file. The expected properties are:
 * </p>
 * <ul>
 *     <li><b>port</b>: the port number the server should listen on</li>
 *     <li><b>dataDir</b>: the directory where the database file and thumbnails will be stored</li>
 *     <li><b>pageSize</b>: the number of items to return in paginated API responses
 *         (e.g. for GET /api/media/groups)</li>
 *     <li><b>apiBasePath</b>: the base path for all API endpoints (default: "/api/")</li>
 *     <li><b>rangeLimitMB</b>: the maximum length of an HTTP Range request in megabytes (default: 32)</li>
 * </ul>
 * <p>
 *     <b>IMPORTANT ENVIRONMENT VARIABLES:</b>
 * </p>
 * <ul>
 *     <li><b>MOVIENIGHT_CONFIG_FILE</b>: points to a valid config file. Fatal if the file can't be read.</li>
 *     <li><b>MOVIENIGHT_LOG_FILE</b>: optional path to a log file. If not set, logs will only go to the console.</li>
 * </ul>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class AppConfig {

    public static final String ENV_VAR_CONFIG = "MOVIENIGHT_CONFIG_FILE";
    public static final String ENV_VAR_LOG_FILE = "MOVIENIGHT_LOG_FILE";

    private static final Logger log = Logger.getLogger(AppConfig.class.getName());

    private static final int DEFAULT_PORT = 8080;
    private static final Path DEFAULT_DATA_DIR = Path.of("."); // horrible default - up to the user to set this
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final String API_BASE_PATH = "/api/";
    private static final int DEFAULT_RANGE_LIMIT_MB = 32;

    private final int port;
    private final Path dataDir;
    private final Path thumbnailDir;
    private final Path dbFile;
    private final int defaultPageSize;
    private final String apiBasePath;
    private final int rangeLimitMB;

    private AppConfig(int port, Path dataDir, int defaultPageSize, String apiBasePath, int rangeLimitMB) {
        this.port = port;
        this.dataDir = dataDir;
        this.dbFile = dataDir.resolve("MovieNight.db"); // okay if this doesn't exist here - will be created.
        this.thumbnailDir = dataDir.resolve("thumbnails");
        this.thumbnailDir.toFile().mkdirs(); // it's okay if this doesn't exist - let's just create it.
        this.defaultPageSize = defaultPageSize;
        this.apiBasePath = apiBasePath;
        this.rangeLimitMB = rangeLimitMB;
    }

    /**
     * Creates an AppConfig by reading settings from the given file.
     * Any setting that is not specified in the file will fall back to its default value.
     * Unrecognized settings are simply ignored.
     * <p>
     * If a data directory is specified in the file, it must exist and be readable!
     * This method will throw IOException if an invalid directory is found.
     * </p>
     */
    public static AppConfig fromFile(File inFile) throws IOException {
        int port = DEFAULT_PORT;
        Path dataDir = DEFAULT_DATA_DIR;
        int pageSize = DEFAULT_PAGE_SIZE;
        String apiBasePath = API_BASE_PATH;
        int rangeLimitMB = DEFAULT_RANGE_LIMIT_MB;
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
        if (props.containsKey("dataDir")) {
            dataDir = requireValidDirectory(Path.of(props.getProperty("dataDir")));
        }
        if (props.containsKey("pageSize")) {
            try {
                pageSize = requireValidPageSize(Integer.parseInt(props.getProperty("pageSize")));
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

        return new AppConfig(port, dataDir, pageSize, apiBasePath, rangeLimitMB);
    }

    /**
     * Creates an {@link AppConfig} using all defaults.
     */
    public static AppConfig defaults() {
        return new AppConfig(DEFAULT_PORT, DEFAULT_DATA_DIR, DEFAULT_PAGE_SIZE, API_BASE_PATH, DEFAULT_RANGE_LIMIT_MB);
    }

    /**
     * Factory method for creating a fully customized AppConfig instance.
     */
    public static AppConfig of(int port, Path dataDir, int pageSize, String apiBasePath, int rangeLimitMB)
            throws IOException {
        return new AppConfig(
                requireValidPort(port),
                requireValidDirectory(dataDir),
                requireValidPageSize(pageSize),
                apiBasePath,
                requireValidMBValue(rangeLimitMB)
        );
    }

    /**
     * Creates an {@link AppConfig} with a custom port. If the port number is invalid
     * (i.e. not a positive integer or greater than 65535), it is ignored and the default is used.
     */
    public static AppConfig withPort(int port) {
        int validPort;
        try {
            validPort = requireValidPort(port);
        }
        catch (NumberFormatException ignored) {
            log.warning("Invalid port number, using default: " + DEFAULT_PORT);
            validPort = DEFAULT_PORT;
        }
        return new AppConfig(validPort, DEFAULT_DATA_DIR, DEFAULT_PAGE_SIZE, API_BASE_PATH, DEFAULT_RANGE_LIMIT_MB);
    }

    /**
     * Creates an {@link AppConfig} with a custom data directory. If the given directory
     * does not exist, is not a directory, or is not readable, it is ignored and the
     * default directory (current working directory) is used.
     */
    public static AppConfig withDataDir(Path dataDir) {
        Path validPath;
        try {
            validPath = requireValidDirectory(dataDir);
        }
        catch (IOException ignored) {
            log.warning("Invalid data directory, using default: " + DEFAULT_DATA_DIR.toAbsolutePath());
            validPath = DEFAULT_DATA_DIR;
        }
        return new AppConfig(DEFAULT_PORT, validPath, DEFAULT_PAGE_SIZE, API_BASE_PATH, DEFAULT_RANGE_LIMIT_MB);
    }

    public int getPort() {
        return port;
    }

    public Path getDataDir() {
        return dataDir;
    }

    public Path getDbFile() {
        return dbFile;
    }

    public Path getThumbnailDir() {
        return thumbnailDir;
    }

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public String getApiBasePath() {
        return apiBasePath;
    }

    public int getRangeLimitMB() {
        return rangeLimitMB;
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
            throw new IOException("Data directory does not exist or is not readable: " + pathStr);
        }
        return dir;
    }

    private static int requireValidPageSize(int pageSize) throws NumberFormatException {
        if (pageSize <= 0) {
            throw new NumberFormatException("Page size must be a positive integer");
        }
        return pageSize;
    }

    private static int requireValidMBValue(int mbValue) throws NumberFormatException {
        if (mbValue <= 0) {
            throw new NumberFormatException("MB value must be a positive integer");
        }
        return mbValue;
    }

    @Override
    public String toString() {
        return "AppConfig{\n" +
                "  port=" + port +
                ",\n  dataDir=" + dataDir.toAbsolutePath() +
                ",\n  dbFile=" + dbFile.toAbsolutePath() +
                ",\n  thumbnailDir=" + thumbnailDir.toAbsolutePath() +
                ",\n  defaultPageSize=" + defaultPageSize +
                ",\n  apiBasePath='" + apiBasePath + '\'' +
                ",\n  rangeLimitMB=" + rangeLimitMB +
                '}';
    }
}
