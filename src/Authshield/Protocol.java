package Authshield;

/**
 * AuthShield application-layer protocol (plain text over TCP, one
 * message per line, UTF-8).
 *
 * <pre>
 *  Server -> Client  WELCOME AuthShield          (sent right after connect)
 *  Client -> Server  LOGIN &lt;username&gt; &lt;password&gt;
 *  Server -> Client  SUCCESS &lt;message&gt;         (connection is then closed)
 *                    FAILED &lt;attemptsLeft&gt;     (client may try again)
 *                    BLOCKED &lt;secondsLeft&gt;     (connection is then closed)
 *                    ERROR &lt;reason&gt;            (malformed request)
 *  Client -> Server  QUIT
 *  Server -> Client  BYE
 * </pre>
 */
public final class Protocol {

    public static final int DEFAULT_PORT = 6363;

    public static final String WELCOME = "WELCOME";
    public static final String LOGIN = "LOGIN";
    public static final String QUIT = "QUIT";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String BLOCKED = "BLOCKED";
    public static final String ERROR = "ERROR";
    public static final String BYE = "BYE";

    private Protocol() {
    }

    /** Builds a LOGIN request line. */
    public static String loginRequest(String username, String password) {
        return LOGIN + " " + username + " " + password;
    }

    /** Returns the command word of a message (text before the first space). */
    public static String command(String line) {
        int space = line.indexOf(' ');
        return space < 0 ? line : line.substring(0, space);
    }

    /** Returns the argument part of a message (text after the first space). */
    public static String argument(String line) {
        int space = line.indexOf(' ');
        return space < 0 ? "" : line.substring(space + 1);
    }
}
