package Authshield;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AuthShield TCP server. Accepts clients on the main thread and hands
 * each connection to a worker thread from a fixed-size thread pool.
 *
 * Usage: java Authshield.AuthServer [port]
 */
public class AuthServer {

    private static final int MAX_CLIENT_THREADS = 10;

    private static final AtomicInteger activeClients = new AtomicInteger();

    static void clientConnected() {
        System.out.println("Active clients: " + activeClients.incrementAndGet());
    }

    static void clientDisconnected() {
        System.out.println("Active clients: " + activeClients.decrementAndGet());
    }

    public static void main(String[] args) {

        int port = Protocol.DEFAULT_PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        EventStore.initialize();

        ExecutorService threadPool =
                Executors.newFixedThreadPool(MAX_CLIENT_THREADS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down AuthShield server...");
            threadPool.shutdownNow();
        }));

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("=======================================");
            System.out.println("  AuthShield server started");
            System.out.println("  Port        : " + port);
            System.out.println("  Server IP   : " + localAddress());
            System.out.println("  Thread pool : " + MAX_CLIENT_THREADS);
            System.out.println("  Block rule  : " + AuthService.MAX_ATTEMPTS
                    + " failures -> " + (AuthService.BLOCK_TIME / 1000)
                    + "s block per IP");
            System.out.println("=======================================");
            System.out.println("Waiting for clients...");

            while (true) {

                Socket clientSocket = serverSocket.accept();

                System.out.println("Client connected: "
                        + clientSocket.getInetAddress().getHostAddress()
                        + ":" + clientSocket.getPort());

                threadPool.execute(new ClientHandler(clientSocket));
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());

        } finally {
            threadPool.shutdown();
        }
    }

    /** LAN IPv4 address(es) clients should connect to. */
    private static String localAddress() {
        StringBuilder found = new StringBuilder();
        try {
            for (NetworkInterface nic : java.util.Collections.list(
                    NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual()) {
                    continue;
                }
                for (InetAddress addr : java.util.Collections.list(
                        nic.getInetAddresses())) {
                    if (addr instanceof Inet4Address && addr.isSiteLocalAddress()) {
                        if (found.length() > 0) {
                            found.append(", ");
                        }
                        found.append(addr.getHostAddress());
                    }
                }
            }
        } catch (IOException e) {
            // fall through
        }
        return found.length() > 0 ? found.toString() : "127.0.0.1 (no LAN found)";
    }
}
