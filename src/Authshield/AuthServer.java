
package Authshield;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuthServer {

    private static final int PORT = 6363;
    private static final int MAX_CLIENT_THREADS = 10;

    public static void main(String[] args) {

        EventStore.initialize();

        ExecutorService threadPool =
                Executors.newFixedThreadPool(MAX_CLIENT_THREADS);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            System.out.println("Welcome to AuthShield.");
            System.out.println("AuthShield server started.");
            System.out.println("Port: " + PORT);
            System.out.println("Waiting for clients...");

            while (true) {

                Socket clientSocket = serverSocket.accept();

                System.out.println(
                        "Client connected: "
                        + clientSocket.getInetAddress().getHostAddress());

                threadPool.execute(
                        new ClientHandler(clientSocket));
            }

        } catch (IOException e) {

            System.err.println(
                    "Server error: " + e.getMessage());

        } finally {

            threadPool.shutdown();
        }
    }
}
