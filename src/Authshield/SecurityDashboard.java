package Authshield;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;

import java.awt.*;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Security monitoring dashboard. Runs as a separate process from the
 * server and reads the shared SQLite database every 3 seconds.
 */
public class SecurityDashboard extends JFrame {

    private static final Color NAVY = new Color(24, 39, 63);
    private static final Color BG = new Color(245, 247, 250);
    private static final Color BORDER = new Color(222, 227, 235);
    private static final Color TEXT = new Color(35, 48, 68);
    private static final Color MUTED = new Color(95, 106, 122);
    private static final Color GREEN = new Color(24, 135, 84);
    private static final Color RED = new Color(205, 75, 65);
    private static final Color ORANGE = new Color(205, 130, 35);
    private static final Color PURPLE = new Color(120, 80, 190);
    private static final Color BLUE = new Color(45, 110, 200);

    private static final int REFRESH_MS = 3000;

    private final JLabel successLabel = new JLabel("0");
    private final JLabel failedLabel = new JLabel("0");
    private final JLabel blockedLabel = new JLabel("0");
    private final JLabel activeBlocksLabel = new JLabel("0");
    private final JLabel uniqueIpLabel = new JLabel("0 unique IPs seen");
    private final JLabel clockLabel = new JLabel();
    private final JLabel lastUpdatedLabel = new JLabel();

    private final JComboBox<String> typeFilter = new JComboBox<>(
            new String[] {"All events", "SUCCESS", "FAILED", "BLOCKED"});
    private final JTextField searchField = new JTextField(14);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] {"ID", "Time", "IP Address", "Username",
                          "Event Type", "Details"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable eventTable = new JTable(tableModel);
    private final TableRowSorter<DefaultTableModel> sorter =
            new TableRowSorter<>(tableModel);

    private final DefaultListModel<String> blockListModel = new DefaultListModel<>();

    private final DateTimeFormatter timestampFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());
    private final DateTimeFormatter clockFormatter =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    public SecurityDashboard() {

        setTitle("AuthShield | Security Monitoring Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1240, 760);
        setMinimumSize(new Dimension(980, 600));
        setLocationRelativeTo(null);

        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setBackground(BG);
        content.setBorder(BorderFactory.createEmptyBorder(20, 22, 14, 22));
        content.add(buildStats(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(18, 0));
        center.setOpaque(false);
        center.add(buildEventsPanel(), BorderLayout.CENTER);
        center.add(buildBlocksPanel(), BorderLayout.EAST);
        content.add(center, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        refreshDashboard();

        new Timer(REFRESH_MS, e -> refreshDashboard()).start();
        new Timer(1000, e -> clockLabel.setText(
                LocalTime.now().format(clockFormatter))).start();
    }

    // ------------------------------------------------------------ layout

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(18, 25, 18, 25));

        JLabel title = new JLabel("AuthShield");
        title.setFont(new Font("Dialog", Font.BOLD, 27));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Security Monitoring Dashboard  |  "
                + AuthService.MAX_ATTEMPTS + " failed logins = "
                + (AuthService.BLOCK_TIME / 1000) + "s IP block");
        subtitle.setFont(new Font("Dialog", Font.PLAIN, 14));
        subtitle.setForeground(new Color(205, 216, 232));

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        text.add(title);
        text.add(Box.createVerticalStrut(5));
        text.add(subtitle);

        JLabel live = new JLabel("● LIVE");
        live.setFont(new Font("Dialog", Font.BOLD, 13));
        live.setForeground(new Color(90, 210, 140));

        clockLabel.setFont(new Font("Dialog", Font.BOLD, 22));
        clockLabel.setForeground(Color.WHITE);
        clockLabel.setText(LocalTime.now().format(clockFormatter));

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);
        live.setAlignmentX(Component.RIGHT_ALIGNMENT);
        clockLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        right.add(clockLabel);
        right.add(Box.createVerticalStrut(4));
        right.add(live);

        header.add(text, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildStats() {
        JPanel stats = new JPanel(new GridLayout(1, 4, 18, 0));
        stats.setOpaque(false);
        stats.add(createStatCard("SUCCESSFUL LOGINS", successLabel, GREEN));
        stats.add(createStatCard("FAILED LOGINS", failedLabel, RED));
        stats.add(createStatCard("BLOCK EVENTS", blockedLabel, ORANGE));
        stats.add(createStatCard("IPs BLOCKED NOW", activeBlocksLabel, PURPLE));
        return stats;
    }

    private JPanel buildEventsPanel() {
        JPanel panel = whiteCard();
        panel.setLayout(new BorderLayout(0, 12));

        JLabel title = new JLabel("Recent Security Events");
        title.setFont(new Font("Dialog", Font.BOLD, 18));
        title.setForeground(TEXT);

        typeFilter.setFont(new Font("Dialog", Font.PLAIN, 13));
        typeFilter.addActionListener(e -> refreshDashboard());

        searchField.setFont(new Font("Dialog", Font.PLAIN, 13));
        searchField.setToolTipText("Filter by IP or username");
        searchField.getDocument().addDocumentListener(
                new javax.swing.event.DocumentListener() {
                    public void insertUpdate(javax.swing.event.DocumentEvent e) { applySearch(); }
                    public void removeUpdate(javax.swing.event.DocumentEvent e) { applySearch(); }
                    public void changedUpdate(javax.swing.event.DocumentEvent e) { applySearch(); }
                });

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);
        controls.add(mutedLabel("Search IP / user"));
        controls.add(searchField);
        controls.add(mutedLabel("Type"));
        controls.add(typeFilter);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.WEST);
        top.add(controls, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        configureTable();

        JScrollPane scroll = new JScrollPane(eventTable);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildBlocksPanel() {
        JPanel panel = whiteCard();
        panel.setLayout(new BorderLayout(0, 10));
        panel.setPreferredSize(new Dimension(260, 0));

        JLabel title = new JLabel("Active IP Blocks");
        title.setFont(new Font("Dialog", Font.BOLD, 18));
        title.setForeground(TEXT);

        JList<String> list = new JList<>(blockListModel);
        list.setFont(new Font("Monospaced", Font.BOLD, 14));
        list.setForeground(PURPLE);
        list.setFixedCellHeight(30);
        list.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int a, int b) {
                // read-only list
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));

        uniqueIpLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
        uniqueIpLabel.setForeground(MUTED);

        panel.add(title, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(uniqueIpLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(BG);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 22, 15, 22));

        lastUpdatedLabel.setForeground(MUTED);
        lastUpdatedLabel.setFont(new Font("Dialog", Font.PLAIN, 12));

        JButton clearButton = styledButton("Clear Logs", RED);
        clearButton.addActionListener(e -> clearLogs());

        JButton refreshButton = styledButton("Refresh Now", BLUE);
        refreshButton.addActionListener(e -> refreshDashboard());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(clearButton);
        buttons.add(refreshButton);

        footer.add(lastUpdatedLabel, BorderLayout.WEST);
        footer.add(buttons, BorderLayout.EAST);
        return footer;
    }

    private JPanel createStatCard(String heading, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        BorderFactory.createEmptyBorder(14, 17, 14, 17))));

        JLabel headingLabel = new JLabel(heading);
        headingLabel.setFont(new Font("Dialog", Font.BOLD, 12));
        headingLabel.setForeground(MUTED);

        valueLabel.setFont(new Font("Dialog", Font.BOLD, 32));
        valueLabel.setForeground(accent);

        card.add(headingLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel whiteCard() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        return panel;
    }

    private JLabel mutedLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Dialog", Font.PLAIN, 12));
        l.setForeground(MUTED);
        return l;
    }

    private JButton styledButton(String text, Color color) {
        JButton b = new JButton(text);
        b.setFont(new Font("Dialog", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setForeground(Color.WHITE);
        b.setBackground(color);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setPreferredSize(new Dimension(130, 34));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void configureTable() {
        eventTable.setFont(new Font("Dialog", Font.PLAIN, 13));
        eventTable.setRowHeight(30);
        eventTable.setShowVerticalLines(false);
        eventTable.setShowHorizontalLines(true);
        eventTable.setGridColor(new Color(232, 235, 240));
        eventTable.setSelectionBackground(new Color(220, 234, 250));
        eventTable.setSelectionForeground(new Color(30, 45, 65));
        eventTable.setFillsViewportHeight(true);
        eventTable.setRowSorter(sorter);
        eventTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        int[] widths = {55, 160, 120, 110, 105, 330};
        for (int i = 0; i < widths.length; i++) {
            eventTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JTableHeader header = eventTable.getTableHeader();
        header.setFont(new Font("Dialog", Font.BOLD, 12));
        header.setBackground(new Color(235, 240, 247));
        header.setForeground(new Color(42, 55, 75));
        header.setPreferredSize(new Dimension(0, 36));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer centered = new DefaultTableCellRenderer();
        centered.setHorizontalAlignment(SwingConstants.CENTER);
        eventTable.getColumnModel().getColumn(0).setCellRenderer(centered);
        eventTable.getColumnModel().getColumn(2).setCellRenderer(centered);
        eventTable.getColumnModel().getColumn(4).setCellRenderer(new EventTypeRenderer());
    }

    /** Coloured badge-style text for the event type column. */
    private static class EventTypeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(new Font("Dialog", Font.BOLD, 12));

            if (!isSelected) {
                String type = value == null ? "" : value.toString();
                switch (type) {
                    case "SUCCESS": setForeground(GREEN); break;
                    case "FAILED":  setForeground(RED); break;
                    case "BLOCKED": setForeground(ORANGE); break;
                    default:        setForeground(Color.DARK_GRAY);
                }
            }
            return this;
        }
    }

    // ------------------------------------------------------------ data

    private void applySearch() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            // Match against IP Address (2) and Username (3) columns.
            sorter.setRowFilter(RowFilter.regexFilter(
                    "(?i)" + java.util.regex.Pattern.quote(text), 2, 3));
        }
    }

    private void clearLogs() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete all stored security events?\n"
                        + "(Active blocks on the server are not affected.)",
                "Clear Logs", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            EventStore.clearEvents();
            refreshDashboard();
        }
    }

    private void refreshDashboard() {

        successLabel.setText(String.valueOf(EventStore.countByType("SUCCESS")));
        failedLabel.setText(String.valueOf(EventStore.countByType("FAILED")));
        blockedLabel.setText(String.valueOf(EventStore.countByType("BLOCKED")));

        Map<String, Long> blocks = EventStore.activeBlocks(AuthService.BLOCK_TIME);
        activeBlocksLabel.setText(String.valueOf(blocks.size()));

        blockListModel.clear();
        if (blocks.isEmpty()) {
            blockListModel.addElement(" No IPs blocked");
        } else {
            for (Map.Entry<String, Long> b : blocks.entrySet()) {
                blockListModel.addElement(String.format(" %-15s %3ds",
                        b.getKey(), b.getValue()));
            }
        }

        uniqueIpLabel.setText(EventStore.countDistinctIPs() + " unique IPs seen");

        String selected = (String) typeFilter.getSelectedItem();
        String type = "All events".equals(selected) ? null : selected;
        List<String[]> events = EventStore.recentEvents(300, type);

        tableModel.setRowCount(0);
        for (String[] event : events) {
            if (event[1] != null) {
                try {
                    event[1] = timestampFormatter.format(Instant.parse(event[1]));
                } catch (Exception ignored) {
                    // keep original value
                }
            }
            tableModel.addRow(event);
        }

        lastUpdatedLabel.setText("Auto-refresh every " + (REFRESH_MS / 1000)
                + "s  |  Last updated " + LocalTime.now().format(clockFormatter)
                + "  |  Showing " + events.size() + " events");
    }

    public static void main(String[] args) {
        // Make sure the table exists even if the server has not run yet.
        EventStore.initialize();
        SwingUtilities.invokeLater(() -> new SecurityDashboard().setVisible(true));
    }
}
