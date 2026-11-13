
package Authshield;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
    	
    	String connectionId = clientSocket.getRemoteSocketAddress()
    	        + "-" + System.identityHashCode(clientSocket);

        String clientIP =
                clientSocket.getInetAddress().getHostAddress();

        try (
            Socket socket = clientSocket;

            BufferedReader input = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream()));

            PrintWriter output = new PrintWriter(
                    socket.getOutputStream(), true)
        ) {

            while (true) {

                String username = input.readLine();

                if (username == null) {
                    break;
                }

                if (username.equalsIgnoreCase("exit")) {
                    break;
                }

                String password = input.readLine();

                if (password == null) {
                    break;
                }

                String result = AuthService.authenticate(
                        username, password, clientIP, connectionId);

                if ("LOGIN_SUCCESS".equals(result)) {

                    output.println("Login Successfully");
                    break;

                } else if ("TEMPORARILY_BLOCKED".equals(result)) {

                    output.println(
                            "Temporarily Blocked");
                    break;

                } else {

                    output.println("Login Failed");
                }
            }

        } catch (IOException e) {

            System.err.println(
                    "Client connection error for "
                    + clientIP + ": " + e.getMessage());

        } finally {

            System.out.println(
                    "Client disconnected: " + clientIP);
        }
    }
}
