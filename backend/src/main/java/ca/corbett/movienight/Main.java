package ca.corbett.movienight;

import ca.corbett.movienight.api.ApiServer;
import ca.corbett.movienight.config.AppConfig;
import ca.corbett.movienight.config.AppConfigInteractiveBuilder;
import ca.corbett.movienight.db.Database;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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

    private static FileHandler fileHandler = null;

    public static void main(String[] args) throws Exception {
        AppConfig config = loadAppConfig();
        configureLogging(config.getLogFile());
        Logger log = Logger.getLogger(Main.class.getName());
        log.info(config.toString());

        Database database = new Database(config);
        database.open();
        log.info("Database initialized and connected.");

        ApiServer apiServer = ApiServer.create(config, database);
        apiServer.start();
        log.info("MovieNight web UI: http://localhost:" + config.getPort());
        log.info("MovieNight API: http://localhost:" + config.getPort() + config.getApiBasePath());

        // 4. Register shutdown hook for graceful teardown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            apiServer.stop();
            database.dispose();
            if (fileHandler != null) {
                fileHandler.flush();
                fileHandler.close();
            }
            System.out.println("Shutdown complete.");
        }));

        // Don't pollute the log file with this. This is just for console users who run the app directly.
        System.out.println("Server is running. Press Ctrl+C to stop.");
    }

    /**
     * Load order for configuration:
     * <ol>
     *     <li>If MOVIENIGHT_CONFIG_FILE is set, read the config file from that location.</li>
     *     <li>If the env var is unspecified, check the current directory for "MovieNight.conf"</li>
     *     <li>If not found, check for a subdirectory called "config" and look for it there</li>
     *     <li>If still not found, we will enter interactive mode, prompting the user to enter
     *         our config values, and then offer to save that config to a file. We will end with
     *         instructions on how to set MOVIENIGHT_CONFIG_FILE to point to this file for next time.</li>
     * </ol>
     * <p>
     *     Note that even if a config file is found, it's still possible to override certain values
     *     with special environment variables. This is documented in the AppConfig class.
     * </p>
     *
     * @return A populated AppConfig instance.
     */
    public static AppConfig loadAppConfig() {
        File configFile;

        // Check out env var first:
        String configPath = System.getenv(AppConfig.ENV_VAR_CONFIG);
        if (configPath != null && !configPath.trim().isEmpty()) {
            configFile = new File(configPath);
        }

        // If the env var isn't set, check current directory and then config subdirectory:
        else {
            configFile = new File(AppConfig.DEFAULT_CONFIG_FILE_NAME);
            if (!configFile.exists()) {
                configFile = new File("config/" + AppConfig.DEFAULT_CONFIG_FILE_NAME);
                if (!configFile.exists()) {
                    configFile = null;
                }
            }
        }

        // If we got something above, let's check it:
        if (configFile != null) {
            if (!configFile.exists() || !configFile.isFile() || !configFile.canRead()) {
                System.out.println("Fatal: Invalid config file: " + configFile.getAbsolutePath());
                System.exit(1);
            }
            try {
                AppConfig config = AppConfig.fromFile(configFile);
                System.out.println("Config loaded from " + configFile.getAbsolutePath());
                return config;
            }
            catch (Exception ex) {
                System.out.println(
                        "Fatal: Failed to load config from " + configFile.getAbsolutePath() + ": " + ex.getMessage());
                System.exit(1);
            }
        }

        // If we get here, no config file was specified or located.
        // So, let's go into interactive mode to build one:
        while (true) {
            try {
                return AppConfigInteractiveBuilder.build("No configuration file found!");
            }
            catch (InterruptedException ie) {
                // The user most likely hit Ctrl+C. Just exit silently.
                System.exit(1);
            }
            catch (Exception e) {
                // Special case the "no console" error:
                if (AppConfigInteractiveBuilder.NO_CONSOLE_ERROR.equals(e.getMessage())) {
                    System.out.println(e.getMessage());
                    System.exit(1);
                }

                // Otherwise, give the user another shot at it, or let them Ctrl+C to kill us:
                System.out.println("Error during interactive config setup: " + e.getMessage());
                e.printStackTrace();
                System.out.println("Let's try again. (Or hit Ctrl+C to exit.)");
            }
        }
    }

    /**
     * If MOVIENIGHT_LOG_FILE is set to a valid file path, configures java.util.logging to write logs to that
     * file in addition to the console. Otherwise, logging is stdout only.
     */
    private static void configureLogging(Path logPath) {
        CustomLogFormatter customFormatter = new CustomLogFormatter();

        // Get the root logger (affects all loggers) and add our custom formatter:
        Logger rootLogger = Logger.getLogger("");
        for (Handler h : rootLogger.getHandlers()) {
            h.setFormatter(customFormatter);
        }

        // It's not a fatal error if we were not given a log file. We'll just output a hint and go with console-only.
        if (logPath == null) {
            System.out.println("Hint: You can set "
                                       + AppConfig.ENV_VAR_LOG_FILE
                                       + " environment variable to enable file logging.");
            return;
        }

        // If we were given a file, let's validate it and attempt to set up file logging.
        // (this is in addition to console logging, not instead of it).
        try {
            // Ensure the parent directory exists
            File logFile = logPath.toFile();
            File parentDir = logFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new IOException("Failed to create log directory: " + parentDir.getAbsolutePath());
                }
            }

            fileHandler = new FileHandler(logFile.getAbsolutePath(), true);
            fileHandler.setFormatter(customFormatter);
            rootLogger.addHandler(fileHandler);

            // Let console users know where they can find the log file:
            System.out.println("File logging enabled: " + logFile.getAbsolutePath());
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
    private static class CustomLogFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            String thrown = "";
            Throwable t = record.getThrown();
            if (t != null) {
                thrown = formatThrowable(t);
            }

            return String.format("%1$tF %1$tr [%2$s] %3$s%4$s%n",
                                 record.getMillis(),
                                 record.getLevel().getName(),
                                 formatMessage(record),
                                 thrown);
        }

        private String formatThrowable(Throwable t) {
            StringBuilder sb = new StringBuilder();
            Throwable current = t;
            while (current != null) {
                sb.append("\n").append(current.getClass().getName());
                String message = current.getMessage();
                if (message != null && !message.isEmpty()) {
                    sb.append(": ").append(message);
                }
                for (StackTraceElement element : current.getStackTrace()) {
                    sb.append("\n\tat ").append(element.toString());
                }
                current = current.getCause();
                if (current != null) {
                    sb.append("\nCaused by:");
                }
            }
            return sb.toString();
        }
    }
}
