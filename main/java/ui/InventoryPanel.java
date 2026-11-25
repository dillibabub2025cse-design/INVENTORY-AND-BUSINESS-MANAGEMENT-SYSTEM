package ui;

import exception.AppException;
import model.Product;
import model.StockMovement;
import model.StockMovementType;
import model.User;
import service.InventoryService;
import service.ProductService;
import util.ConsoleFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Modern Inventory Valuation and Stock Movement Audit panel.
 */
public class InventoryPanel extends JPanel {

    private final InventoryService inventoryService;
    private final ProductService productService;
    private final User currentUser;

    private JLabel totalItemsVal;
    private JLabel totalUnitsVal;
    private JLabel costValuationVal;
    private JLabel retailValuationVal;
    private JLabel potentialProfitVal;

    private JTable stockTable;
    private DefaultTableModel stockTableModel;

    private JTable movementTable;
    private DefaultTableModel movementTableModel;

    private JComboBox<Integer> limitCombo;

    public InventoryPanel(InventoryService inventoryService, ProductService productService, User currentUser) {
        this.inventoryService = inventoryService;
        this.productService = productService;
        this.currentUser = currentUser;
        initUI();
        refreshAll();
    }

    private void initUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(Theme.APP_BG);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header and KPIs
        JPanel topContainer = new JPanel(new BorderLayout(12, 12));
        topContainer.setOpaque(false);

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel title = new JLabel("Inventory & Stock Movement Audits");
        title.setFont(Theme.FONT_TITLE_LARGE);
        title.setForeground(Theme.TEXT_PRIMARY);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionButtons.setOpaque(false);

        JButton adjustBtn = Theme.createPrimaryButton("⚡ Manual Stock Adjustment");
        adjustBtn.addActionListener(e -> showStockAdjustmentDialog());

        JButton refreshBtn = Theme.createSecondaryButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> refreshAll());

        actionButtons.add(adjustBtn);
        actionButtons.add(refreshBtn);

        headerRow.add(title, BorderLayout.WEST);
        headerRow.add(actionButtons, BorderLayout.EAST);
        topContainer.add(headerRow, BorderLayout.NORTH);

        // Valuation KPI Cards
        JPanel kpiGrid = new JPanel(new GridLayout(1, 5, 12, 12));
        kpiGrid.setOpaque(false);

        totalItemsVal = new JLabel("0", SwingConstants.CENTER);
        totalUnitsVal = new JLabel("0", SwingConstants.CENTER);
        costValuationVal = new JLabel("₹0.00", SwingConstants.CENTER);
        retailValuationVal = new JLabel("₹0.00", SwingConstants.CENTER);
        potentialProfitVal = new JLabel("₹0.00", SwingConstants.CENTER);

        kpiGrid.add(createKpiCard("Catalog Items", totalItemsVal, Theme.PRIMARY));
        kpiGrid.add(createKpiCard("Total Units on Hand", totalUnitsVal, Theme.SECONDARY));
        kpiGrid.add(createKpiCard("Cost Valuation", costValuationVal, Theme.WARNING));
        kpiGrid.add(createKpiCard("Retail Valuation", retailValuationVal, Theme.INFO));
        kpiGrid.add(createKpiCard("Potential Profit", potentialProfitVal, Theme.SUCCESS));

        topContainer.add(kpiGrid, BorderLayout.CENTER);
        add(topContainer, BorderLayout.NORTH);

        // Tabbed view: Current Stock vs Stock Movements
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.FONT_BODY_BOLD);
        tabs.setBackground(Theme.SURFACE);

        // Tab 1: Current Stock Snapshot
        JPanel stockTab = new JPanel(new BorderLayout(10, 10));
        stockTab.setBackground(Theme.SURFACE);
        stockTab.setBorder(new EmptyBorder(12, 12, 12, 12));

        String[] stockCols = {"ID", "Barcode", "Product Name", "Category", "On Hand", "Min Level", "Cost Price", "Selling Price", "Cost Value", "Retail Value", "Health Status"};
        stockTableModel = new DefaultTableModel(stockCols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        stockTable = new JTable(stockTableModel);
        Theme.styleTable(stockTable);
        stockTable.getColumnModel().getColumn(0).setMaxWidth(60);

        JScrollPane stockScroll = new JScrollPane(stockTable);
        stockScroll.setBorder(new LineBorder(Theme.BORDER, 1));
        stockTab.add(stockScroll, BorderLayout.CENTER);

        // Tab 2: Stock Movements Audit
        JPanel movementTab = new JPanel(new BorderLayout(10, 10));
        movementTab.setBackground(Theme.SURFACE);
        movementTab.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filterBar.setOpaque(false);
        filterBar.add(new JLabel("History Limit:"));
        limitCombo = new JComboBox<>(new Integer[]{50, 100, 200, 500});
        limitCombo.setFont(Theme.FONT_BODY);
        limitCombo.addActionListener(e -> refreshMovements());
        filterBar.add(limitCombo);

        JButton filterRefreshBtn = Theme.createSecondaryButton("Filter Movements");
        filterRefreshBtn.addActionListener(e -> refreshMovements());
        filterBar.add(filterRefreshBtn);

        movementTab.add(filterBar, BorderLayout.NORTH);

        String[] moveCols = {"ID", "Timestamp", "Product Name", "Barcode", "Movement Type", "Quantity Delta", "Prev Stock", "New Stock", "Ref / Document", "Audit Notes", "Auditor"};
        movementTableModel = new DefaultTableModel(moveCols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        movementTable = new JTable(movementTableModel);
        Theme.styleTable(movementTable);
        movementTable.getColumnModel().getColumn(0).setMaxWidth(60);

        JScrollPane moveScroll = new JScrollPane(movementTable);
        moveScroll.setBorder(new LineBorder(Theme.BORDER, 1));
        movementTab.add(moveScroll, BorderLayout.CENTER);

        tabs.addTab("📦 Current Stock Snapshot", stockTab);
        tabs.addTab("📋 Movement Audit Trail", movementTab);

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel createKpiCard(String title, JLabel valLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(6, 6));
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.FONT_SMALL);
        titleLabel.setForeground(Theme.TEXT_MUTED);

        valLabel.setFont(Theme.FONT_HEADER);
        valLabel.setForeground(accentColor);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valLabel, BorderLayout.CENTER);
        return card;
    }

    public void refreshAll() {
        refreshValuation();
        refreshStockTable();
        refreshMovements();
    }

    private void refreshValuation() {
        try {
            Map<String, Object> metrics = inventoryService.getInventoryValuation();
            totalItemsVal.setText(String.valueOf(metrics.getOrDefault("total_items", 0)));
            totalUnitsVal.setText(String.valueOf(metrics.getOrDefault("total_units", 0)));

            BigDecimal costVal = (BigDecimal) metrics.getOrDefault("cost_valuation", BigDecimal.ZERO);
            BigDecimal retailVal = (BigDecimal) metrics.getOrDefault("retail_valuation", BigDecimal.ZERO);
            BigDecimal profitVal = (BigDecimal) metrics.getOrDefault("potential_profit", BigDecimal.ZERO);

            costValuationVal.setText(ConsoleFormatter.formatCurrency(costVal));
            retailValuationVal.setText(ConsoleFormatter.formatCurrency(retailVal));
            potentialProfitVal.setText(ConsoleFormatter.formatCurrency(profitVal));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load inventory valuation: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshStockTable() {
        stockTableModel.setRowCount(0);
        try {
            List<Product> products = inventoryService.getCurrentStockList();
            for (Product p : products) {
                BigDecimal costVal = p.getPurchasePrice().multiply(BigDecimal.valueOf(p.getCurrentStock()));
                BigDecimal retailVal = p.getSellingPrice().multiply(BigDecimal.valueOf(p.getCurrentStock()));

                String status;
                if (p.getCurrentStock() <= 0) {
                    status = "🔴 OUT OF STOCK";
                } else if (p.getCurrentStock() <= p.getMinStockLevel()) {
                    status = "🟡 LOW STOCK";
                } else {
                    status = "🟢 HEALTHY";
                }

                stockTableModel.addRow(new Object[]{
                        p.getId(),
                        p.getBarcode(),
                        p.getName(),
                        p.getCategoryName(),
                        p.getCurrentStock(),
                        p.getMinStockLevel(),
                        ConsoleFormatter.formatCurrency(p.getPurchasePrice()),
                        ConsoleFormatter.formatCurrency(p.getSellingPrice()),
                        ConsoleFormatter.formatCurrency(costVal),
                        ConsoleFormatter.formatCurrency(retailVal),
                        status
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load stock list: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshMovements() {
        movementTableModel.setRowCount(0);
        try {
            int limit = limitCombo != null ? (Integer) limitCombo.getSelectedItem() : 100;
            List<StockMovement> list = inventoryService.getStockMovements(null, limit);
            for (StockMovement sm : list) {
                String qtyStr = (sm.getQuantity() > 0 ? "+" : "") + sm.getQuantity();
                movementTableModel.addRow(new Object[]{
                        sm.getId(),
                        ConsoleFormatter.formatDateTime(sm.getCreatedAt()),
                        sm.getProductName(),
                        sm.getProductBarcode(),
                        sm.getMovementType().name(),
                        qtyStr,
                        sm.getPreviousStock(),
                        sm.getNewStock(),
                        sm.getReferenceId(),
                        sm.getNotes(),
                        sm.getCreatedByName() != null ? sm.getCreatedByName() : "System"
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load stock movements: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showStockAdjustmentDialog() {
        List<Product> products = productService.getAllProducts();
        if (products.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No products available for adjustment.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JComboBox<Product> productCombo = new JComboBox<>(products.toArray(new Product[0]));
        productCombo.setFont(Theme.FONT_BODY);
        productCombo.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Product) {
                    Product p = (Product) value;
                    setText(p.getName() + " (SKU: " + p.getBarcode() + ") [Stock: " + p.getCurrentStock() + "]");
                }
                return this;
            }
        });

        StockMovementType[] allowedTypes = new StockMovementType[]{
                StockMovementType.ADJUSTMENT,
                StockMovementType.MANUAL_IN,
                StockMovementType.MANUAL_OUT
        };
        JComboBox<StockMovementType> typeCombo = new JComboBox<>(allowedTypes);
        typeCombo.setFont(Theme.FONT_BODY);

        JTextField qtyField = Theme.createTextField(10);
        JTextField reasonField = Theme.createTextField(25);

        JPanel p = new JPanel(new GridLayout(0, 1, 6, 6));
        p.add(new JLabel("Select Product:"));
        p.add(productCombo);
        p.add(new JLabel("Adjustment Type:"));
        p.add(typeCombo);
        p.add(new JLabel("Quantity Delta (e.g. +5 to increase, -3 to decrease):"));
        p.add(qtyField);
        p.add(new JLabel("Reason / Audit Justification (Required):"));
        p.add(reasonField);

        int res = JOptionPane.showConfirmDialog(this, p, "Record Stock Adjustment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            try {
                Product selected = (Product) productCombo.getSelectedItem();
                if (selected == null) throw new AppException("Please select a product.");

                int delta = Integer.parseInt(qtyField.getText().trim());
                StockMovementType type = (StockMovementType) typeCombo.getSelectedItem();
                String reason = reasonField.getText().trim();

                boolean success = inventoryService.adjustStock(selected.getId(), delta, type, reason, currentUser != null ? currentUser.getId() : 1);
                if (success) {
                    JOptionPane.showMessageDialog(this, "Stock adjusted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    refreshAll();
                } else {
                    JOptionPane.showMessageDialog(this, "Could not perform adjustment.", "Warning", JOptionPane.WARNING_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Quantity delta must be a valid integer.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            } catch (AppException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Adjustment Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

