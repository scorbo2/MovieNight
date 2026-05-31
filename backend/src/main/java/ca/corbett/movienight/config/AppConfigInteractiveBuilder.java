package ca.corbett.movienight.config;

import java.io.File;
import java.util.Locale;

/**
 * This class uses stdin/stdout to interrogate the user for configuration values,
 * and then builds and returns an AppConfig instance representing those values.
 * An option will be presented to save the config to a file for future use,
 * and instructions are provided on how to set MOVIENIGHT_CONFIG_FILE to load it.
 * <p>
 * This is just a fallback in the case where we are launched with no configuration.
 * Rather than guessing defaults, we'll ask the user for it.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class AppConfigInteractiveBuilder {

    public static final String NO_CONSOLE_ERROR = "Interactive mode requires a terminal. Set "
            + AppConfig.ENV_VAR_CONFIG + " to use a config file instead.";

    private AppConfigInteractiveBuilder() {
    }

    /**
     * Starts the process of building out an AppConfig instance interactively.
     * You can specify an optional banner message to be displayed before we begin.
     *
     * @param bannerMsg Optional display message. If non-null and not blank, will be output before we begin.
     * @return A populated AppConfig instance.
     * @throws Exception on certain validation errors. We try to pre-validate before creating AppConfig.
     */
    public static AppConfig build(String bannerMsg) throws Exception {
        if (bannerMsg != null && !bannerMsg.trim().isEmpty()) {
            System.out.println(bannerMsg);
            System.out.println();
        }

        // Make sure we have access to a console. This is not guaranteed.
        // We may be running as a systemd service, or someone may have redirected stdin/stdout, etc.
        if (System.console() == null) {
            throw new Exception(NO_CONSOLE_ERROR);
        }

        // We'll ask for the important ones:
        int port = askInt("What port shall we listen on?", AppConfig.DEFAULT_PORT, 1, 65535);
        File mediaDir = askExistingDir("Where are our media files?",
                                       AppConfig.DEFAULT_MEDIA_DIR.toAbsolutePath().toString());
        File thumbDir = askExistingDir("Where are our thumbnails?",
                                       AppConfig.DEFAULT_THUMBNAIL_DIR.toAbsolutePath().toString());
        File dbFile = askWritableFile("Where is our database file?",
                                      AppConfig.DEFAULT_DB_FILE.toAbsolutePath().toString());
        if (!dbFile.exists()) {
            System.out.println("Database file does not exist. Will be created.");
        }

        // The less important ones, eh, we'll supply defaults.
        // This shouldn't feel like a drawn-out interrogation.
        int pageSize = AppConfig.DEFAULT_PAGE_SIZE;
        int rangeLimitMB = AppConfig.DEFAULT_RANGE_LIMIT_MB;
        String apiBasePath = AppConfig.DEFAULT_API_BASE_PATH;
        int recentlyWatchedDays = AppConfig.DEFAULT_RECENTLY_WATCHED_DAYS;
        int threadCount = AppConfig.DEFAULT_THREAD_COUNT;

        // We can optionally enable file logging:
        File logFile = null;
        if (askYesNo("Enable file-based logging?", false)) {
            File defaultLogFile = new File("MovieNight.log").getAbsoluteFile();
            logFile = askWritableFile("Where should we write logs?", defaultLogFile.getAbsolutePath());
        }

        AppConfig config = AppConfig.of(port,
                                        mediaDir.toPath(),
                                        thumbDir.toPath(),
                                        dbFile.toPath(),
                                        pageSize,
                                        apiBasePath,
                                        rangeLimitMB,
                                        logFile == null ? null : logFile.toPath(),
                                        recentlyWatchedDays,
                                        threadCount);

        if (askYesNo("Would you like to save this config?", true)) {
            File defaultConfigFile = new File(AppConfig.DEFAULT_CONFIG_FILE_NAME).getAbsoluteFile();
            File configFile = askWritableFile("Where should we save the config?", defaultConfigFile.getAbsolutePath());
            config.writeToFile(configFile);
            System.out.println("Config saved to " + configFile.getAbsolutePath());
            System.out.println("Next time, you can set the environment variable "
                                       + AppConfig.ENV_VAR_CONFIG + " to point to this file.");
        }
        else {
            System.out.println("Config not saved. You will need to re-enter these values next time.");
        }

        return config;
    }


    private static boolean askYesNo(String question, boolean defaultAnswer) {
        String defaultStr = defaultAnswer ? "Y/n" : "y/N";
        while (true) {
            System.out.print(question + " [" + defaultStr + "]: ");
            String input = System.console().readLine().trim().toLowerCase(Locale.ROOT);
            if (input.isEmpty()) {
                return defaultAnswer;
            }
            if (input.equals("y") || input.equals("yes") || input.equals("true") || input.equals("on")) {
                return true;
            }
            if (input.equals("n") || input.equals("no") || input.equals("false") || input.equals("off")) {
                return false;
            }
            System.out.println("Please enter 'y' or 'n'.");
        }
    }

    private static File askExistingDir(String question, String defaultPath) {
        while (true) {
            System.out.print(question + " [" + defaultPath + "]: ");
            String input = System.console().readLine().trim();
            if (input.isEmpty()) {
                input = defaultPath;
            }
            File dir = new File(input);
            if (!dir.exists() || !dir.isDirectory() || !dir.canRead()) {
                System.out.println("Please enter a valid directory path.");
                continue;
            }
            return dir;
        }
    }

    /**
     * Ask the user for the location of a file that we can write to.
     * It's perfectly fine if the file doesn't exist yet - we'll create it,
     * but the parent directory must exist and be writable, and if the file does exist, it must be writable as well.
     */
    private static File askWritableFile(String question, String defaultPath) {
        while (true) {
            System.out.print(question + " [" + defaultPath + "]: ");
            String input = System.console().readLine().trim();
            if (input.isEmpty()) {
                input = defaultPath;
            }
            File file = new File(input);
            if (file.exists() && (!file.isFile() || !file.canWrite())) {
                System.out.println("That file exists, but is not writable. Please enter a different file path.");
                continue;
            }
            if (!file.exists()) {
                File parentDir = file.getAbsoluteFile().getParentFile();
                if (parentDir == null || !parentDir.exists() || !parentDir.isDirectory() || !parentDir.canWrite()) {
                    System.out.println(
                            "That file is not in a valid writable directory. Please enter a different file path.");
                    continue;
                }
            }
            return file;
        }
    }

    private static int askInt(String question, int defaultAnswer, int min, int max) {
        while (true) {
            System.out.print(question + " [" + defaultAnswer + "]: ");
            String input = System.console().readLine().trim();
            if (input.isEmpty()) {
                return defaultAnswer;
            }
            try {
                int answer = Integer.parseInt(input);
                if (answer < min || answer > max) {
                    System.out.println("Please enter a number between " + min + " and " + max + ".");
                    continue;
                }
                return answer;
            }
            catch (NumberFormatException ex) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }
}
