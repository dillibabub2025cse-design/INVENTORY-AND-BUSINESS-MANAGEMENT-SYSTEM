package ui;

import exception.AppException;
import model.Product;
import model.Purchase;
import model.PurchaseItem;
import model.Supplier;
import model.User;
import service.ProductService;
import service.PurchaseService;
import service.SupplierService;
import util.ConsoleFormatter;
import util.InvoiceGenerator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern Purchase Order Procurement management panel.
 */
public class PurchasePanel extends JPanel {

    private final PurchaseService purchaseService;
    private final SupplierService supplierService;
    private final ProductService productService;
    private final User currentUser;

    private JTable purchaseTable;
    private DefaultTableModel tableModel;

    public PurchasePanel(PurchaseService purchaseService, SupplierService supplierService, ProductService productService, User currentUser) {
        this.purchaseService = purchaseService;
        this.supplierService = supplierService;
        this.productService = productService;
        this.currentUser = currentUser;
        initUI();
        refreshTable();
    }

    private void initUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(Theme.APP_BG);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel title = new JLabel("Purchase Orders & Stock Replenishment");
        title.setFont(Theme.FONT_TITLE_LARGE);
        title.setForeground(Theme.TEXT_PRIMARY);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionButtons.setOpaque(false);

        JButton createPoBtn = Theme.createPrimaryButton("+ Create Purchase Order");
        createPoBtn.addActionListener(e -> showCreatePoDialog());

        JButton viewReceiptBtn = Theme.createSecondaryButton("📄 View Receipt");
        viewReceiptBtn.addActionListener(e -> showReceiptDialog());

        JButton cancelPoBtn = Theme.createDangerButton("❌ Cancel PO (Reverse Stock)");
        cancelPoBtn.addActionListener(e -> handleCancelPurchase());

        JButton refreshBtn = Theme.createSecondaryButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> refreshTable());

        actionButtons.add(createPoBtn);
        actionButtons.add(viewReceiptBtn);
        actionButtons.add(cancelPoBtn);
        actionButtons.add(refreshBtn);

        headerRow.add(title, BorderLayout.WEST);
        headerRow.add(actionButtons, BorderLayout.EAST);
        add(headerRow, BorderLayout.NORTH);

        String[] columns = {"ID", "PO Number", "Supplier Name", "Total Amount", "Status", "Date & Time", "Created By"};
        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        purchaseTable = new JTable(tableModel);
        Theme.styleTable(purchaseTable);

        JScrollPane scroll = new JScrollPane(purchaseTable);
        scroll.setBorder(new LineBorder(Theme.BORDER, 1));
        add(scroll, BorderLayout.CENTER);
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        List<Purchase> list = purchaseService.getAllPurchases();
        for (Purchase p : list) {
            tableModel.addRow(new Object[]{
                    p.getId(),
                    p.getPurchaseNumber(),
                    p.getSupplierName(),
                    ConsoleFormatter.formatCurrency(p.getTotalAmount()),
                    p.getStatus(),
                    ConsoleFormatter.formatDateTime(p.getPurchaseDate()),
                    p.getUserName()
            });
        }
    }

    private void showCreatePoDialog() {
        List<Supplier> suppliers = supplierService.getAllSuppliers();
        List<Product> products = productService.getAllProducts();

        if (suppliers.isEmpty() || products.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please ensure you have registered suppliers and products first.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "New Purchase Order", true);
        dlg.setSize(800, 600);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(10, 10));

        JPanel topP = new JPanel(new GridLayout(2, 2, 10, 10));
        topP.setBorder(new EmptyBorder(12, 16, 12, 16));
        JComboBox<Supplier> supBox = new JComboBox<>(suppliers.toArray(new Supplier[0]));
        JTextField notesField = Theme.createTextField(20);

        topP.add(new JLabel("Select Supplier:"));
        topP.add(supBox);
        topP.add(new JLabel("Order Notes:"));
        topP.add(notesField);
        dlg.add(topP, BorderLayout.NORTH);

        // Center Cart Table
        String[] itemCols = {"Product ID", "Product Name", "Quantity", "Unit Cost (₹)", "Subtotal (₹)"};
        DefaultTableModel itemsModel = new DefaultTableModel(itemCols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable itemsTable = new JTable(itemsModel);
        Theme.styleTable(itemsTable);

        List<PurchaseItem> pendingItems = new ArrayList<>();

        JPanel centerP = new JPanel(new BorderLayout(8, 8));
        centerP.setBorder(new EmptyBorder(0, 16, 0, 16));

        // Add line row
        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JComboBox<Product> prodBox = new JComboBox<>(products.toArray(new Product[0]));
        JTextField qtyField = Theme.createTextField(5);
        qtyField.setText("10");
        JTextField costField = Theme.createTextField(8);
        if (!products.isEmpty()) {
            costField.setText(products.get(0).getPurchasePrice().toString());
        }

        prodBox.addActionListener(e -> {
            Product sel = (Product) prodBox.getSelectedItem();
            if (sel != null) costField.setText(sel.getPurchasePrice().toString());
        });

        JLabel totalLbl = new JLabel("Total: ₹0.00");
        totalLbl.setFont(Theme.FONT_TITLE);
        totalLbl.setForeground(Theme.PRIMARY);

        JButton addItemBtn = Theme.createPrimaryButton("+ Add Item");
        addItemBtn.addActionListener(e -> {
            try {
                Product sel = (Product) prodBox.getSelectedItem();
                int q = Integer.parseInt(qtyField.getText().trim());
                BigDecimal c = new BigDecimal(costField.getText().trim());
                if (q <= 0) throw new AppException("Quantity must be positive.");

                PurchaseItem pi = new PurchaseItem(sel.getId(), sel.getName(), q, c);
                pendingItems.add(pi);
                itemsModel.addRow(new Object[]{sel.getId(), sel.getName(), q, ConsoleFormatter.formatCurrency(c), ConsoleFormatter.formatCurrency(pi.getSubtotal())});

                BigDecimal sum = BigDecimal.ZERO;
                for (PurchaseItem it : pendingItems) sum = sum.add(it.getSubtotal());
                totalLbl.setText("Total: " + ConsoleFormatter.formatCurrency(sum));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Error adding item: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton removeBtn = Theme.createDangerButton("Remove Item");
        removeBtn.addActionListener(e -> {
            int sel = itemsTable.getSelectedRow();
            if (sel >= 0) {
                pendingItems.remove(sel);
                itemsModel.removeRow(sel);
                BigDecimal sum = BigDecimal.ZERO;
                for (PurchaseItem it : pendingItems) sum = sum.add(it.getSubtotal());
                totalLbl.setText("Total: " + ConsoleFormatter.formatCurrency(sum));
            }
        });

        addRow.add(new JLabel("Product:"));
        addRow.add(prodBox);
        addRow.add(new JLabel("Qty:"));
        addRow.add(qtyField);
        addRow.add(new JLabel("Unit Cost:"));
        addRow.add(costField);
        addRow.add(addItemBtn);
        addRow.add(removeBtn);

        centerP.add(addRow, BorderLayout.NORTH);
        centerP.add(new JScrollPane(itemsTable), BorderLayout.CENTER);
        dlg.add(centerP, BorderLayout.CENTER);

        // Bottom confirm panel
        JPanel btmP = new JPanel(new BorderLayout());
        btmP.setBorder(new EmptyBorder(12, 16, 12, 16));
        btmP.add(totalLbl, BorderLayout.WEST);

        JButton confirmBtn = Theme.createPrimaryButton("Confirm & Submit Purchase Order");
        confirmBtn.addActionListener(e -> {
            if (pendingItems.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Please add at least one product item.", "Notice", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                Supplier s = (Supplier) supBox.getSelectedItem();
                Purchase po = purchaseService.createPurchaseOrder(s.getId(), currentUser.getId(), pendingItems, notesField.getText().trim());
                JOptionPane.showMessageDialog(dlg, "Purchase Order created successfully!\nPO Number: " + po.getPurchaseNumber() + "\nInventory stock replenished.", "Success", JOptionPane.INFORMATION_MESSAGE);
                dlg.dispose();
                refreshTable();
            } catch (AppException ex) {
                JOptionPane.showMessageDialog(dlg, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btmP.add(confirmBtn, BorderLayout.EAST);
        dlg.add(btmP, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    private void showReceiptDialog() {
        int row = purchaseTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a purchase order from the table.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) purchaseTable.getValueAt(row, 0);
        Purchase p = purchaseService.getPurchaseById(id);
        String receiptText = InvoiceGenerator.generatePurchaseOrder(p, false);

        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Purchase Order: " + p.getPurchaseNumber(), true);
        dlg.setSize(620, 500);
        dlg.setLocationRelativeTo(this);

        JTextArea ta = new JTextArea(receiptText);
        ta.setFont(new Font("Consolas", Font.PLAIN, 12));
        ta.setEditable(false);
        ta.setCaretPosition(0);

        JPanel pnl = new JPanel(new BorderLayout(8, 8));
        pnl.setBorder(new EmptyBorder(10, 10, 10, 10));
        pnl.add(new JScrollPane(ta), BorderLayout.CENTER);

        JButton close = Theme.createSecondaryButton("Close");
        close.addActionListener(e -> dlg.dispose());
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bp.add(close);
        pnl.add(bp, BorderLayout.SOUTH);

        dlg.add(pnl);
        dlg.setVisible(true);
    }

    private void handleCancelPurchase() {
        int row = purchaseTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a purchase order to cancel.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) purchaseTable.getValueAt(row, 0);
        String poNum = (String) purchaseTable.getValueAt(row, 1);
        String status = (String) purchaseTable.getValueAt(row, 4);

        if ("CANCELLED".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "Purchase order #" + poNum + " is already cancelled.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String reason = JOptionPane.showInputDialog(this, "Enter reason for cancellation (Stock will be reversed):", "Cancellation Reason", JOptionPane.QUESTION_MESSAGE);
        if (reason != null && !reason.trim().isEmpty()) {
            try {
                purchaseService.cancelPurchaseOrder(id, currentUser.getId(), reason.trim());
                JOptionPane.showMessageDialog(this, "Purchase order cancelled and stock reversed successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshTable();
            } catch (AppException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot Cancel", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

