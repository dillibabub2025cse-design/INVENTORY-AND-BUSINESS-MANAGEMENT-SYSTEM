package ui;

import exception.AppException;
import model.Supplier;
import service.SupplierService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Modern Supplier and Vendor management panel.
 */
public class SupplierPanel extends JPanel {

    private final SupplierService supplierService;
    private JTable supplierTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    public SupplierPanel(SupplierService supplierService) {
        this.supplierService = supplierService;
        initUI();
        refreshTable();
    }

    private void initUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(Theme.APP_BG);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel title = new JLabel("Supplier & Vendor Directory");
        title.setFont(Theme.FONT_TITLE_LARGE);
        title.setForeground(Theme.TEXT_PRIMARY);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionButtons.setOpaque(false);

        JButton addBtn = Theme.createPrimaryButton("+ Add Supplier");
        addBtn.addActionListener(e -> showAddSupplierDialog());

        JButton editBtn = Theme.createSecondaryButton("✏ Edit");
        editBtn.addActionListener(e -> showEditSupplierDialog());

        JButton deleteBtn = Theme.createDangerButton("🗑 Delete");
        deleteBtn.addActionListener(e -> handleDeleteSupplier());

        JButton refreshBtn = Theme.createSecondaryButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> refreshTable());

        actionButtons.add(addBtn);
        actionButtons.add(editBtn);
        actionButtons.add(deleteBtn);
        actionButtons.add(refreshBtn);

        headerRow.add(title, BorderLayout.WEST);
        headerRow.add(actionButtons, BorderLayout.EAST);

        // Search Bar
        JPanel searchBar = Theme.createCardPanel();
        searchBar.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 4));

        searchField = Theme.createTextField(22);
        searchField.putClientProperty("JTextField.placeholderText", "Search company, contact person, phone...");
        searchField.addActionListener(e -> refreshTable());

        JButton searchBtn = Theme.createSecondaryButton("Search");
        searchBtn.addActionListener(e -> refreshTable());

        searchBar.add(new JLabel("Search Vendors:"));
        searchBar.add(searchField);
        searchBar.add(searchBtn);

        JPanel topBox = new JPanel();
        topBox.setLayout(new BoxLayout(topBox, BoxLayout.Y_AXIS));
        topBox.setOpaque(false);
        topBox.add(headerRow);
        topBox.add(Box.createVerticalStrut(12));
        topBox.add(searchBar);

        add(topBox, BorderLayout.NORTH);

        String[] columns = {"ID", "Company Name", "Contact Person", "Email", "Phone", "Address", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        supplierTable = new JTable(tableModel);
        Theme.styleTable(supplierTable);

        JScrollPane scroll = new JScrollPane(supplierTable);
        scroll.setBorder(new LineBorder(Theme.BORDER, 1));
        add(scroll, BorderLayout.CENTER);
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        String query = searchField.getText().trim();
        List<Supplier> list = query.isEmpty() ? supplierService.getAllSuppliers() : supplierService.searchSuppliers(query);

        for (Supplier s : list) {
            tableModel.addRow(new Object[]{
                    s.getId(),
                    s.getCompanyName(),
                    s.getContactName(),
                    s.getEmail(),
                    s.getPhone(),
                    s.getAddress(),
                    s.isActive() ? "ACTIVE" : "INACTIVE"
            });
        }
    }

    private void showAddSupplierDialog() {
        JTextField compField = Theme.createTextField(20);
        JTextField contactField = Theme.createTextField(20);
        JTextField emailField = Theme.createTextField(20);
        JTextField phoneField = Theme.createTextField(20);
        JTextField addrField = Theme.createTextField(20);

        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(new JLabel("Company Name:")); p.add(compField);
        p.add(new JLabel("Contact Person:")); p.add(contactField);
        p.add(new JLabel("Email Address:")); p.add(emailField);
        p.add(new JLabel("Phone Number:")); p.add(phoneField);
        p.add(new JLabel("Address:")); p.add(addrField);

        int res = JOptionPane.showConfirmDialog(this, p, "Register New Supplier", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            try {
                Supplier s = new Supplier(0, compField.getText().trim(), contactField.getText().trim(),
                        emailField.getText().trim(), phoneField.getText().trim(), addrField.getText().trim(), true);
                supplierService.addSupplier(s);
                JOptionPane.showMessageDialog(this, "Supplier added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshTable();
            } catch (AppException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditSupplierDialog() {
        int row = supplierTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a supplier from the table.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) supplierTable.getValueAt(row, 0);
        Supplier s = supplierService.getSupplierById(id);

        JTextField compField = Theme.createTextField(20); compField.setText(s.getCompanyName());
        JTextField contactField = Theme.createTextField(20); contactField.setText(s.getContactName());
        JTextField emailField = Theme.createTextField(20); emailField.setText(s.getEmail());
        JTextField phoneField = Theme.createTextField(20); phoneField.setText(s.getPhone());
        JTextField addrField = Theme.createTextField(20); addrField.setText(s.getAddress());
        JCheckBox activeBox = new JCheckBox("Active", s.isActive());

        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(new JLabel("Company Name:")); p.add(compField);
        p.add(new JLabel("Contact Person:")); p.add(contactField);
        p.add(new JLabel("Email Address:")); p.add(emailField);
        p.add(new JLabel("Phone Number:")); p.add(phoneField);
        p.add(new JLabel("Address:")); p.add(addrField);
        p.add(new JLabel("Status:")); p.add(activeBox);

        int res = JOptionPane.showConfirmDialog(this, p, "Edit Supplier #" + id, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            try {
                s.setCompanyName(compField.getText().trim());
                s.setContactName(contactField.getText().trim());
                s.setEmail(emailField.getText().trim());
                s.setPhone(phoneField.getText().trim());
                s.setAddress(addrField.getText().trim());
                s.setActive(activeBox.isSelected());

                supplierService.updateSupplier(s);
                JOptionPane.showMessageDialog(this, "Supplier updated.", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshTable();
            } catch (AppException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleDeleteSupplier() {
        int row = supplierTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a supplier to delete.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) supplierTable.getValueAt(row, 0);
        String name = (String) supplierTable.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this, "Delete Supplier '" + name + "'?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                supplierService.deleteSupplier(id);
                JOptionPane.showMessageDialog(this, "Supplier deleted.", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshTable();
            } catch (AppException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot Delete", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

