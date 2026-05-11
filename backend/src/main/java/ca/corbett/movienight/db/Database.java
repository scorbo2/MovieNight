package ca.corbett.movienight.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A wrapper around the SQLite database connection and access logic.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since MovieNight 2.0
 */
public class Database {

    private static final Logger log = Logger.getLogger(Database.class.getName());
    private final File dbFile;
    private Connection connection;

    public Database(File dbFile) {
        if (dbFile == null) {
            throw new IllegalArgumentException("dbFile cannot be null");
        }
        if (dbFile.isDirectory() || !dbFile.canRead()) {
            throw new IllegalArgumentException("dbFile must be a readable file: " + dbFile.getAbsolutePath());
        }
        this.dbFile = dbFile;
        if (!dbFile.exists()) {
            initializeDatabase();
        }
        openDatabase();
    }

    /**
     * Releases our database connection.
     */
    public void dispose() {
        try {
            if (connection != null) {
                connection.close();
                connection = null;
            }
        }
        catch (SQLException sqe) {
            log.log(Level.SEVERE, "Problem closing database connection: " + sqe.getMessage(), sqe);
        }
    }

    /**
     * Creates a new, blank database with the appropriate schema.
     * Assumes that our dbFile does not exist - will overwrite if it does.
     */
    private void initializeDatabase() {
        try {
            if (dbFile.exists()) {
                log.warning("Database file already exists at " + dbFile.getAbsolutePath() + " - overwriting");
                dbFile.delete();
            }
            dbFile.createNewFile();
            openDatabase();
            DatabaseInitializer.initializeSchema(connection);
        }
        catch (Exception e) {
            log.log(Level.SEVERE, "Problem initializing database: " + e.getMessage(), e);
        }
    }

    /**
     * Opens a connection to the database.
     */
    private void openDatabase() throws SQLException {
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        connection = DriverManager.getConnection(url);
        log.info("Connected to database at " + dbFile.getAbsolutePath());
    }
}
