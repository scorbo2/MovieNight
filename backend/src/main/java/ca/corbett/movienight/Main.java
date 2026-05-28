package ca.corbett.movienight;

import ca.corbett.movienight.api.ApiServer;
import ca.corbett.movienight.config.AppConfig;
import ca.corbett.movienight.db.Database;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * The entry point of the application.
 * <p>
 * Wires up configuration, database lifecycle, and the HTTP server.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        configureLogging();
        Logger log = Logger.getLogger(Main.class.getName());

        AppConfig config = loadAppConfig(log);
        log.info(config.toString());

        Database database = new Database(config);
        database.open();
        log.info("Database initialized and connected.");

        ApiServer apiServer = ApiServer.create(config, database);
        apiServer.start();
        log.info("MovieNight started on port " + config.getPort());

        // 4. Register shutdown hook for graceful teardown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            apiServer.stop();
            database.dispose();
            log.info("Shutdown complete.");
        }));

        // Don't pollute the log file with this. This is just for console users who run the app directly.
        System.out.println("Server is running. Press Ctrl+C to stop.");
    }

    /**
     * Checks for MOVIENIGHT_CONFIG_FILE and attempts to load application configuration from
     * the named file. If the environment variable is not set, or if loading fails, defaults will be used.
     *
     * @return A populated AppConfig instance, either loaded from file or with defaults.
     */
    public static AppConfig loadAppConfig(Logger log) {
        AppConfig config = AppConfig.defaults();
        String configPath = System.getenv(AppConfig.ENV_VAR_CONFIG);
        if (configPath != null) {
            File configFile = new File(configPath);
            if (!configFile.exists() || !configFile.isFile() || !configFile.canRead()) {
                log.severe("Fatal: Invalid config file specified in " + AppConfig.ENV_VAR_CONFIG + ": " + configPath);
                System.exit(1);
            }
            try {
                config = AppConfig.fromFile(configFile);
                log.info("Config loaded from " + configPath);
            }
            catch (Exception ex) {
                log.severe("Fatal: Failed to load config from " + configPath + ": " + ex.getMessage());
                System.exit(1);
            }
        }
        else {
            log.info("Hint: set " + AppConfig.ENV_VAR_CONFIG + " to set custom configuration. Using defaults.");
        }

        return config;
    }

    /**
     * If MOVIENIGHT_LOG_FILE is set to a valid file path, configures java.util.logging to write logs to that
     * file in addition to the console. Otherwise, logging is stdout only.
     */
    public static void configureLogging() {
        CustomLogFormatter customFormatter = new CustomLogFormatter();

        // Get the root logger (affects all loggers) and add our custom formatter:
        Logger rootLogger = Logger.getLogger("");
        for (Handler h : rootLogger.getHandlers()) {
            h.setFormatter(customFormatter);
        }

        // It's not a fatal error if we were not given a log file. We'll just output a hint and go with console-only.
        String logFile = System.getenv(AppConfig.ENV_VAR_LOG_FILE);
        if (logFile == null || logFile.trim().isEmpty()) {
            System.out.println("Hint: You can set "
                                       + AppConfig.ENV_VAR_LOG_FILE
                                       + " environment variable to enable file logging.");
            return;
        }

        // If we were given a file, let's validate it and attempt to set up file logging.
        // (this is in addition to console logging, not instead of it).
        try {
            // Ensure the parent directory exists
            File logPath = new File(logFile);
            File parentDir = logPath.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new IOException("Failed to create log directory: " + parentDir.getAbsolutePath());
                }
            }

            FileHandler fileHandler = new FileHandler(logFile, true);
            fileHandler.setFormatter(customFormatter);
            rootLogger.addHandler(fileHandler);

            // Let console users know where they can find the log file:
            System.out.println("File logging enabled: " + logFile);
        }
        catch (IOException | SecurityException e) {
            // Again, not fatal if we couldn't get it set up. Just log a warning and move on.
            System.err.println("Failed to initialize file logging: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * A custom log formatter that produces more human-friendly log messages.
     */
    public static class CustomLogFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            String thrown = "";
            if (record.getThrown() != null) {
                thrown = "\n" + record.getThrown();
            }

            return String.format("%1$tF %1$tr [%2$s] %3$s%4$s%n",
                                 record.getMillis(),
                                 record.getLevel().getName(),
                                 formatMessage(record),
                                 thrown);
        }
    }
}
