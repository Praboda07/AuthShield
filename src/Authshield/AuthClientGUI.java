package Authshield;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Swing login client for AuthShield. Network calls run on a background
 * thread (SwingWorker) so the window never freezes while waiting for the
 * server.
 *
 * Usage: java Authshield.AuthClientGUI [serverIP] [port]
 */
public class AuthClientGUI extends JFrame {

    private static final Color NAVY = new Color(24, 39, 63);
    private static final Color BG = new Color(245, 247, 250);
    private static final Color GREEN = new Color(24, 135, 84);
    private static final Color RED = new Color(205, 75, 65);
    private static final Color ORANGE = new Color(205, 130, 35);
    private static final Color BLUE = new Color(45, 110, 200);
    private static final Color MUTED = new Color(95, 106, 122);

    private final JTextField hostField;
    private final JTextField portField;
    private final JTextField userField = new JTextField(18);
    private final JPasswordField passField = new JPasswordField(18);
    private final JButton loginButton = new JButton("Login");
    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel connectionLabel = new JLabel("Not connected");
    private final AttemptDots attemptDots = new AttemptDots();

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private Timer countdownTimer;

    public AuthClientGUI(String host, int port) {

        setTitle("AuthShield | Secure Login");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        hostField = new JTextField(host, 12);
        portField = new JTextField(String.valueOf(port), 5);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildForm(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        loginButton.addActionListener(e -> login());
        getRootPane().setDefaultButton(loginButton);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                if (output != null) {
                    output.println(Protocol.QUIT);
                }
                closeConnection();
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------ layout

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JLabel title = new JLabel("AuthShield");
        title.setFont(new Font("Dialog", Font.BOLD, 24));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Secure login with brute-force protection");
        subtitle.setFont(new Font("Dialog", Font.PLAIN, 13));
        subtitle.setForeground(new Color(205, 216, 232));

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        return header;
    }

    private JPanel buildForm() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(16, 16, 8, 16, BG),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(222, 227, 235)),
                        BorderFactory.createEmptyBorder(18, 20, 18, 20))));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 4, 5, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        // Server row
        JPanel server = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        server.setOpaque(false);
        server.add(hostField);
        server.add(new JLabel("  Port "));
        server.add(portField);

        addRow(card, c, 0, "Server IP", server);
        addRow(card, c, 1, "Username", userField);
        addRow(card, c, 2, "Password", passField);

        c.gridx = 1;
        c.gridy = 3;
        attemptDots.setOpaque(false);
        card.add(attemptDots, c);

        loginButton.setFont(new Font("Dialog", Font.BOLD, 14));
        loginButton.setBackground(BLUE);
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setOpaque(true);
        loginButton.setBorderPainted(false);
        loginButton.setPreferredSize(new Dimension(0, 38));
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        c.insets = new Insets(14, 4, 6, 4);
        card.add(loginButton, c);

        statusLabel.setFont(new Font("Dialog", Font.BOLD, 13));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setPreferredSize(new Dimension(320, 22));
        c.gridy = 5;
        c.insets = new Insets(4, 4, 0, 4);
        card.add(statusLabel, c);

        for (JComponent field : new JComponent[] {hostField, portField, userField, passField}) {
            field.setFont(new Font("Dialog", Font.PLAIN, 14));
        }
        return card;
    }

    private void addRow(JPanel panel, GridBagConstraints c, int row,
                        String label, JComponent field) {
        JLabel l = new JLabel(label);
        l.setFont(new Font("Dialog", Font.BOLD, 12));
        l.setForeground(MUTED);
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        panel.add(l, c);
        c.gridx = 1;
        c.weightx = 1;
        panel.add(field, c);
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(BG);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 18, 12, 18));
        connectionLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
        connectionLabel.setForeground(MUTED);
        footer.add(connectionLabel, BorderLayout.WEST);
        JLabel proto = new JLabel("TCP / port " + portField.getText());
        proto.setFont(new Font("Dialog", Font.PLAIN, 12));
        proto.setForeground(MUTED);
        footer.add(proto, BorderLayout.EAST);
        return footer;
    }

    // ------------------------------------------------------------ actions

    private void login() {
        String host = hostField.getText().trim();
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());

        if (host.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            showStatus("Please fill in server, username and password.", RED);
            return;
        }
        if (user.contains(" ")) {
            showStatus("Username cannot contain spaces.", RED);
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            showStatus("Port must be a number.", RED);
            return;
        }

        setBusy(true);
        showStatus("Contacting server...", MUTED);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws IOException {
                if (socket == null || socket.isClosed()) {
                    String greeting = openConnection(host, port);
                    if (greeting == null) {
                        throw new IOException("Server closed the connection");
                    }
                    if (!Protocol.command(greeting).equals(Protocol.WELCOME)) {
                        return greeting; // e.g. BLOCKED 42
                    }
                }
                output.println(Protocol.loginRequest(user, pass));
                String reply = input.readLine();
                if (reply == null) {
                    throw new IOException("Server closed the connection");
                }
                return reply;
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    handleReply(get());
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    closeConnection();
                    showStatus("Cannot reach server: " + cause.getMessage(), RED);
                }
            }
        }.execute();
    }

    private String openConnection(String host, int port) throws IOException {
        closeConnection();
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000);
        socket.setSoTimeout(10_000);
        input = new BufferedReader(new InputStreamReader(
                socket.getInputStream(), StandardCharsets.UTF_8));
        output = new PrintWriter(new OutputStreamWriter(
                socket.getOutputStream(), StandardCharsets.UTF_8), true);
        SwingUtilities.invokeLater(() -> connectionLabel.setText(
                "\u25CF Connected to " + host + ":" + port));
        return input.readLine();
    }

    private void handleReply(String reply) {
        String command = Protocol.command(reply);
        String arg = Protocol.argument(reply);

        switch (command) {
            case Protocol.SUCCESS:
                closeConnection();
                attemptDots.setRemaining(AuthService.MAX_ATTEMPTS);
                showStatus("Login successful. " + arg, GREEN);
                passField.setText("");
                JOptionPane.showMessageDialog(this,
                        arg + "\nYou are now authenticated.",
                        "AuthShield", JOptionPane.INFORMATION_MESSAGE);
                break;

            case Protocol.FAILED:
                int left = parseInt(arg);
                attemptDots.setRemaining(left);
                passField.setText("");
                passField.requestFocusInWindow();
                showStatus("Wrong username or password. "
                        + left + " attempt" + (left == 1 ? "" : "s") + " left.", RED);
                break;

            case Protocol.BLOCKED:
                closeConnection();
                attemptDots.setRemaining(0);
                passField.setText("");
                startCountdown(parseInt(arg));
                break;

            default:
                showStatus("Server: " + reply, ORANGE);
        }
    }

    private void startCountdown(int seconds) {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        final int[] left = {seconds};
        setFormEnabled(false);
        showStatus("Too many attempts. Blocked for " + left[0] + "s", ORANGE);

        countdownTimer = new Timer(1000, e -> {
            left[0]--;
            if (left[0] <= 0) {
                countdownTimer.stop();
                setFormEnabled(true);
                attemptDots.setRemaining(AuthService.MAX_ATTEMPTS);
                showStatus("Block expired. You can try again.", BLUE);
            } else {
                showStatus("Too many attempts. Blocked for " + left[0] + "s", ORANGE);
            }
        });
        countdownTimer.start();
    }

    // ------------------------------------------------------------ helpers

    private void closeConnection() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
        socket = null;
        input = null;
        output = null;
        connectionLabel.setText("Not connected");
    }

    private void setBusy(boolean busy) {
        loginButton.setEnabled(!busy);
        loginButton.setText(busy ? "Please wait..." : "Login");
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : null);
    }

    private void setFormEnabled(boolean enabled) {
        loginButton.setEnabled(enabled);
        userField.setEnabled(enabled);
        passField.setEnabled(enabled);
        loginButton.setBackground(enabled ? BLUE : new Color(170, 178, 190));
    }

    private void showStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Small indicator showing how many attempts are left. */
    private static class AttemptDots extends JComponent {
        private int remaining = AuthService.MAX_ATTEMPTS;

        AttemptDots() {
            setPreferredSize(new Dimension(220, 18));
        }

        void setRemaining(int remaining) {
            this.remaining = Math.max(0, remaining);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int x = 0;
            for (int i = 0; i < AuthService.MAX_ATTEMPTS; i++) {
                g2.setColor(i < remaining ? GREEN : new Color(220, 224, 230));
                g2.fillOval(x, 3, 12, 12);
                x += 18;
            }
            g2.setColor(MUTED);
            g2.setFont(new Font("Dialog", Font.PLAIN, 11));
            g2.drawString("attempts left", x + 4, 14);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : Protocol.DEFAULT_PORT;

        SwingUtilities.invokeLater(() -> new AuthClientGUI(host, port).setVisible(true));
    }
}
