import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class ExpenseTracker extends JFrame {
    private static final String FILE_NAME = "expenses.csv";
    private final JTextField nameField = new JTextField(12);
    private final JTextField amountField = new JTextField(8);
    private final JComboBox<String> categoryBox = new JComboBox<>(
            new String[]{"Food", "Travel", "Shopping", "Education", "Bills", "Health", "Other"});
    private final JTextField dateField = new JTextField(LocalDate.now().toString(), 10);
    private final JTextField budgetField = new JTextField("10000", 8);
    private final JLabel totalLabel = new JLabel("Total expenses: ₹0.00");
    private final JLabel remainingLabel = new JLabel("Remaining budget: ₹10000.00");
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Name", "Amount (₹)", "Category", "Date"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final List<String[]> expenses = new ArrayList<>();

    public ExpenseTracker() {
        setTitle("Expense Tracker System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 520);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JLabel heading = new JLabel("Expense Tracker System", SwingConstants.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 24));
        add(heading, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new GridLayout(2, 4, 8, 8));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Enter Expense Details"));
        inputPanel.add(new JLabel("Expense Name:"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("Amount (₹):"));
        inputPanel.add(amountField);
        inputPanel.add(new JLabel("Category:"));
        inputPanel.add(categoryBox);
        inputPanel.add(new JLabel("Date (YYYY-MM-DD):"));
        inputPanel.add(dateField);

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.add(inputPanel, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton addButton = new JButton("Add Expense");
        JButton deleteButton = new JButton("Delete Selected");
        JButton budgetButton = new JButton("Update Budget");
        JButton clearButton = new JButton("Clear Fields");
        buttons.add(addButton);
        buttons.add(deleteButton);
        buttons.add(budgetButton);
        buttons.add(clearButton);
        topPanel.add(buttons, BorderLayout.SOUTH);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.add(topPanel, BorderLayout.NORTH);
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        JPanel summary = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 8));
        summary.add(totalLabel);
        summary.add(new JLabel("Monthly budget (₹):"));
        summary.add(budgetField);
        summary.add(remainingLabel);
        add(summary, BorderLayout.SOUTH);

        addButton.addActionListener(e -> addExpense());
        deleteButton.addActionListener(e -> deleteExpense());
        budgetButton.addActionListener(e -> updateSummary());
        clearButton.addActionListener(e -> clearFields());

        loadExpenses();
        refreshTable();
        updateSummary();
    }

    private void addExpense() {
        String name = nameField.getText().trim();
        String amountText = amountField.getText().trim();
        String category = (String) categoryBox.getSelectedItem();
        String date = dateField.getText().trim();

        if (name.isEmpty() || amountText.isEmpty() || date.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all expense details.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
            if (!Double.isFinite(amount) || amount <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid amount greater than zero.");
            return;
        }

        try {
            LocalDate.parse(date);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Enter the date in YYYY-MM-DD format.");
            return;
        }

        expenses.add(new String[]{name, String.format(java.util.Locale.US, "%.2f", amount), category, date});
        saveExpenses();
        refreshTable();
        updateSummary();
        clearFields();
    }

    private void deleteExpense() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an expense to delete.");
            return;
        }
        expenses.remove(selectedRow);
        saveExpenses();
        refreshTable();
        updateSummary();
    }

    private void clearFields() {
        nameField.setText("");
        amountField.setText("");
        dateField.setText(LocalDate.now().toString());
        categoryBox.setSelectedIndex(0);
        nameField.requestFocus();
    }

    private double calculateTotal() {
        double total = 0;
        for (String[] expense : expenses) {
            try { total += Double.parseDouble(expense[1]); }
            catch (NumberFormatException ignored) { }
        }
        return total;
    }

    private void updateSummary() {
        double budget;
        try {
            budget = Double.parseDouble(budgetField.getText().trim());
            if (!Double.isFinite(budget) || budget < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid budget (zero or greater).");
            budgetField.requestFocus();
            return;
        }
        double total = calculateTotal();
        totalLabel.setText(String.format(java.util.Locale.US, "Total expenses: ₹%.2f", total));
        remainingLabel.setText(String.format(java.util.Locale.US, "Remaining budget: ₹%.2f", budget - total));
        remainingLabel.setForeground(budget - total < 0 ? Color.RED : new Color(0, 110, 0));
    }

    private void refreshTable() {
        model.setRowCount(0);
        for (String[] expense : expenses) {
            model.addRow(new Object[]{expense[0], expense[1], expense[2], expense[3]});
        }
    }

    private void saveExpenses() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (String[] expense : expenses) {
                writer.println(csvEscape(expense[0]) + "," + csvEscape(expense[1]) + ","
                        + csvEscape(expense[2]) + "," + csvEscape(expense[3]));
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save expenses: " + ex.getMessage());
        }
    }

    private String csvEscape(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private void loadExpenses() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = parseCsvLine(line);
                if (fields.length == 4) expenses.add(fields);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not load saved expenses: " + ex.getMessage());
        }
    }

    // Handles quoted CSV fields, including commas and escaped quote characters.
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString());
        return fields.toArray(new String[0]);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ExpenseTracker().setVisible(true));
    }
}
