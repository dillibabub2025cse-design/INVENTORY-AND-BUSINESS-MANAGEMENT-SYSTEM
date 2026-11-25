package ui;

import exception.AppException;
import model.Customer;
import model.Sale;
import service.CustomerService;
import util.ConsoleFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Modern Customer directory and purchase history panel.
 */
public class CustomerPanel extends JPanel {

    private final CustomerService customerService;
    private JTable customerTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    public CustomerPanel(CustomerService customerService) {
        this.customerService = customerService;
        initUI();
        refreshTable();
    }

    private void initUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(Theme.APP_BG);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel title = new JLabel("Customer Records & Purchase History");
        title.setFont(Theme.FONT_TITLE_LARGE);
        title.setForeground(Theme.TEXT_PRIMARY);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionButtons.setOpaque(false);

        JButton addBtn = Theme.createPrimaryButton("+ Add Customer");
        addBtn.addActionListener(e -> showAddCustomerDialog());

        JButton editBtn = Theme.createSecondaryButton("✏ Edit");
        editBtn.addActionListener(e -> showEditCustomerDialog());

        JButton historyBtn = Theme.createSecondaryButton("📜 Purchase History");
        historyBtn.addActionListener(e -> showPurchaseHistoryDialog());

        JButton deleteBtn = Theme.createDangerButton("🗑 Delete");
        deleteBtn.addActionListener(e -> handleDeleteCustomer());

        JButton refreshBtn = Theme.createSecondaryButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> refreshTable());

        actionButtons.add(addBtn);
        actionButtons.add(editBtn);
        actionButtons.add(historyBtn);
        actionButtons.add(deleteBtn);
        actionButtons.add(refreshBtn);

        headerRow.add(title, BorderLayout.WEST);
        headerRow.add(actionButtons, BorderLayout.EAST);

        // Search Bar
        JPanel searchBar = Theme.createCardPanel();
        searchBar.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 4));

        searchField = Theme.createTextField(22);
        searchField.putClientProperty("JTextField.placeholderText", "Search name, phone, email...");
        searchField.addActionListener(e -> refreshTable());

        JButton searchBtn = Theme.createSecondaryButton("Search");
        searchBtn.addActionListener(e -> refreshTable());

        searchBar.add(new JLabel("Search Customers:"));
        searchBar.add(searchField);
        searchBar.add(searchBtn);

        JPanel topBox = new JPanel();
        topBox.setLayout(new BoxLayout(topBox, BoxLayout.Y_AXIS));
        topBox.setOpaque(false);
        topBox.add(headerRow);
        topBox.add(Box.createVerticalStrut(12));
        topBox.add(searchBar);

        add(topBox, BorderLayout.NORTH);

        String[] columns = {"ID", "Customer Name", "Email", "Phone", "Address"};
        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        customerTable = new JTable(tableModel);
        Theme.styleTable(customerTable);

        JScrollPane scroll = new JScrollPane(customerTable);
        scroll.setBorder(new LineBorder(Theme.BORDER, 1));
        add(scroll, BorderLayout.CENTER);
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        String query = searchField.getText().trim();
        List<Customer> list = query.isEmpty() ? customerService.getAllCustomers() : customerService.searchCustomers(query);

        for (Customer c : list) {
            tableModel.addRow(new Object[]{
                    c.getId(),
                    c.getName(),
                    c.getEmail() != null ? c.getEmail() : "N/A",
                    c.getPhone(),
                    c.getAddress() != null ? c.getAddress() : "N/A"
            });
        }
    }

    private void showAddCustomerDialog() {
        JTextField nameField = Theme.createTextField(20);
        JTextField emailField = Theme.createTextField(20);
        JTextField phoneField = Theme.createTextField(20);
        JTextField addrField = Theme.createTextField(20);

        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(new JLabel("Customer Name:")); p.add(nameField);
        p.add(new JLabel("Email Address:")); p.add(emailField);
        p.add(new JLabel("Phone Number:")); p.add(phoneField);
        p.add(new JLabel("Address:")); p.add(addrField);

        int res = JOptionPane.showConfirmDialog(this, p, "Register New Customer", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            try {
                Customer c = new Customer(0, nameField.getText().trim(), emailField.getText().trim(),
                        phoneField.getText().trim(), addrField.getText().trim());
                customerService.addCustomer(c);
                JOptionPane.showMessageDialog(this, "Customer registered successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshTable();
            } catch (AppException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditCustomerDialog() {
        int row = customerTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a customer to edit.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) customerTable.getValueAt(row, 0);
        Customer c = customerService.getCustomerById(id);

        JTextField nameField = Theme.createTextField(20); nameField.setText(c.getName());
        JTextField emailField = Theme.createTextField(20); emailField.setText(c.getEmail());
        JTextField phoneField = Theme.createTextField(20); phoneField.setText(c.getPhone());
        JTextField addrField = Theme.createTextField(20); addrField.setText(c.getAddress());

        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(new JLabel("Customer Name:")); p.add(nameField);
        p.add(new JLabel("Email Address:")); p.add(emailField);
        p.add(new JLabel("Phone Number:")); p.add(phoneField);
        p.add(new JLabel("Address:")); p.add(addrField);

        int res = JOptionPane.showConfirmDialog(this, p, "Edit Customer #" + id, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            try {
                c.setName(nameField.getText().trim());
                c.setEmail(emailField.getText().trim());
                c.setPhone(phoneField.getText().trim());
                c.setAddress(addrField.getText().trim());

                customerService.updateCustomer(c);
                JOptionPane.showMessageDialog(this, "Customer updated.", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshTable();
            } catch (AppException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showPurchaseHistoryDialog() {
        int row = customerTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a customer from the table.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) customerTable.getValueAt(row, 0);
        Customer c = customerService.getCustomerById(id);
        List<Sale> sales = customerService.getCustomerSalesHistory(id);

        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Purchase History: " + c.getName(), true);
        dlg.setSize(750, 420);
        dlg.setLocationRelativeTo(this);

        String[] cols = {"Invoice #", "Date", "Total Amount", "Payment", "Status"};
        DefaultTableModel mod = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int co) { return false; }
        };
        for (Sale s : sales) {
            mod.addRow(new Object[]{
                    s.getInvoiceNumber(),
                    ConsoleFormatter.formatDateTime(s.getSaleDate()),
                    ConsoleFormatter.formatCurrency(s.getTotalAmount()),
                    s.getPaymentMethod(),
                    s.getStatus()
            });
        }

        JTable tbl = new JTable(mod);
        Theme.styleTable(tbl);

        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(new EmptyBorder(14, 14, 14, 14));
        p.add(new JLabel("Sales orders placed by " + c.getName() + " (" + sales.size() + " orders):"), BorderLayout.NORTH);
        p.add(new JScrollPane(tbl), BorderLayout.CENTER);

        JButton closeBtn = Theme.createSecondaryButton("Close");
        closeBtn.addActionListener(e -> dlg.dispose());
        JPanel btm = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btm.add(closeBtn);
        p.add(btm, BorderLayout.SOUTH);

        dlg.add(p);
        dlg.setVisible(true);
    }

    private void handleDeleteCustomer() {
        int row = customerTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a customer to delete.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) customerTable.getValueAt(row, 0);
        String name = (String) customerTable.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this, "Delete Customer '" + name + "'?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                customerService.deleteCustomer(id);
                JOptionPane.showMessageDialog(this, "Customer deleted.", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshTable();
            } catch (AppException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot Delete", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

