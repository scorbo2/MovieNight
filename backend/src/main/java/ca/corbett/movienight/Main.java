package ca.corbett.movienight;

import ca.corbett.movienight.api.ApiServer;
import ca.corbett.movienight.config.AppConfig;
import ca.corbett.movienight.db.Database;

import java.io.File;
import java.util.logging.Logger;

/**
 * The entry point of the application.
 * <p>
 * Wires up configuration, database lifecycle, and the HTTP server.
 */
public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {
        // 1. Load configuration
        //    1a) if MOVIE_NIGHT_CONFIG env var is set, load from that file
        //    1b) otherwise, use defaults
        AppConfig config;
        String configPath = System.getenv("MOVIE_NIGHT_CONFIG");
        if (configPath != null) {
            File configFile = new File(configPath);
            if (!configFile.exists() || !configFile.isFile() || !configFile.canRead()) {
                System.err.println("Invalid config file specified in MOVIE_NIGHT_CONFIG: " + configPath);
                System.exit(1);
            }
            try {
                config = AppConfig.fromFile(configFile);
                System.out.println("Config loaded from " + configPath);
            }
            catch (Exception ex) {
                System.err.println("Failed to load config from " + configPath + ": " + ex.getMessage());
                System.exit(1);
                return; // unreachable but satisfies compiler
            }
        }
        else {
            config = AppConfig.defaults();
            System.out.println("No config file specified, using defaults.");
        }
        System.out.println("Starting application...");
        System.out.println("  Port: " + config.getPort());
        System.out.println("  Database: " + config.getDbFile());
        System.out.println("  API Base Path: " + config.getApiBasePath());

        // 2. Initialize and open the database
        Database database = new Database(config);
        database.open();
        System.out.println("Database initialized and connected.");

        // 3. Create and start the API server
        ApiServer apiServer = ApiServer.create(config, database);
        apiServer.start();

        // 4. Register shutdown hook for graceful teardown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            apiServer.stop();
            database.dispose();
            System.out.println("Shutdown complete.");
        }));

        System.out.println("Server is running. Press Ctrl+C to stop.");
    }
}
