
package Authshield;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SecurityDashboard extends JFrame {

    private final JLabel successLabel = new JLabel("0");
    private final JLabel failedLabel = new JLabel("0");
    private final JLabel blockedLabel = new JLabel("0");

    private final DefaultTableModel tableModel =
            new DefaultTableModel(
                    new String[]{
                        "ID", "Time", "IP Address",
                        "Username", "Event Type", "Details"
                    }, 0) {

                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

    private final JTable eventTable = new JTable(tableModel);

    private final DateTimeFormatter timestampFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    public SecurityDashboard() {

        setTitle("AuthShield | Security Monitoring Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 720);
        setMinimumSize(new Dimension(900, 550));
        setLocationRelativeTo(null);

        getContentPane().setBackground(new Color(245, 247, 250));
        setLayout(new BorderLayout(0, 0));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(24, 39, 63));
        header.setBorder(
                BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel title = new JLabel("AuthShield");
        title.setFont(new Font("Arial", Font.BOLD, 27));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Security Monitoring Dashboard");
        subtitle.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitle.setForeground(new Color(205, 216, 232));

        JPanel headingText = new JPanel();
        headingText.setLayout(new BoxLayout(
                headingText, BoxLayout.Y_AXIS));
        headingText.setOpaque(false);

        headingText.add(title);
        headingText.add(Box.createVerticalStrut(5));
        headingText.add(subtitle);

        header.add(headingText, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // Main content
        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setBackground(new Color(245, 247, 250));
        content.setBorder(
                BorderFactory.createEmptyBorder(20, 22, 18, 22));

        // Statistics cards
        JPanel statsPanel = new JPanel(
                new GridLayout(1, 3, 18, 0));
        statsPanel.setOpaque(false);

        statsPanel.add(createStatCard(
                "SUCCESSFUL LOGINS",
                successLabel,
                new Color(24, 135, 84)));

        statsPanel.add(createStatCard(
                "FAILED LOGINS",
                failedLabel,
                new Color(205, 75, 65)));

        statsPanel.add(createStatCard(
                "BLOCK EVENTS",
                blockedLabel,
                new Color(205, 130, 35)));

        content.add(statsPanel, BorderLayout.NORTH);

        // Security events section
        JPanel eventsPanel = new JPanel(
                new BorderLayout(0, 12));
        eventsPanel.setBackground(Color.WHITE);

        eventsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        new Color(222, 227, 235)),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));

        JLabel eventsTitle = new JLabel("Recent Security Events");
        eventsTitle.setFont(new Font("Arial", Font.BOLD, 18));
        eventsTitle.setForeground(new Color(35, 48, 68));

        eventsPanel.add(eventsTitle, BorderLayout.NORTH);

        configureTable();

        JScrollPane scrollPane = new JScrollPane(eventTable);
        scrollPane.setBorder(
                BorderFactory.createLineBorder(
                        new Color(220, 225, 232)));

        scrollPane.getViewport().setBackground(Color.WHITE);

        scrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        scrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        eventsPanel.add(scrollPane, BorderLayout.CENTER);
        content.add(eventsPanel, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(245, 247, 250));
        footer.setBorder(
                BorderFactory.createEmptyBorder(0, 22, 15, 22));

        JLabel statusLabel = new JLabel(
                "Auto-refresh enabled | Updates every 3 seconds");

        statusLabel.setForeground(new Color(90, 101, 117));
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        JButton refreshButton = new JButton("Refresh Now");
        refreshButton.setFont(new Font("Arial", Font.BOLD, 13));
        refreshButton.setFocusPainted(false);
        refreshButton.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        refreshButton.addActionListener(
                e -> refreshDashboard());

        JPanel buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(refreshButton);

        footer.add(statusLabel, BorderLayout.WEST);
        footer.add(buttonPanel, BorderLayout.EAST);

        add(footer, BorderLayout.SOUTH);

        // Load data
        refreshDashboard();

        // Automatic refresh every 3 seconds
        Timer timer = new Timer(
                3000, e -> refreshDashboard());

        timer.start();
    }

    private JPanel createStatCard(
            String heading,
            JLabel valueLabel,
            Color accentColor) {

        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Color.WHITE);

        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(
                        0, 4, 0, 0, accentColor),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(225, 230, 237)),
                        BorderFactory.createEmptyBorder(
                                15, 17, 15, 17))));

        JLabel headingLabel = new JLabel(heading);
        headingLabel.setFont(
                new Font("Arial", Font.BOLD, 12));
        headingLabel.setForeground(
                new Color(95, 106, 122));

        valueLabel.setFont(
                new Font("Arial", Font.BOLD, 32));
        valueLabel.setForeground(accentColor);

        card.add(headingLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private void configureTable() {

        eventTable.setFont(
                new Font("Arial", Font.PLAIN, 13));

        eventTable.setRowHeight(32);
        eventTable.setShowVerticalLines(false);
        eventTable.setShowHorizontalLines(true);
        eventTable.setGridColor(new Color(232, 235, 240));

        eventTable.setSelectionBackground(
                new Color(220, 234, 250));
        eventTable.setSelectionForeground(
                new Color(30, 45, 65));

        eventTable.setFillsViewportHeight(true);
        eventTable.setAutoCreateRowSorter(true);

        // Keep columns readable and enable horizontal scrolling.
        eventTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        int[] widths = {65, 175, 125, 130, 125, 390};

        for (int i = 0; i < widths.length; i++) {
            eventTable.getColumnModel()
                    .getColumn(i)
                    .setPreferredWidth(widths[i]);
        }

        JTableHeader tableHeader = eventTable.getTableHeader();

        tableHeader.setFont(
                new Font("Arial", Font.BOLD, 12));
        tableHeader.setBackground(
                new Color(235, 240, 247));
        tableHeader.setForeground(
                new Color(42, 55, 75));
        tableHeader.setPreferredSize(
                new Dimension(0, 38));
        tableHeader.setReorderingAllowed(false);

        // Center ID and IP address values.
        DefaultTableCellRenderer centered =
                new DefaultTableCellRenderer();

        centered.setHorizontalAlignment(
                SwingConstants.CENTER);

        eventTable.getColumnModel()
                .getColumn(0)
                .setCellRenderer(centered);

        eventTable.getColumnModel()
                .getColumn(2)
                .setCellRenderer(centered);

        // Color event types.
        eventTable.getColumnModel()
                .getColumn(4)
                .setCellRenderer(new EventTypeRenderer());

        // No custom mouse-movement tooltip is added.
    }

    private static class EventTypeRenderer
            extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            Component component =
                    super.getTableCellRendererComponent(
                            table, value, isSelected,
                            hasFocus, row, column);

            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(new Font("Arial", Font.BOLD, 12));

            if (!isSelected) {

                String type =
                        value == null ? "" : value.toString();

                switch (type) {

                    case "SUCCESS":
                        setForeground(new Color(24, 135, 84));
                        break;

                    case "FAILED":
                        setForeground(new Color(205, 75, 65));
                        break;

                    case "BLOCKED":
                        setForeground(new Color(205, 130, 35));
                        break;

                    default:
                        setForeground(Color.DARK_GRAY);
                }
            }

            return component;
        }
    }

    private void refreshDashboard() {

        int successes = EventStore.countByType("SUCCESS");
        int failures = EventStore.countByType("FAILED");
        int blocks = EventStore.countByType("BLOCKED");

        successLabel.setText(String.valueOf(successes));
        failedLabel.setText(String.valueOf(failures));
        blockedLabel.setText(String.valueOf(blocks));

        List<String[]> events = EventStore.recentEvents(200);

        tableModel.setRowCount(0);

        for (String[] event : events) {

            // Format the database timestamp for easier reading.
            if (event[1] != null) {
                try {
                    event[1] = timestampFormatter.format(
                            Instant.parse(event[1]));
                } catch (Exception e) {
                    // Preserve the original timestamp if parsing fails.
                }
            }

            tableModel.addRow(event);
        }
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            SecurityDashboard dashboard =
                    new SecurityDashboard();

            dashboard.setVisible(true);
        });
    }
}
