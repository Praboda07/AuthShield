package Authshield;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Persists security events in a local SQLite database. The server
 * writes events and the dashboard (a separate process) reads them.
 *
 * The database file defaults to ./authshield.db and can be changed with
 * -Dauthshield.db=path/to/file.db
 */
public class EventStore {

    private static final String DB_URL = "jdbc:sqlite:"
            + System.getProperty("authshield.db", "authshield.db");

    /** Text written in the details of the event that starts a block. */
    public static final String BLOCK_START_MARKER = "IP blocked";

    private static Connection connect() throws SQLException {
        Properties props = new Properties();
        // Wait instead of failing when the other process holds the lock.
        props.setProperty("busy_timeout", "5000");
        return DriverManager.getConnection(DB_URL, props);
    }

    public static synchronized void initialize() {

        String sql = "CREATE TABLE IF NOT EXISTS security_events ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "event_time TEXT NOT NULL, "
                + "ip_address TEXT NOT NULL, "
                + "username TEXT NOT NULL, "
                + "event_type TEXT NOT NULL, "
                + "details TEXT)";

        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {

            statement.execute(sql);
            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            System.err.println("Database initialization failed: "
                    + e.getMessage());
        }
    }

    public static synchronized void logEvent(
            String time, String ip, String username,
            String type, String details) {

        String sql = "INSERT INTO security_events "
                + "(event_time, ip_address, username, event_type, details) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, time);
            statement.setString(2, ip);
            statement.setString(3, username == null ? "" : username);
            statement.setString(4, type);
            statement.setString(5, details);

            statement.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Could not save event: " + e.getMessage());
        }
    }

    /**
     * Most recent events, newest first.
     *
     * @param type event type to filter by, or null for all events
     */
    public static synchronized List<String[]> recentEvents(int limit, String type) {

        List<String[]> events = new ArrayList<>();

        String sql = "SELECT id, event_time, ip_address, username, "
                + "event_type, details FROM security_events "
                + (type == null ? "" : "WHERE event_type = ? ")
                + "ORDER BY id DESC LIMIT ?";

        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            int index = 1;
            if (type != null) {
                statement.setString(index++, type);
            }
            statement.setInt(index, Math.max(1, limit));

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    events.add(new String[] {
                        String.valueOf(result.getInt("id")),
                        result.getString("event_time"),
                        result.getString("ip_address"),
                        result.getString("username"),
                        result.getString("event_type"),
                        result.getString("details")
                    });
                }
            }

        } catch (SQLException e) {
            System.err.println("Could not read events: " + e.getMessage());
        }

        return events;
    }

    public static List<String[]> recentEvents(int limit) {
        return recentEvents(limit, null);
    }

    public static synchronized int countByType(String type) {
        return queryInt("SELECT COUNT(*) FROM security_events "
                + "WHERE event_type = ?", type);
    }

    public static synchronized int countDistinctIPs() {
        return queryInt("SELECT COUNT(DISTINCT ip_address) "
                + "FROM security_events", null);
    }

    /**
     * IP addresses whose block is still active, mapped to the number of
     * seconds left. Worked out from the latest block-start event per IP.
     */
    public static synchronized Map<String, Long> activeBlocks(long blockMillis) {

        Map<String, Long> blocks = new LinkedHashMap<>();

        String sql = "SELECT ip_address, MAX(event_time) AS started "
                + "FROM security_events WHERE event_type = 'BLOCKED' "
                + "AND details LIKE ? GROUP BY ip_address "
                + "ORDER BY started DESC";

        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, BLOCK_START_MARKER + "%");

            long now = System.currentTimeMillis();

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    try {
                        long started = Instant.parse(
                                result.getString("started")).toEpochMilli();
                        long left = started + blockMillis - now;
                        if (left > 0) {
                            blocks.put(result.getString("ip_address"),
                                    (left + 999) / 1000);
                        }
                    } catch (Exception ignored) {
                        // Skip rows with an unreadable timestamp.
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Could not read blocks: " + e.getMessage());
        }

        return blocks;
    }

    /** Deletes every stored event. Used by the dashboard's Clear button. */
    public static synchronized void clearEvents() {
        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM security_events");
        } catch (SQLException e) {
            System.err.println("Could not clear events: " + e.getMessage());
        }
    }

    private static int queryInt(String sql, String param) {
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (param != null) {
                statement.setString(1, param);
            }
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.err.println("Could not run query: " + e.getMessage());
        }
        return 0;
    }
}
