
package Authshield;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class Authclient {

    public static void main(String[] args) {

        try (
        		Socket socket = new Socket("192.168.8.195", 6363);

            BufferedReader keyboard = new BufferedReader(
                    new InputStreamReader(System.in));

            BufferedReader input = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream()));

            PrintWriter output = new PrintWriter(
                    socket.getOutputStream(), true)
        ) {

            System.out.println("Connected to AuthShield server.");

            while (true) {

                System.out.print("Enter username (or exit): ");
                String username = keyboard.readLine();

                if (username == null ||
                        username.equalsIgnoreCase("exit")) {
                    break;
                }

                System.out.print("Enter password: ");
                String password = keyboard.readLine();

                if (password == null) {
                    break;
                }

                output.println(username);
                output.println(password);

                String response = input.readLine();

                if (response == null) {
                    System.out.println(
                            "Server disconnected the client.");
                    break;
                }

                switch (response) {
                    case "Login Successfully":
                        System.out.println("Login successful!");
                        return;

                    case "Temporarily Blocked":
                        System.out.println(
                                "Too many failed attempts. "
                                + "Temporarily blocked for 60 seconds.");
                        return;

                    default:
                        System.out.println(
                                "Login failed. Please try again.");
                }
            }

        } catch (Exception e) {
            System.out.println(
                    "Client error: " + e.getMessage());
        }
    }
}
