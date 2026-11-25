package ui;

import exception.AppException;
import model.Expense;
import model.ExpenseCategory;
import model.User;
import service.ExpenseService;
import util.ConsoleFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

/**
 * Modern Business Operating Expenses Management panel.
 */
public class ExpensePanel extends JPanel {

    private final ExpenseService expenseService;
    private final User currentUser;

    private JTable expenseTable;
    private DefaultTableModel tableModel;
    private JLabel totalExpenseLabel;

    private JTextField startDateField;
    private JTextField endDateField;

    public ExpensePanel(ExpenseService expenseService, User currentUser) {
        this.expenseService = expenseService;
        this.currentUser = currentUser;
        initUI();
        refreshTable();
    }

    private void initUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(Theme.APP_BG);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel topContainer = new JPanel(new BorderLayout(10, 10));
        topContainer.setOpaque(false);

        // Header Row
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel title = new JLabel("Operating Expenses & Cost Tracking");
        title.setFont(Theme.FONT_TITLE_LARGE);
        title.setForeground(Theme.TEXT_PRIMARY);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionButtons.setOpaque(false);

        JButton addBtn = Theme.createPrimaryButton("+ Record Expense");
        addBtn.addActionListener(e -> showRecordExpenseDialog());

        JButton catBtn = Theme.createSecondaryButton("📁 Categories");
        catBtn.addActionListener(e -> showCategoryManagerDialog());

        JButton delBtn = Theme.createDangerButton("🗑 Delete");
        delBtn.addActionListener(e -> handleDeleteExpense());

        JButton refreshBtn = Theme.createSecondaryButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> refreshTable());

        actionButtons.add(addBtn);
        actionButtons.add(catBtn);
        actionButtons.add(delBtn);
        actionButtons.add(refreshBtn);

        headerRow.add(title, BorderLayout.WEST);
        headerRow.add(actionButtons, BorderLayout.EAST);
        topContainer.add(headerRow, BorderLayout.NORTH);

        // Filter and Total summary strip
        JPanel filterStrip = new JPanel(new BorderLayout());
        filterStrip.setBackground(Theme.SURFACE);
        filterStrip.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.BORDER, 1),
                new EmptyBorder(10, 14, 10, 14)
        ));

        JPanel dateFilterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        dateFilterPanel.setOpaque(false);

        LocalDate now = LocalDate.now();
        startDateField = Theme.createTextField(10);
        startDateField.setText(now.withDayOfMonth(1).toString());

        endDateField = Theme.createTextField(10);
        endDateField.setText(now.toString());

        JButton applyFilterBtn = Theme.createSecondaryButton("Apply Date Range");
        applyFilterBtn.addActionListener(e -> applyDateFilter());

        JButton showAllBtn = Theme.createSecondaryButton("View All");
        showAllBtn.addActionListener(e -> refreshTable());

        dateFilterPanel.add(new JLabel("From:"));
        dateFilterPanel.add(startDateField);
        dateFilterPanel.add(new JLabel("To:"));
        dateFilterPanel.add(endDateField);
        dateFilterPanel.add(applyFilterBtn);
        dateFilterPanel.add(showAllBtn);

        totalExpenseLabel = new JLabel("Total: ₹0.00");
        totalExpenseLabel.setFont(Theme.FONT_TITLE);
        totalExpenseLabel.setForeground(Theme.DANGER);

        filterStrip.add(dateFilterPanel, BorderLayout.WEST);
        filterStrip.add(totalExpenseLabel, BorderLayout.EAST);

        topContainer.add(filterStrip, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Expense Date", "Title / Payee", "Category", "Amount", "Logged By", "Notes"};
        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        expenseTable = new JTable(tableModel);
        Theme.styleTable(expenseTable);
        expenseTable.getColumnModel().getColumn(0).setMaxWidth(60);

        JScrollPane scroll = new JScrollPane(expenseTable);
        scroll.setBorder(new LineBorder(Theme.BORDER, 1));
        add(scroll, BorderLayout.CENTER);
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        try {
            List<Expense> list = expenseService.getAllExpenses();
            BigDecimal total = BigDecimal.ZERO;
            for (Expense e : list) {
                total = total.add(e.getAmount());
                tableModel.addRow(new Object[]{
                        e.getId(),
                        e.getExpenseDate() != null ? e.getExpenseDate().toString() : "-",
                        e.getTitle(),
                        e.getCategoryName(),
                        ConsoleFormatter.formatCurrency(e.getAmount()),
                        e.getUserName(),
                        e.getNotes() != null ? e.getNotes() : ""
                });
            }
            totalExpenseLabel.setText("All-Time Total: " + ConsoleFormatter.formatCurrency(total));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load expenses: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyDateFilter() {
        try {
            Date start = Date.valueOf(startDateField.getText().trim());
            Date end = Date.valueOf(endDateField.getText().trim());

            tableModel.setRowCount(0);
            List<Expense> list = expenseService.getExpensesByDateRange(start, end);
            BigDecimal total = BigDecimal.ZERO;
            for (Expense e : list) {
                total = total.add(e.getAmount());
                tableModel.addRow(new Object[]{
                        e.getId(),
                        e.getExpenseDate() != null ? e.getExpenseDate().toString() : "-",
                        e.getTitle(),
                        e.getCategoryName(),
                        ConsoleFormatter.formatCurrency(e.getAmount()),
                        e.getUserName(),
                        e.getNotes() != null ? e.getNotes() : ""
                });
            }
            totalExpenseLabel.setText("Range Total: " + ConsoleFormatter.formatCurrency(total));
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.", "Validation Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Filter error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showRecordExpenseDialog() {
        List<ExpenseCategory> categories = expenseService.getAllCategories();
        if (categories.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please create at least one expense category first.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField titleField = Theme.createTextField(20);
        JComboBox<ExpenseCategory> catCombo = new JComboBox<>(categories.toArray(new ExpenseCategory[0]));
        catCombo.setFont(Theme.FONT_BODY);
        catCombo.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ExpenseCategory) {
                    setText(((ExpenseCategory) value).getName());
                }
                return this;
            }
        });

        JTextField amountField = Theme.createTextField(10);
        JTextField dateField = Theme.createTextField(10);
        dateField.setText(LocalDate.now().toString());
        JTextField notesField = Theme.createTextField(20);

        JPanel p = new JPanel(new GridLayout(0, 1, 6, 6));
        p.add(new JLabel("Expense Title / Payee:"));
        p.add(titleField);
        p.add(new JLabel("Category:"));
        p.add(catCombo);
        p.add(new JLabel("Amount:"));
        p.add(amountField);
        p.add(new JLabel("Expense Date (YYYY-MM-DD):"));
        p.add(dateField);
        p.add(new JLabel("Notes / Voucher Reference:"));
        p.add(notesField);

        int res = JOptionPane.showConfirmDialog(this, p, "Record Operating Expense", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            try {
                Expense exp = new Expense();
                exp.setTitle(titleField.getText().trim());
                ExpenseCategory selectedCat = (ExpenseCategory) catCombo.getSelectedItem();
                exp.setCategoryId(selectedCat.getId());
                exp.setAmount(new BigDecimal(amountField.getText().trim()));
                exp.setExpenseDate(Date.valueOf(dateField.getText().trim()));
                exp.setNotes(notesField.getText().trim());
                exp.setUserId(currentUser != null ? currentUser.getId() : 1);

                boolean success = expenseService.recordExpense(exp);
                if (success) {
                    JOptionPane.showMessageDialog(this, "Expense recorded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    refreshTable();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to record expense: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showCategoryManagerDialog() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Expense Categories", true);
        dlg.setLayout(new BorderLayout(10, 10));
        dlg.setSize(480, 400);
        dlg.setLocationRelativeTo(this);

        DefaultTableModel catModel = new DefaultTableModel(new String[]{"ID", "Category Name", "Description"}, 0);
        JTable catTable = new JTable(catModel);
        Theme.styleTable(catTable);

        Runnable loadCats = () -> {
            catModel.setRowCount(0);
            for (ExpenseCategory c : expenseService.getAllCategories()) {
                catModel.addRow(new Object[]{c.getId(), c.getName(), c.getDescription()});
            }
        };
        loadCats.run();

        JPanel topP = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addCatBtn = Theme.createPrimaryButton("+ New Category");
        addCatBtn.addActionListener(e -> {
            JTextField nameField = Theme.createTextField(15);
            JTextField descField = Theme.createTextField(20);
            JPanel p = new JPanel(new GridLayout(0, 1, 4, 4));
            p.add(new JLabel("Category Name:"));
            p.add(nameField);
            p.add(new JLabel("Description:"));
            p.add(descField);

            int r = JOptionPane.showConfirmDialog(dlg, p, "Create Expense Category", JOptionPane.OK_CANCEL_OPTION);
            if (r == JOptionPane.OK_OPTION && !nameField.getText().trim().isEmpty()) {
                expenseService.addCategory(nameField.getText().trim(), descField.getText().trim());
                loadCats.run();
            }
        });
        topP.add(addCatBtn);

        dlg.add(topP, BorderLayout.NORTH);
        dlg.add(new JScrollPane(catTable), BorderLayout.CENTER);
        dlg.setVisible(true);
    }

    private void handleDeleteExpense() {
        int row = expenseTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an expense to delete.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int expId = (int) tableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to permanently delete Expense #" + expId + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                expenseService.deleteExpense(expId);
                JOptionPane.showMessageDialog(this, "Expense deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to delete expense: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

