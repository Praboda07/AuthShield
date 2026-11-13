
package Authshield;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class EventStore {

    private static final String DB_URL =
            "jdbc:sqlite:authshield.db";

    public static synchronized void initialize() {

        String sql = "CREATE TABLE IF NOT EXISTS security_events ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "event_time TEXT NOT NULL, "
                + "ip_address TEXT NOT NULL, "
                + "username TEXT NOT NULL, "
                + "event_type TEXT NOT NULL, "
                + "details TEXT)";

        try (Connection connection =
                     DriverManager.getConnection(DB_URL);
             Statement statement = connection.createStatement()) {

            statement.execute(sql);
            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            System.err.println(
                    "Database initialization failed: " + e.getMessage());
        }
    }

    public static synchronized void logEvent(
            String time, String ip, String username,
            String type, String details) {

        String sql = "INSERT INTO security_events "
                + "(event_time, ip_address, username, event_type, details) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection connection =
                     DriverManager.getConnection(DB_URL);
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, time);
            statement.setString(2, ip);
            statement.setString(3, username);
            statement.setString(4, type);
            statement.setString(5, details);

            statement.executeUpdate();

        } catch (SQLException e) {
            System.err.println(
                    "Could not save event: " + e.getMessage());
        }
    }

    public static synchronized List<String[]> recentEvents(int limit) {

        List<String[]> events = new ArrayList<>();

        String sql = "SELECT id, event_time, ip_address, username, "
                + "event_type, details FROM security_events "
                + "ORDER BY id DESC LIMIT ?";

        try (Connection connection =
                     DriverManager.getConnection(DB_URL);
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, Math.max(1, limit));

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
            System.err.println(
                    "Could not read events: " + e.getMessage());
        }

        return events;
    }

    public static synchronized int countByType(String type) {

        String sql = "SELECT COUNT(*) FROM security_events "
                + "WHERE event_type = ?";

        try (Connection connection =
                     DriverManager.getConnection(DB_URL);
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, type);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.err.println(
                    "Could not count events: " + e.getMessage());
        }

        return 0;
    }
}
