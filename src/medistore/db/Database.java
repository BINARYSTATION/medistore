package medistore.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Single point of contact with the SQLite database file.
 *
 * The database lives next to the application in data/medistore.db so the whole
 * project stays portable - copying the folder copies the data with it.
 */
public final class Database {

    /** Overridable with -Dmedistore.db=... so tools can work on a throwaway file. */
    private static final String DB_FILE =
            System.getProperty("medistore.db", "data/medistore.db");

    private static Connection connection;

    private Database() {
    }

    /** Returns the shared connection, opening and preparing it on first use. */
    public static synchronized Connection get() {
        if (connection == null) {
            open();
        }
        return connection;
    }

    private static void open() {
        try {
            File file = new File(DB_FILE);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("Cannot create data folder: " + parent);
            }
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getPath());
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
            createTables();
        } catch (ClassNotFoundException | SQLException e) {
            throw new IllegalStateException("Unable to open the database: " + e.getMessage(), e);
        }
    }

    /** Creates every table the first time the application runs. */
    private static void createTables() throws SQLException {
        try (Statement st = connection.createStatement()) {
            for (String ddl : Schema.STATEMENTS) {
                st.execute(ddl);
            }
        }
    }

    /** Deletes the database file and rebuilds it empty. Used by the sample-data tool. */
    public static synchronized void reset() {
        close();
        File file = new File(DB_FILE);
        if (file.exists() && !file.delete()) {
            throw new IllegalStateException("Cannot delete " + file);
        }
        open();
    }

    public static synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // Nothing useful can be done while shutting down.
            }
            connection = null;
        }
    }
}
