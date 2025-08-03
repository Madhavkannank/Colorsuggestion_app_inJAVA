import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.formdev.flatlaf.FlatDarculaLaf;

public class ColorSuggestionGUI extends JFrame {
    private JComboBox<String> themeComboBox;
    private JTextArea themeDisplayArea;
    private JPanel colorDisplayPanel;
    private List<Integer> themeIds = new ArrayList<>();

    public ColorSuggestionGUI() {
        setTitle("Color Suggestion App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (UnsupportedLookAndFeelException ex) {
            System.err.println("Failed to initialize FlatLaf: " + ex);
        }

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        topPanel.add(new JLabel("Select Theme:"));
        themeComboBox = new JComboBox<>();
        themeComboBox.setPreferredSize(new Dimension(200, 30));
        topPanel.add(themeComboBox);

        JButton loadButton = new JButton("Load Colors");
        loadButton.setFont(loadButton.getFont().deriveFont(Font.BOLD, 14f));
        topPanel.add(loadButton);

        JButton addThemeButton = new JButton("Add Theme");
        addThemeButton.setFont(addThemeButton.getFont().deriveFont(Font.BOLD, 14f));
        topPanel.add(addThemeButton);

        JButton addColorButton = new JButton("Add Color");
        addColorButton.setFont(addColorButton.getFont().deriveFont(Font.BOLD, 14f));
        topPanel.add(addColorButton);

        JButton exitButton = new JButton("Exit");
        exitButton.setFont(exitButton.getFont().deriveFont(Font.BOLD, 14f));
        topPanel.add(exitButton);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        themeDisplayArea = new JTextArea(10, 30);
        themeDisplayArea.setEditable(false);
        themeDisplayArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        themeDisplayArea.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        JScrollPane scrollPane = new JScrollPane(themeDisplayArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        colorDisplayPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        colorDisplayPanel.setBorder(BorderFactory.createTitledBorder("Suggested Colors"));
        colorDisplayPanel.setBackground(UIManager.getColor("Panel.background"));

        mainPanel.add(colorDisplayPanel, BorderLayout.SOUTH);

        add(mainPanel);

        loadThemes();

        loadButton.addActionListener(e -> loadColors());
        addThemeButton.addActionListener(e -> addTheme());
        addColorButton.addActionListener(e -> addColor());
        exitButton.addActionListener(e -> System.exit(0));
    }

    private void loadThemes() {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/color_suggestion_app", "root", "Madhav@123");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, theme_name FROM themes")) {

            themeComboBox.removeAllItems();
            themeIds.clear();

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("theme_name");
                themeComboBox.addItem(name);
                themeIds.add(id);
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Failed to load themes: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadColors() {
        int selectedIndex = themeComboBox.getSelectedIndex();
        if (selectedIndex == -1) {
            JOptionPane.showMessageDialog(this, "Please select a theme.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int themeId = themeIds.get(selectedIndex);
        themeDisplayArea.setText("");
        colorDisplayPanel.removeAll();

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/color_suggestion_app", "root", "Madhav@123");
             PreparedStatement ps = conn.prepareStatement("SELECT color_name, color_hex FROM colors WHERE theme_id = ?")) {

            ps.setInt(1, themeId);
            ResultSet rs = ps.executeQuery();

            boolean colorsFound = false;
            while (rs.next()) {
                colorsFound = true;
                String name = rs.getString("color_name");
                String hex = rs.getString("color_hex");
                themeDisplayArea.append(name + " (" + hex + ")\n");

                JPanel colorBox = new JPanel();
                colorBox.setPreferredSize(new Dimension(80, 40));
                colorBox.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

                try {
                    colorBox.setBackground(Color.decode(hex));
                } catch (NumberFormatException e) {
                    colorBox.setBackground(Color.LIGHT_GRAY);
                    colorBox.setToolTipText("Invalid hex: " + hex);
                }

                colorBox.setToolTipText(name + " (" + hex + ")");
                colorDisplayPanel.add(colorBox);
            }

            if (!colorsFound) {
                themeDisplayArea.setText("No colors found for this theme.");
            }

            colorDisplayPanel.revalidate();
            colorDisplayPanel.repaint();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Failed to load colors: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addTheme() {
        JTextField nameField = new JTextField();
        JTextField descField = new JTextField();
        Object[] inputs = {"Theme Name:", nameField, "Description:", descField};

        int result = JOptionPane.showConfirmDialog(this, inputs, "Add Theme", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/color_suggestion_app", "root", "Madhav@123");
                 PreparedStatement stmt = conn.prepareStatement("INSERT INTO themes(theme_name, description) VALUES(?, ?)");) {
                stmt.setString(1, nameField.getText());
                stmt.setString(2, descField.getText());
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Theme added successfully!");
                loadThemes();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error adding theme: " + e.getMessage());
            }
        }
    }

    private void addColor() {
        if (themeComboBox.getItemCount() == 0) {
            JOptionPane.showMessageDialog(this, "No themes available. Add a theme first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int selectedIndex = themeComboBox.getSelectedIndex();
        int themeId = themeIds.get(selectedIndex);

        JTextField colorNameField = new JTextField();
        JTextField colorHexField = new JTextField();
        Object[] inputs = {"Color Name:", colorNameField, "Hex Code:", colorHexField};

        int result = JOptionPane.showConfirmDialog(this, inputs, "Add Color", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/color_suggestion_app", "root", "Madhav@123");
                 PreparedStatement stmt = conn.prepareStatement("INSERT INTO colors(theme_id, color_name, color_hex) VALUES(?, ?, ?)")) {
                stmt.setInt(1, themeId);
                stmt.setString(2, colorNameField.getText());
                stmt.setString(3, colorHexField.getText());
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Color added successfully!");
                loadColors();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error adding color: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ColorSuggestionGUI().setVisible(true));
    }
}
