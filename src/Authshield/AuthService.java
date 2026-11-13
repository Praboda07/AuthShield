
package Authshield;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {

    private static final String USERNAME = "Admin";
    private static final String PASSWORD = "1234";

    private static final int MAX_ATTEMPTS = 3;
    private static final long BLOCK_TIME = 60_000;

    // Each TCP connection has its own attempt counter.
    private static final Map<String, AttemptState> attempts =
            new ConcurrentHashMap<>();

    private static class AttemptState {
        int failedAttempts = 0;
        long blockedUntil = 0;
    }

    public static synchronized String authenticate(
            String username,
            String password,
            String clientIP,
            String connectionId) {

        String key = connectionId;
        long currentTime = System.currentTimeMillis();

        AttemptState state = attempts.computeIfAbsent(
                key, k -> new AttemptState());

        // Check whether this connection is blocked.
        if (currentTime < state.blockedUntil) {

            EventStore.logEvent(
                    Instant.now().toString(),
                    clientIP,
                    username,
                    "BLOCKED",
                    "Connection " + connectionId
                            + " attempted login while blocked");

            System.out.println(
                    "[BLOCKED] Connection " + connectionId
                            + " from " + clientIP);

            return "TEMPORARILY_BLOCKED";
        }

        // Reset the counter after the block expires.
        if (state.blockedUntil != 0
                && currentTime >= state.blockedUntil) {

            state.failedAttempts = 0;
            state.blockedUntil = 0;

            System.out.println(
                    "[UNBLOCKED] Connection " + connectionId);
        }

        // Check username and password.
        if (USERNAME.equals(username)
                && PASSWORD.equals(password)) {

            state.failedAttempts = 0;
            state.blockedUntil = 0;

            EventStore.logEvent(
                    Instant.now().toString(),
                    clientIP,
                    username,
                    "SUCCESS",
                    "Login successful; connection " + connectionId);

            System.out.println(
                    "[SUCCESS] Login successful for "
                            + username + " on " + connectionId);

            return "LOGIN_SUCCESS";
        }

        // Record a failed attempt for this connection.
        state.failedAttempts++;

        EventStore.logEvent(
                Instant.now().toString(),
                clientIP,
                username,
                "FAILED",
                "Connection " + connectionId
                        + "; failed attempt: "
                        + state.failedAttempts);

        System.out.println(
                "[FAILED] Connection " + connectionId
                        + ", attempt " + state.failedAttempts
                        + ", IP " + clientIP);

        // Block this connection after three failures.
        if (state.failedAttempts >= MAX_ATTEMPTS) {

            state.blockedUntil = currentTime + BLOCK_TIME;

            EventStore.logEvent(
                    Instant.now().toString(),
                    clientIP,
                    username,
                    "BLOCKED",
                    "Connection " + connectionId
                            + " blocked for 60 seconds");

            System.out.println(
                    "[BLOCKED] Connection " + connectionId
                            + " blocked for 60 seconds");

            return "TEMPORARILY_BLOCKED";
        }

        return "LOGIN_FAILED";
    }
}
