package Authshield;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * Handles one connected client on a worker thread from the server's
 * thread pool. Reads protocol messages line by line and replies.
 */
public class ClientHandler implements Runnable {

    /** Idle clients are disconnected after this many milliseconds. */
    private static final int READ_TIMEOUT_MS = 120_000;

    private final Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {

        String connectionId = clientSocket.getRemoteSocketAddress()
                + "-" + System.identityHashCode(clientSocket);

        String clientIP = clientSocket.getInetAddress().getHostAddress();

        AuthServer.clientConnected();

        try (
            Socket socket = clientSocket;

            BufferedReader input = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream(), StandardCharsets.UTF_8));

            PrintWriter output = new PrintWriter(
                    new java.io.OutputStreamWriter(
                            socket.getOutputStream(), StandardCharsets.UTF_8),
                    true)
        ) {
            socket.setSoTimeout(READ_TIMEOUT_MS);

            // Reject an IP that is still blocked straight away.
            long blockedFor = AuthService.remainingBlockSeconds(clientIP);
            if (blockedFor > 0) {
                EventStore.logEvent(java.time.Instant.now().toString(),
                        clientIP, "-", "BLOCKED",
                        "Connection rejected while blocked ("
                                + blockedFor + "s left)");
                System.out.println("[BLOCKED] Rejected connection from "
                        + clientIP + " (" + blockedFor + "s left)");
                output.println(Protocol.BLOCKED + " " + blockedFor);
                return;
            }

            output.println(Protocol.WELCOME + " AuthShield");

            String line;
            while ((line = input.readLine()) != null) {

                line = line.trim();
                String command = Protocol.command(line).toUpperCase();

                if (command.equals(Protocol.QUIT)
                        || command.equalsIgnoreCase("exit")) {
                    output.println(Protocol.BYE);
                    break;
                }

                if (!command.equals(Protocol.LOGIN)) {
                    output.println(Protocol.ERROR + " Unknown command");
                    continue;
                }

                String[] parts = Protocol.argument(line).split(" ", 2);
                if (parts.length < 2 || parts[0].isEmpty()) {
                    output.println(Protocol.ERROR
                            + " Usage: LOGIN <username> <password>");
                    continue;
                }

                AuthService.Result result = AuthService.authenticate(
                        parts[0], parts[1], clientIP, connectionId);

                switch (result.status) {
                    case SUCCESS:
                        output.println(Protocol.SUCCESS
                                + " Welcome, " + parts[0]);
                        return;

                    case BLOCKED:
                        output.println(Protocol.BLOCKED + " " + result.value);
                        return;

                    default:
                        output.println(Protocol.FAILED + " " + result.value);
                }
            }

        } catch (SocketTimeoutException e) {
            System.out.println("Client " + clientIP + " timed out (idle).");

        } catch (IOException e) {
            System.err.println("Client connection error for "
                    + clientIP + ": " + e.getMessage());

        } finally {
            AuthServer.clientDisconnected();
            System.out.println("Client disconnected: " + clientIP);
        }
    }
}
