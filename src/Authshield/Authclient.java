package Authshield;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Console client for AuthShield.
 *
 * Usage: java Authshield.Authclient [serverIP] [port]
 * (defaults: localhost 6363)
 */
public class Authclient {

    public static void main(String[] args) {

        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1
                ? Integer.parseInt(args[1]) : Protocol.DEFAULT_PORT;

        try (
            Socket socket = new Socket(host, port);

            BufferedReader keyboard = new BufferedReader(
                    new InputStreamReader(System.in));

            BufferedReader input = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream(), StandardCharsets.UTF_8));

            PrintWriter output = new PrintWriter(
                    new OutputStreamWriter(
                            socket.getOutputStream(), StandardCharsets.UTF_8),
                    true)
        ) {
            String greeting = input.readLine();
            if (greeting == null) {
                System.out.println("Server closed the connection.");
                return;
            }
            if (Protocol.command(greeting).equals(Protocol.BLOCKED)) {
                System.out.println("This IP is blocked. Try again in "
                        + Protocol.argument(greeting) + " seconds.");
                return;
            }

            System.out.println("Connected to AuthShield server at "
                    + host + ":" + port);

            while (true) {

                System.out.print("Enter username (or exit): ");
                String username = keyboard.readLine();

                if (username == null || username.equalsIgnoreCase("exit")) {
                    output.println(Protocol.QUIT);
                    break;
                }

                System.out.print("Enter password: ");
                String password = keyboard.readLine();
                if (password == null) {
                    break;
                }

                output.println(Protocol.loginRequest(username.trim(), password));

                String response = input.readLine();
                if (response == null) {
                    System.out.println("Server disconnected the client.");
                    break;
                }

                String command = Protocol.command(response);
                String arg = Protocol.argument(response);

                switch (command) {
                    case Protocol.SUCCESS:
                        System.out.println("Login successful! " + arg);
                        return;

                    case Protocol.BLOCKED:
                        System.out.println("Too many failed attempts. "
                                + "Blocked for " + arg + " seconds.");
                        return;

                    case Protocol.FAILED:
                        System.out.println("Login failed. Attempts left: " + arg);
                        break;

                    default:
                        System.out.println("Server: " + response);
                }
            }

        } catch (Exception e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }
}
