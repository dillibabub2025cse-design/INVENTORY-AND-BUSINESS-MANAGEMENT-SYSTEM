package ui;

import exception.AppException;
import model.Customer;
import model.Product;
import model.Sale;
import model.SaleItem;
import model.User;
import service.CustomerService;
import service.ProductService;
import service.SalesService;
import util.ConsoleFormatter;
import util.InvoiceGenerator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern Point-of-Sale (POS) and Sales Invoicing panel.
 */
public class SalesPanel extends JPanel {

    private final SalesService salesService;
    private final ProductService productService;
    private final CustomerService customerService;
    private final User currentUser;

    private JTabbedPane tabbedPane;

    // Tab 1: Invoices List
    private JTable salesTable;
    private DefaultTableModel salesModel;

    // Tab 2: POS Checkout Counter
    private JComboBox<String> posCustomerBox;
    private List<Customer> customerList;
    private JComboBox<Product> posProductBox;
    private List<Product> productList;
    private JLabel lblAvailableStock;
    private JTextField posQtyField;
    private JTable cartTable;
    private DefaultTableModel cartModel;
    private List<SaleItem> cartItems = new ArrayList<>();

    private JLabel lblSubtotal;
    private JTextField posDiscountField;
    private JTextField posTaxField;
    private JLabel lblGrandTotal;
    private JComboBox<String> posPaymentBox;

    public SalesPanel(SalesService salesService, ProductService productService, CustomerService customerService, User currentUser) {
        this.salesService = salesService;
        this.productService = productService;
        this.customerService = customerService;
        this.currentUser = currentUser;
        initUI();
        refreshSalesList();
        refreshPosPickers();
    }

    public void refreshTable() {
        refreshSalesList();
        refreshPosPickers();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(Theme.APP_BG);

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(Theme.FONT_HEADER);

        tabbedPane.addTab("🛒 Point of Sale (POS Checkout)", createPosPanel());
        tabbedPane.addTab("📋 Invoices History", createInvoicesListPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createInvoicesListPanel() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(Theme.APP_BG);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel title = new JLabel("Sales Transaction Ledger");
        title.setFont(Theme.FONT_TITLE_LARGE);
        title.setForeground(Theme.TEXT_PRIMARY);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionButtons.setOpaque(false);

        JButton viewReceiptBtn = Theme.createSecondaryButton("📄 View Invoice Receipt");
        viewReceiptBtn.addActionListener(e -> showReceiptDialog());

        JButton cancelBtn = Theme.createDangerButton("❌ Cancel Sale (Restore Stock)");
        cancelBtn.addActionListener(e -> handleCancelSale());

        JButton refreshBtn = Theme.createSecondaryButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> refreshSalesList());

        actionButtons.add(viewReceiptBtn);
        actionButtons.add(cancelBtn);
        actionButtons.add(refreshBtn);

        headerRow.add(title, BorderLayout.WEST);
        headerRow.add(actionButtons, BorderLayout.EAST);
        panel.add(headerRow, BorderLayout.NORTH);

        String[] columns = {"ID", "Invoice Number", "Customer Name", "Total Amount", "Payment", "Status", "Date & Time", "Cashier"};
        salesModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        salesTable = new JTable(salesModel);
        Theme.styleTable(salesTable);

        JScrollPane scroll = new JScrollPane(salesTable);
        scroll.setBorder(new LineBorder(Theme.BORDER, 1));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createPosPanel() {
        JPanel pos = new JPanel(new BorderLayout(14, 14));
        pos.setBackground(Theme.APP_BG);
        pos.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Top Customer & Barcode Search Bar
        JPanel topBar = Theme.createCardPanel();
        topBar.setLayout(new FlowLayout(FlowLayout.LEFT, 14, 6));

        posCustomerBox = new JComboBox<>();
        posCustomerBox.setFont(Theme.FONT_BODY);

        posProductBox = new JComboBox<>();
        posProductBox.setFont(Theme.FONT_BODY);
        posProductBox.setPreferredSize(new Dimension(280, 32));
        posProductBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Product) {
                    setText(((Product) value).getName());
                } else if (value == null) {
                    setText("-- Select Product --");
                }
                return this;
            }
        });

        lblAvailableStock = new JLabel("Stock: 0");
        lblAvailableStock.setFont(Theme.FONT_BODY_BOLD);
        lblAvailableStock.setForeground(Theme.PRIMARY);

        posQtyField = Theme.createTextField(5);
        posQtyField.setText("1");
        posQtyField.addActionListener(e -> handleAddToCart());

        JButton addToCartBtn = Theme.createPrimaryButton("+ Add to Cart");
        addToCartBtn.addActionListener(e -> handleAddToCart());

        posProductBox.addActionListener(e -> updateStockBadge());

        topBar.add(new JLabel("Customer:"));
        topBar.add(posCustomerBox);
        topBar.add(Box.createHorizontalStrut(10));
        topBar.add(new JLabel("Product:"));
        topBar.add(posProductBox);
        topBar.add(lblAvailableStock);
        topBar.add(new JLabel("Qty:"));
        topBar.add(posQtyField);
        topBar.add(addToCartBtn);

        pos.add(topBar, BorderLayout.NORTH);

        // Center Cart Table
        String[] cartCols = {"Product ID", "Item Description", "Qty", "Unit Price (₹)", "Subtotal (₹)"};
        cartModel = new DefaultTableModel(cartCols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        cartTable = new JTable(cartModel);
        Theme.styleTable(cartTable);

        JPanel centerContainer = new JPanel(new BorderLayout(8, 8));
        centerContainer.setOpaque(false);
        centerContainer.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        JButton removeCartBtn = Theme.createDangerButton("Remove Selected Item");
        removeCartBtn.addActionListener(e -> {
            int sel = cartTable.getSelectedRow();
            if (sel >= 0 && sel < cartItems.size()) {
                cartItems.remove(sel);
                cartModel.removeRow(sel);
                recalculateTotals();
            } else {
                JOptionPane.showMessageDialog(this, "Please select an item from the cart table to remove.", "Notice", JOptionPane.WARNING_MESSAGE);
            }
        });
        JPanel cartActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cartActions.setOpaque(false);
        cartActions.add(removeCartBtn);
        centerContainer.add(cartActions, BorderLayout.SOUTH);

        pos.add(centerContainer, BorderLayout.CENTER);

        // Right / Bottom Summary Panel
        JPanel checkoutPanel = Theme.createCardPanel();
        checkoutPanel.setLayout(new GridLayout(6, 2, 10, 8));
        checkoutPanel.setPreferredSize(new Dimension(320, 220));

        lblSubtotal = new JLabel("₹0.00");
        lblSubtotal.setFont(Theme.FONT_HEADER);

        posDiscountField = Theme.createTextField(6);
        posDiscountField.setText("0");
        posDiscountField.addActionListener(e -> recalculateTotals());

        posTaxField = Theme.createTextField(6);
        posTaxField.setText("5");
        posTaxField.addActionListener(e -> recalculateTotals());

        javax.swing.event.DocumentListener totalsDocListener = new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { recalculateTotals(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { recalculateTotals(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { recalculateTotals(); }
        };
        posDiscountField.getDocument().addDocumentListener(totalsDocListener);
        posTaxField.getDocument().addDocumentListener(totalsDocListener);

        posPaymentBox = new JComboBox<>(new String[]{"CASH", "CARD", "ONLINE", "OTHER"});
        posPaymentBox.setFont(Theme.FONT_BODY);

        lblGrandTotal = new JLabel("₹0.00");
        lblGrandTotal.setFont(Theme.FONT_TITLE_LARGE);
        lblGrandTotal.setForeground(Theme.SUCCESS);

        JButton completeSaleBtn = Theme.createPrimaryButton("✓ Complete Sale (Checkout)");
        completeSaleBtn.addActionListener(e -> handleCompleteSale());

        checkoutPanel.add(new JLabel("Subtotal:"));
        checkoutPanel.add(lblSubtotal);
        checkoutPanel.add(new JLabel("Discount (%):"));
        checkoutPanel.add(posDiscountField);
        checkoutPanel.add(new JLabel("Tax / VAT (%):"));
        checkoutPanel.add(posTaxField);
        checkoutPanel.add(new JLabel("Payment Method:"));
        checkoutPanel.add(posPaymentBox);
        checkoutPanel.add(new JLabel("GRAND TOTAL:"));
        checkoutPanel.add(lblGrandTotal);
        checkoutPanel.add(new JLabel(""));
        checkoutPanel.add(completeSaleBtn);

        pos.add(checkoutPanel, BorderLayout.EAST);

        refreshPosPickers();
        return pos;
    }

    private void updateStockBadge() {
        Product p = (Product) posProductBox.getSelectedItem();
        if (p != null) {
            lblAvailableStock.setText("Stock: " + p.getCurrentStock());
            if (p.getCurrentStock() <= 0) {
                lblAvailableStock.setForeground(Theme.DANGER);
            } else if (p.getCurrentStock() <= p.getMinStockLevel()) {
                lblAvailableStock.setForeground(Theme.WARNING);
            } else {
                lblAvailableStock.setForeground(Theme.SUCCESS);
            }
        } else {
            lblAvailableStock.setText("Stock: 0");
            lblAvailableStock.setForeground(Theme.TEXT_MUTED);
        }
    }

    public void refreshPosPickers() {
        posCustomerBox.removeAllItems();
        posCustomerBox.addItem("Walk-in Customer");
        customerList = customerService.getAllCustomers();
        for (Customer c : customerList) {
            posCustomerBox.addItem(c.getName() + " [" + c.getPhone() + "]");
        }

        posProductBox.removeAllItems();
        productList = productService.getAllProducts();
        for (Product p : productList) {
            posProductBox.addItem(p);
        }
        updateStockBadge();
    }

    private void handleAddToCart() {
        Product p = (Product) posProductBox.getSelectedItem();
        if (p == null) {
            JOptionPane.showMessageDialog(this, "Please select a product first.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String qtyStr = posQtyField.getText().trim();
            if (qtyStr.isEmpty()) {
                throw new AppException("Please enter a valid quantity.");
            }
            int qty;
            try {
                qty = Integer.parseInt(qtyStr);
            } catch (NumberFormatException nfe) {
                throw new AppException("Quantity must be a valid whole number.");
            }
            if (qty <= 0) {
                throw new AppException("Quantity must be at least 1.");
            }

            if (p.getCurrentStock() <= 0) {
                throw new AppException("'" + p.getName() + "' is currently OUT OF STOCK.");
            }

            // Calculate currently carted quantity for this product
            int existingInCart = 0;
            int existingIndex = -1;
            for (int i = 0; i < cartItems.size(); i++) {
                SaleItem it = cartItems.get(i);
                if (it.getProductId() == p.getId()) {
                    existingInCart += it.getQuantity();
                    existingIndex = i;
                }
            }

            if (existingInCart + qty > p.getCurrentStock()) {
                throw new AppException("Cannot add " + qty + " units! Current stock is " + p.getCurrentStock()
                        + (existingInCart > 0 ? " (" + existingInCart + " already in cart)." : "."));
            }

            if (existingIndex >= 0) {
                // Product already exists in cart -> update quantity and subtotal
                SaleItem existingItem = cartItems.get(existingIndex);
                int newQty = existingItem.getQuantity() + qty;
                existingItem.setQuantity(newQty);

                cartModel.setValueAt(newQty, existingIndex, 2);
                cartModel.setValueAt(ConsoleFormatter.formatCurrency(existingItem.getSubtotal()), existingIndex, 4);
            } else {
                // New line item in cart
                SaleItem si = new SaleItem(p.getId(), p.getName(), qty, p.getSellingPrice(), p.getPurchasePrice());
                cartItems.add(si);
                cartModel.addRow(new Object[]{
                        p.getId(),
                        p.getName(),
                        qty,
                        ConsoleFormatter.formatCurrency(p.getSellingPrice()),
                        ConsoleFormatter.formatCurrency(si.getSubtotal())
                });
            }

            recalculateTotals();
            posQtyField.setText("1");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void recalculateTotals() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (SaleItem it : cartItems) {
            subtotal = subtotal.add(it.getSubtotal());
        }

        lblSubtotal.setText(ConsoleFormatter.formatCurrency(subtotal));

        BigDecimal discPct = BigDecimal.ZERO;
        try {
            String d = posDiscountField.getText().trim();
            if (!d.isEmpty()) {
                discPct = new BigDecimal(d);
                if (discPct.compareTo(BigDecimal.ZERO) < 0) discPct = BigDecimal.ZERO;
                if (discPct.compareTo(BigDecimal.valueOf(100)) > 0) discPct = BigDecimal.valueOf(100);
            }
        } catch (Exception ignored) {}

        BigDecimal taxPct = BigDecimal.ZERO;
        try {
            String t = posTaxField.getText().trim();
            if (!t.isEmpty()) {
                taxPct = new BigDecimal(t);
                if (taxPct.compareTo(BigDecimal.ZERO) < 0) taxPct = BigDecimal.ZERO;
            }
        } catch (Exception ignored) {}

        BigDecimal discountAmt = subtotal.multiply(discPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal discounted = subtotal.subtract(discountAmt);
        if (discounted.compareTo(BigDecimal.ZERO) < 0) {
            discounted = BigDecimal.ZERO;
        }

        BigDecimal taxAmt = discounted.multiply(taxPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = discounted.add(taxAmt);

        lblGrandTotal.setText(ConsoleFormatter.formatCurrency(grandTotal));
    }

    private void handleCompleteSale() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Shopping cart is empty. Add products before completing checkout.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Integer custId = null;
            int selCustIdx = posCustomerBox.getSelectedIndex();
            if (selCustIdx > 0 && customerList != null && selCustIdx - 1 < customerList.size()) {
                custId = customerList.get(selCustIdx - 1).getId();
            }

            BigDecimal discPct = BigDecimal.ZERO;
            String discStr = posDiscountField.getText().trim();
            if (!discStr.isEmpty()) {
                try {
                    discPct = new BigDecimal(discStr);
                    if (discPct.compareTo(BigDecimal.ZERO) < 0 || discPct.compareTo(BigDecimal.valueOf(100)) > 0) {
                        throw new AppException("Discount must be between 0% and 100%.");
                    }
                } catch (NumberFormatException nfe) {
                    throw new AppException("Discount must be a valid number.");
                }
            }

            BigDecimal taxPct = BigDecimal.ZERO;
            String taxStr = posTaxField.getText().trim();
            if (!taxStr.isEmpty()) {
                try {
                    taxPct = new BigDecimal(taxStr);
                    if (taxPct.compareTo(BigDecimal.ZERO) < 0) {
                        throw new AppException("Tax percent cannot be negative.");
                    }
                } catch (NumberFormatException nfe) {
                    throw new AppException("Tax percent must be a valid number.");
                }
            }

            String payment = (String) posPaymentBox.getSelectedItem();

            Sale sale = salesService.createSale(custId, currentUser.getId(), cartItems, discPct, taxPct, payment);

            JOptionPane.showMessageDialog(this,
                    "Sale completed successfully!\nInvoice Number: " + sale.getInvoiceNumber() +
                    "\nTotal: " + ConsoleFormatter.formatCurrency(sale.getTotalAmount()),
                    "Success", JOptionPane.INFORMATION_MESSAGE);

            // Clear cart
            cartItems.clear();
            cartModel.setRowCount(0);
            recalculateTotals();

            // Refresh data
            refreshSalesList();
            refreshPosPickers();

            // Open receipt view
            showSpecificReceiptDialog(sale);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Checkout failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refreshSalesList() {
        salesModel.setRowCount(0);
        List<Sale> list = salesService.getAllSales();
        for (Sale s : list) {
            salesModel.addRow(new Object[]{
                    s.getId(),
                    s.getInvoiceNumber(),
                    s.getCustomerName(),
                    ConsoleFormatter.formatCurrency(s.getTotalAmount()),
                    s.getPaymentMethod(),
                    s.getStatus(),
                    ConsoleFormatter.formatDateTime(s.getSaleDate()),
                    s.getUserName()
            });
        }
    }

    private void showReceiptDialog() {
        int row = salesTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an invoice from the table.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) salesTable.getValueAt(row, 0);
        Sale s = salesService.getSaleById(id);
        showSpecificReceiptDialog(s);
    }

    private void showSpecificReceiptDialog(Sale s) {
        String receipt = InvoiceGenerator.generateSalesInvoice(s, false);

        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Sales Invoice: " + s.getInvoiceNumber(), true);
        dlg.setSize(620, 520);
        dlg.setLocationRelativeTo(this);

        JTextArea ta = new JTextArea(receipt);
        ta.setFont(new Font("Consolas", Font.PLAIN, 12));
        ta.setEditable(false);
        ta.setCaretPosition(0);

        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        p.add(new JScrollPane(ta), BorderLayout.CENTER);

        JButton close = Theme.createSecondaryButton("Close");
        close.addActionListener(e -> dlg.dispose());
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bp.add(close);
        p.add(bp, BorderLayout.SOUTH);

        dlg.add(p);
        dlg.setVisible(true);
    }

    private void handleCancelSale() {
        int row = salesTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a sale invoice to cancel.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) salesTable.getValueAt(row, 0);
        String invNum = (String) salesTable.getValueAt(row, 1);
        String status = (String) salesTable.getValueAt(row, 5);

        if ("CANCELLED".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "Invoice #" + invNum + " is already cancelled.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String reason = JOptionPane.showInputDialog(this, "Enter cancellation reason (Sold stock will be returned to inventory):", "Cancel Sale", JOptionPane.QUESTION_MESSAGE);
        if (reason != null && !reason.trim().isEmpty()) {
            try {
                salesService.cancelSale(id, currentUser.getId(), reason.trim());
                JOptionPane.showMessageDialog(this, "Sale cancelled and stock restored to inventory successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshSalesList();
                refreshPosPickers();
            } catch (AppException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot Cancel", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

