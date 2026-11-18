package Authshield;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentication and brute-force detection logic.
 *
 * Failed attempts are tracked per client IP address (not per TCP
 * connection), so a client cannot bypass the block simply by
 * disconnecting and reconnecting. After MAX_ATTEMPTS consecutive
 * failures the IP is blocked for BLOCK_TIME milliseconds.
 */
public class AuthService {

    private static final String USERNAME = "Admin";
    private static final String PASSWORD = "1234";

    public static final int MAX_ATTEMPTS = 3;
    public static final long BLOCK_TIME = 60_000;

    /** Result of one authentication attempt. */
    public static final class Result {
        public enum Status { SUCCESS, FAILED, BLOCKED }

        public final Status status;
        /** Remaining attempts (FAILED) or remaining block seconds (BLOCKED). */
        public final long value;

        Result(Status status, long value) {
            this.status = status;
            this.value = value;
        }
    }

    // Shared by all client threads, keyed by client IP address.
    private static final Map<String, AttemptState> attempts =
            new ConcurrentHashMap<>();

    private static class AttemptState {
        int failedAttempts = 0;
        long blockedUntil = 0;
    }

    /** Seconds left in the block for this IP, or 0 when not blocked. */
    public static synchronized long remainingBlockSeconds(String clientIP) {
        AttemptState state = attempts.get(clientIP);
        if (state == null) {
            return 0;
        }
        long left = state.blockedUntil - System.currentTimeMillis();
        return left > 0 ? (left + 999) / 1000 : 0;
    }

    public static synchronized Result authenticate(
            String username,
            String password,
            String clientIP,
            String connectionId) {

        long currentTime = System.currentTimeMillis();

        AttemptState state = attempts.computeIfAbsent(
                clientIP, k -> new AttemptState());

        // 1. Is this IP currently blocked?
        if (currentTime < state.blockedUntil) {

            long secondsLeft = (state.blockedUntil - currentTime + 999) / 1000;

            EventStore.logEvent(
                    Instant.now().toString(),
                    clientIP,
                    username,
                    "BLOCKED",
                    "Login attempt rejected while blocked ("
                            + secondsLeft + "s left); connection "
                            + connectionId);

            log("BLOCKED", clientIP + " tried to log in while blocked ("
                    + secondsLeft + "s left)");

            return new Result(Result.Status.BLOCKED, secondsLeft);
        }

        // 2. Block expired: reset the counter.
        if (state.blockedUntil != 0) {
            state.failedAttempts = 0;
            state.blockedUntil = 0;
            log("UNBLOCKED", clientIP);
        }

        // 3. Check the credentials.
        if (USERNAME.equals(username) && PASSWORD.equals(password)) {

            state.failedAttempts = 0;

            EventStore.logEvent(
                    Instant.now().toString(),
                    clientIP,
                    username,
                    "SUCCESS",
                    "Login successful; connection " + connectionId);

            log("SUCCESS", username + " from " + clientIP);

            return new Result(Result.Status.SUCCESS, 0);
        }

        // 4. Wrong credentials: count the failure for this IP.
        state.failedAttempts++;

        EventStore.logEvent(
                Instant.now().toString(),
                clientIP,
                username,
                "FAILED",
                "Failed attempt " + state.failedAttempts + " of "
                        + MAX_ATTEMPTS + "; connection " + connectionId);

        log("FAILED", clientIP + " attempt " + state.failedAttempts
                + "/" + MAX_ATTEMPTS + " (user '" + username + "')");

        // 5. Too many failures: block the IP.
        if (state.failedAttempts >= MAX_ATTEMPTS) {

            state.blockedUntil = currentTime + BLOCK_TIME;

            EventStore.logEvent(
                    Instant.now().toString(),
                    clientIP,
                    username,
                    "BLOCKED",
                    EventStore.BLOCK_START_MARKER + " for "
                            + (BLOCK_TIME / 1000) + " seconds");

            log("BLOCKED", clientIP + " blocked for "
                    + (BLOCK_TIME / 1000) + " seconds");

            return new Result(Result.Status.BLOCKED, BLOCK_TIME / 1000);
        }

        return new Result(Result.Status.FAILED,
                MAX_ATTEMPTS - state.failedAttempts);
    }

    private static void log(String tag, String message) {
        System.out.println("[" + tag + "] " + message);
    }
}
