package ui;

import service.ReportService;
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
import java.util.Map;

/**
 * Modern Business Intelligence, Analytical Reports, and Profit & Loss panel.
 */
public class ReportPanel extends JPanel {

    private final ReportService reportService;

    // Sales Report components
    private JTable salesTable;
    private DefaultTableModel salesModel;
    private JLabel salesSummaryLabel;
    private JTextField salesStartField;
    private JTextField salesEndField;

    // Purchase Report components
    private JTable purchaseTable;
    private DefaultTableModel purchaseModel;
    private JLabel purchaseSummaryLabel;
    private JTextField poStartField;
    private JTextField poEndField;

    // P&L Statement components
    private JTextField pnlStartField;
    private JTextField pnlEndField;
    private JPanel pnlStatementContainer;

    // Top Selling components
    private JTable topProductsTable;
    private DefaultTableModel topProductsModel;
    private JComboBox<Integer> topLimitCombo;

    public ReportPanel(ReportService reportService) {
        this.reportService = reportService;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(Theme.APP_BG);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel title = new JLabel("Business Intelligence & Financial Reports");
        title.setFont(Theme.FONT_TITLE_LARGE);
        title.setForeground(Theme.TEXT_PRIMARY);

        headerRow.add(title, BorderLayout.WEST);
        add(headerRow, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.FONT_BODY_BOLD);
        tabs.setBackground(Theme.SURFACE);

        tabs.addTab("📈 Sales Performance", buildSalesReportTab());
        tabs.addTab("📥 Purchase Analysis", buildPurchaseReportTab());
        tabs.addTab("💰 Profit & Loss Statement", buildProfitLossTab());
        tabs.addTab("🏆 Top Selling Products", buildTopSellingTab());

        add(tabs, BorderLayout.CENTER);

        // Initial loads
        loadSalesData();
        loadPurchaseData();
        loadProfitLossData();
        loadTopSellingData();
    }

    // ==========================================
    // Tab 1: Sales Analysis
    // ==========================================
    private JPanel buildSalesReportTab() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(Theme.SURFACE);
        p.setBorder(new EmptyBorder(14, 14, 14, 14));

        // Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterBar.setOpaque(false);

        LocalDate now = LocalDate.now();
        salesStartField = Theme.createTextField(10);
        salesStartField.setText(now.withDayOfMonth(1).toString());
        salesEndField = Theme.createTextField(10);
        salesEndField.setText(now.toString());

        JButton todayBtn = Theme.createSecondaryButton("Today");
        todayBtn.addActionListener(e -> {
            salesStartField.setText(LocalDate.now().toString());
            salesEndField.setText(LocalDate.now().toString());
            loadSalesData();
        });

        JButton monthBtn = Theme.createSecondaryButton("This Month");
        monthBtn.addActionListener(e -> {
            salesStartField.setText(LocalDate.now().withDayOfMonth(1).toString());
            salesEndField.setText(LocalDate.now().toString());
            loadSalesData();
        });

        JButton applyBtn = Theme.createPrimaryButton("Run Report");
        applyBtn.addActionListener(e -> loadSalesData());

        filterBar.add(new JLabel("From:"));
        filterBar.add(salesStartField);
        filterBar.add(new JLabel("To:"));
        filterBar.add(salesEndField);
        filterBar.add(applyBtn);
        filterBar.add(todayBtn);
        filterBar.add(monthBtn);

        p.add(filterBar, BorderLayout.NORTH);

        // Table
        String[] cols = {"Invoice #", "Sale Date", "Customer", "Subtotal", "Discount", "Tax", "Net Total", "Payment", "Status", "Cashier"};
        salesModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        salesTable = new JTable(salesModel);
        Theme.styleTable(salesTable);

        p.add(new JScrollPane(salesTable), BorderLayout.CENTER);

        salesSummaryLabel = new JLabel("Total Sales: ₹0.00");
        salesSummaryLabel.setFont(Theme.FONT_TITLE);
        salesSummaryLabel.setForeground(Theme.PRIMARY);
        salesSummaryLabel.setBorder(new EmptyBorder(8, 4, 4, 4));
        p.add(salesSummaryLabel, BorderLayout.SOUTH);

        return p;
    }

    private void loadSalesData() {
        salesModel.setRowCount(0);
        try {
            Date start = Date.valueOf(salesStartField.getText().trim());
            Date end = Date.valueOf(salesEndField.getText().trim());

            List<Map<String, Object>> rows = reportService.getCustomSalesReport(start, end);
            BigDecimal totalGross = BigDecimal.ZERO;
            BigDecimal totalDisc = BigDecimal.ZERO;
            BigDecimal totalTax = BigDecimal.ZERO;
            BigDecimal totalInvoiced = BigDecimal.ZERO;

            for (Map<String, Object> r : rows) {
                BigDecimal sub = (BigDecimal) r.get("subtotal");
                BigDecimal disc = (BigDecimal) r.get("discount_amount");
                BigDecimal tax = (BigDecimal) r.get("tax_amount");
                BigDecimal tot = (BigDecimal) r.get("total_amount");

                if (sub != null) totalGross = totalGross.add(sub);
                if (disc != null) totalDisc = totalDisc.add(disc);
                if (tax != null) totalTax = totalTax.add(tax);
                if (tot != null) totalInvoiced = totalInvoiced.add(tot);

                salesModel.addRow(new Object[]{
                        r.get("invoice_number"),
                        ConsoleFormatter.formatDateTime((java.util.Date) r.get("sale_date")),
                        r.get("customer_name"),
                        ConsoleFormatter.formatCurrency(sub),
                        ConsoleFormatter.formatCurrency(disc),
                        ConsoleFormatter.formatCurrency(tax),
                        ConsoleFormatter.formatCurrency(tot),
                        r.get("payment_method"),
                        r.get("status"),
                        r.get("cashier")
                });
            }

            salesSummaryLabel.setText(String.format("Period Summary: %d Invoices | Gross: %s | Discounts: -%s | Tax: +%s | Total Invoiced: %s",
                    rows.size(),
                    ConsoleFormatter.formatCurrency(totalGross),
                    ConsoleFormatter.formatCurrency(totalDisc),
                    ConsoleFormatter.formatCurrency(totalTax),
                    ConsoleFormatter.formatCurrency(totalInvoiced)
            ));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Sales Report Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==========================================
    // Tab 2: Purchase Analysis
    // ==========================================
    private JPanel buildPurchaseReportTab() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(Theme.SURFACE);
        p.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterBar.setOpaque(false);

        LocalDate now = LocalDate.now();
        poStartField = Theme.createTextField(10);
        poStartField.setText(now.withDayOfMonth(1).toString());
        poEndField = Theme.createTextField(10);
        poEndField.setText(now.toString());

        JButton applyBtn = Theme.createPrimaryButton("Run PO Report");
        applyBtn.addActionListener(e -> loadPurchaseData());

        filterBar.add(new JLabel("From:"));
        filterBar.add(poStartField);
        filterBar.add(new JLabel("To:"));
        filterBar.add(poEndField);
        filterBar.add(applyBtn);

        p.add(filterBar, BorderLayout.NORTH);

        String[] cols = {"PO Number", "Purchase Date", "Supplier Name", "Total Amount", "Status", "Ordered By"};
        purchaseModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        purchaseTable = new JTable(purchaseModel);
        Theme.styleTable(purchaseTable);

        p.add(new JScrollPane(purchaseTable), BorderLayout.CENTER);

        purchaseSummaryLabel = new JLabel("Total Purchases: ₹0.00");
        purchaseSummaryLabel.setFont(Theme.FONT_TITLE);
        purchaseSummaryLabel.setForeground(Theme.SECONDARY);
        purchaseSummaryLabel.setBorder(new EmptyBorder(8, 4, 4, 4));
        p.add(purchaseSummaryLabel, BorderLayout.SOUTH);

        return p;
    }

    private void loadPurchaseData() {
        purchaseModel.setRowCount(0);
        try {
            Date start = Date.valueOf(poStartField.getText().trim());
            Date end = Date.valueOf(poEndField.getText().trim());

            List<Map<String, Object>> rows = reportService.getPurchaseReport(start, end);
            BigDecimal totalPo = BigDecimal.ZERO;

            for (Map<String, Object> r : rows) {
                BigDecimal tot = (BigDecimal) r.get("total_amount");
                if (tot != null) totalPo = totalPo.add(tot);

                purchaseModel.addRow(new Object[]{
                        r.get("purchase_number"),
                        ConsoleFormatter.formatDateTime((java.util.Date) r.get("purchase_date")),
                        r.get("supplier_name"),
                        ConsoleFormatter.formatCurrency(tot),
                        r.get("status"),
                        r.get("ordered_by")
                });
            }

            purchaseSummaryLabel.setText(String.format("Period Purchases: %d Orders | Total Procurement: %s",
                    rows.size(),
                    ConsoleFormatter.formatCurrency(totalPo)
            ));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Purchase Report Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==========================================
    // Tab 3: Profit & Loss Statement (P&L)
    // ==========================================
    private JPanel buildProfitLossTab() {
        JPanel p = new JPanel(new BorderLayout(14, 14));
        p.setBackground(Theme.SURFACE);
        p.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterBar.setOpaque(false);

        LocalDate now = LocalDate.now();
        pnlStartField = Theme.createTextField(10);
        pnlStartField.setText(now.withDayOfMonth(1).toString());
        pnlEndField = Theme.createTextField(10);
        pnlEndField.setText(now.toString());

        JButton applyBtn = Theme.createPrimaryButton("Generate P&L Statement");
        applyBtn.addActionListener(e -> loadProfitLossData());

        JButton monthBtn = Theme.createSecondaryButton("Current Month");
        monthBtn.addActionListener(e -> {
            pnlStartField.setText(LocalDate.now().withDayOfMonth(1).toString());
            pnlEndField.setText(LocalDate.now().toString());
            loadProfitLossData();
        });

        JButton yearBtn = Theme.createSecondaryButton("Year to Date");
        yearBtn.addActionListener(e -> {
            pnlStartField.setText(LocalDate.of(LocalDate.now().getYear(), 1, 1).toString());
            pnlEndField.setText(LocalDate.now().toString());
            loadProfitLossData();
        });

        filterBar.add(new JLabel("Period From:"));
        filterBar.add(pnlStartField);
        filterBar.add(new JLabel("To:"));
        filterBar.add(pnlEndField);
        filterBar.add(applyBtn);
        filterBar.add(monthBtn);
        filterBar.add(yearBtn);

        p.add(filterBar, BorderLayout.NORTH);

        pnlStatementContainer = new JPanel();
        pnlStatementContainer.setLayout(new BoxLayout(pnlStatementContainer, BoxLayout.Y_AXIS));
        pnlStatementContainer.setBackground(Theme.SURFACE);

        JScrollPane scroll = new JScrollPane(pnlStatementContainer);
        scroll.setBorder(new LineBorder(Theme.BORDER, 1));
        p.add(scroll, BorderLayout.CENTER);

        return p;
    }

    private void loadProfitLossData() {
        pnlStatementContainer.removeAll();
        try {
            Date start = Date.valueOf(pnlStartField.getText().trim());
            Date end = Date.valueOf(pnlEndField.getText().trim());

            Map<String, BigDecimal> pnl = reportService.getProfitAndLossReport(start, end);

            BigDecimal grossSales = pnl.getOrDefault("gross_sales", BigDecimal.ZERO);
            BigDecimal discounts = pnl.getOrDefault("total_discounts", BigDecimal.ZERO);
            BigDecimal netRevenue = pnl.getOrDefault("net_revenue", BigDecimal.ZERO);
            BigDecimal tax = pnl.getOrDefault("total_tax", BigDecimal.ZERO);
            BigDecimal totalInvoiced = pnl.getOrDefault("total_invoiced", BigDecimal.ZERO);
            BigDecimal cogs = pnl.getOrDefault("cogs", BigDecimal.ZERO);
            BigDecimal grossProfit = pnl.getOrDefault("gross_profit", BigDecimal.ZERO);
            BigDecimal expenses = pnl.getOrDefault("operating_expenses", BigDecimal.ZERO);
            BigDecimal netProfit = pnl.getOrDefault("net_profit", BigDecimal.ZERO);

            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(Theme.SURFACE);
            card.setBorder(new EmptyBorder(20, 40, 20, 40));

            JLabel title = new JLabel("STATEMENT OF PROFIT AND LOSS");
            title.setFont(Theme.FONT_TITLE_LARGE);
            title.setForeground(Theme.TEXT_PRIMARY);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel subtitle = new JLabel(String.format("Reporting Period: %s to %s | Accounting Basis: Accrual / Independent Multi-Item", start, end));
            subtitle.setFont(Theme.FONT_SMALL);
            subtitle.setForeground(Theme.TEXT_MUTED);
            subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

            card.add(title);
            card.add(subtitle);
            card.add(Box.createVerticalStrut(18));

            // Section 1: Revenue
            card.add(createPnlRow("1. Gross Sales Revenue", grossSales, false, false, Theme.TEXT_PRIMARY));
            card.add(createPnlRow("   Less: Sales Discounts & Allowances", discounts.negate(), false, false, Theme.DANGER));
            card.add(new JSeparator(SwingConstants.HORIZONTAL));
            card.add(createPnlRow("Net Operating Revenue", netRevenue, true, false, Theme.PRIMARY));
            card.add(Box.createVerticalStrut(12));

            // Section 2: COGS & Gross Profit
            card.add(createPnlRow("2. Cost of Goods Sold (COGS)", cogs.negate(), false, false, Theme.DANGER));
            card.add(new JSeparator(SwingConstants.HORIZONTAL));
            card.add(createPnlRow("Gross Operating Profit", grossProfit, true, false, grossProfit.compareTo(BigDecimal.ZERO) >= 0 ? Theme.SUCCESS : Theme.DANGER));
            card.add(Box.createVerticalStrut(12));

            // Section 3: Operating Expenses
            card.add(createPnlRow("3. Operating Business Expenses", expenses.negate(), false, false, Theme.DANGER));
            card.add(new JSeparator(SwingConstants.HORIZONTAL));

            // Bottom Line: Net Profit
            Color netColor = netProfit.compareTo(BigDecimal.ZERO) >= 0 ? Theme.SUCCESS : Theme.DANGER;
            card.add(createPnlRow("NET BUSINESS PROFIT / (LOSS)", netProfit, true, true, netColor));
            card.add(Box.createVerticalStrut(16));

            // Tax note
            JLabel taxNote = new JLabel(String.format("Sales Tax Collected (Fiduciary Liability): %s | Gross Invoiced Receipts: %s",
                    ConsoleFormatter.formatCurrency(tax),
                    ConsoleFormatter.formatCurrency(totalInvoiced)));
            taxNote.setFont(Theme.FONT_SMALL);
            taxNote.setForeground(Theme.TEXT_MUTED);
            card.add(taxNote);

            pnlStatementContainer.add(card);
            pnlStatementContainer.revalidate();
            pnlStatementContainer.repaint();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to calculate P&L: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createPnlRow(String title, BigDecimal amount, boolean isBold, boolean isLarge, Color color) {
        JPanel r = new JPanel(new BorderLayout());
        r.setOpaque(false);
        r.setMaximumSize(new Dimension(800, isLarge ? 36 : 28));

        JLabel lTitle = new JLabel(title);
        lTitle.setFont(isLarge ? Theme.FONT_HEADER : (isBold ? Theme.FONT_BODY_BOLD : Theme.FONT_BODY));
        lTitle.setForeground(Theme.TEXT_PRIMARY);

        JLabel lAmt = new JLabel(ConsoleFormatter.formatCurrency(amount));
        lAmt.setFont(isLarge ? Theme.FONT_HEADER : (isBold ? Theme.FONT_BODY_BOLD : Theme.FONT_BODY));
        lAmt.setForeground(color);

        r.add(lTitle, BorderLayout.WEST);
        r.add(lAmt, BorderLayout.EAST);
        return r;
    }

    // ==========================================
    // Tab 4: Top Performing Products
    // ==========================================
    private JPanel buildTopSellingTab() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(Theme.SURFACE);
        p.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterBar.setOpaque(false);

        topLimitCombo = new JComboBox<>(new Integer[]{10, 20, 50});
        topLimitCombo.setFont(Theme.FONT_BODY);
        topLimitCombo.addActionListener(e -> loadTopSellingData());

        JButton refreshBtn = Theme.createSecondaryButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> loadTopSellingData());

        filterBar.add(new JLabel("Top Items Limit:"));
        filterBar.add(topLimitCombo);
        filterBar.add(refreshBtn);

        p.add(filterBar, BorderLayout.NORTH);

        String[] cols = {"Rank", "Product ID", "Barcode", "Product Name", "Category", "Units Sold", "Total Revenue"};
        topProductsModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        topProductsTable = new JTable(topProductsModel);
        Theme.styleTable(topProductsTable);
        topProductsTable.getColumnModel().getColumn(0).setMaxWidth(60);

        p.add(new JScrollPane(topProductsTable), BorderLayout.CENTER);

        return p;
    }

    private void loadTopSellingData() {
        topProductsModel.setRowCount(0);
        try {
            int limit = topLimitCombo != null ? (Integer) topLimitCombo.getSelectedItem() : 10;
            List<Map<String, Object>> list = reportService.getTopSellingProducts(limit);
            int rank = 1;
            for (Map<String, Object> r : list) {
                topProductsModel.addRow(new Object[]{
                        "#" + rank++,
                        r.get("product_id"),
                        r.get("barcode"),
                        r.get("product_name"),
                        r.get("category_name"),
                        r.get("units_sold"),
                        ConsoleFormatter.formatCurrency((BigDecimal) r.get("total_revenue"))
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Top Selling Report Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

