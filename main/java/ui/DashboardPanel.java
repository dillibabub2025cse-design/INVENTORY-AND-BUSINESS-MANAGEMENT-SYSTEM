package ui;

import model.User;
import service.ReportService;
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
 * Modern Executive Dashboard panel with real-time KPI metric cards and recent transactions stream.
 */
public class DashboardPanel extends JPanel {

    private final ReportService reportService;
    private final User currentUser;

    private JLabel lblTotalProducts;
    private JLabel lblTotalStock;
    private JLabel lblLowStock;
    private JLabel lblTodaySales;
    private JLabel lblMonthlyRevenue;
    private JLabel lblMonthlyExpenses;
    private JLabel lblNetProfit;
    private JLabel lblSuppliers;
    private JLabel lblCustomers;

    private DefaultTableModel recentTxnModel;

    public DashboardPanel(ReportService reportService, User currentUser) {
        this.reportService = reportService;
        this.currentUser = currentUser;
        initUI();
        refreshData();
    }

    private void initUI() {
        setLayout(new BorderLayout(16, 16));
        setBackground(Theme.APP_BG);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        // Top Header
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel title = new JLabel("Executive Business Dashboard");
        title.setFont(Theme.FONT_TITLE_LARGE);
        title.setForeground(Theme.TEXT_PRIMARY);

        JButton refreshBtn = Theme.createSecondaryButton("Refresh Metrics");
        refreshBtn.addActionListener(e -> refreshData());

        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(refreshBtn, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Center Scrollable content
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // Metric Cards Grid (2 rows x 4 cols or 3 x 3)
        JPanel cardsGrid = new JPanel(new GridLayout(2, 4, 14, 14));
        cardsGrid.setOpaque(false);
        cardsGrid.setMaximumSize(new Dimension(1600, 240));

        lblTotalProducts = new JLabel("0");
        lblTotalStock = new JLabel("0 units");
        lblLowStock = new JLabel("0");
        lblTodaySales = new JLabel("₹0.00");
        lblMonthlyRevenue = new JLabel("₹0.00");
        lblMonthlyExpenses = new JLabel("₹0.00");
        lblNetProfit = new JLabel("₹0.00");
        lblSuppliers = new JLabel("0");

        cardsGrid.add(createKpiCard("TOTAL PRODUCTS", lblTotalProducts, "Catalog items", Theme.PRIMARY));
        cardsGrid.add(createKpiCard("TOTAL STOCK", lblTotalStock, "Units on hand", Theme.PRIMARY));
        cardsGrid.add(createKpiCard("LOW STOCK ALERTS", lblLowStock, "Below minimum", Theme.WARNING));
        cardsGrid.add(createKpiCard("TODAY'S SALES", lblTodaySales, "Completed today", Theme.SUCCESS));

        cardsGrid.add(createKpiCard("MONTHLY NET SALES", lblMonthlyRevenue, "Current month", Theme.PRIMARY));
        cardsGrid.add(createKpiCard("OPERATING EXPENSES", lblMonthlyExpenses, "Monthly expenses", Theme.DANGER));
        cardsGrid.add(createKpiCard("ESTIMATED NET PROFIT", lblNetProfit, "Revenue - COGS - Exp", Theme.SUCCESS));
        cardsGrid.add(createKpiCard("ACTIVE SUPPLIERS", lblSuppliers, "Registered vendors", Theme.TEXT_MUTED));

        contentPanel.add(cardsGrid);
        contentPanel.add(Box.createVerticalStrut(20));

        // Recent Transactions Section
        JPanel recentPanel = Theme.createCardPanel();
        recentPanel.setLayout(new BorderLayout(10, 10));

        JLabel recentTitle = new JLabel("Recent Activity Stream (Sales & Purchases)");
        recentTitle.setFont(Theme.FONT_HEADER);
        recentTitle.setForeground(Theme.TEXT_PRIMARY);
        recentPanel.add(recentTitle, BorderLayout.NORTH);

        String[] cols = {"Type", "Reference #", "Amount", "Status", "Date & Time"};
        recentTxnModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable txnTable = new JTable(recentTxnModel);
        Theme.styleTable(txnTable);
        JScrollPane scroll = new JScrollPane(txnTable);
        scroll.setBorder(new LineBorder(Theme.BORDER, 1));
        scroll.setPreferredSize(new Dimension(800, 260));

        recentPanel.add(scroll, BorderLayout.CENTER);
        contentPanel.add(recentPanel);

        JScrollPane mainScroll = new JScrollPane(contentPanel);
        mainScroll.setBorder(null);
        mainScroll.setOpaque(false);
        mainScroll.getViewport().setOpaque(false);
        add(mainScroll, BorderLayout.CENTER);
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, String subtitle, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(6, 6));
        card.setBackground(Theme.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.BORDER, 1),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.FONT_SMALL_BOLD);
        titleLabel.setForeground(Theme.TEXT_MUTED);

        valueLabel.setFont(Theme.FONT_TITLE_LARGE);
        valueLabel.setForeground(accentColor);

        JLabel sub = new JLabel(subtitle);
        sub.setFont(Theme.FONT_SMALL);
        sub.setForeground(Theme.TEXT_MUTED);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(sub, BorderLayout.SOUTH);

        return card;
    }

    public void refreshData() {
        try {
            Map<String, Object> m = reportService.getDashboardMetrics();
            lblTotalProducts.setText(String.valueOf(m.get("total_products")));
            lblTotalStock.setText(m.get("total_stock_qty") + " units");
            lblLowStock.setText(String.valueOf(m.get("low_stock_count")));

            BigDecimal todayAmt = (BigDecimal) m.get("today_sales_amount");
            int todayCount = (int) m.get("today_sales_count");
            lblTodaySales.setText(ConsoleFormatter.formatCurrency(todayAmt) + " (" + todayCount + ")");

            BigDecimal netRev = (BigDecimal) m.get("monthly_revenue");
            BigDecimal exp = (BigDecimal) m.get("monthly_expenses");
            BigDecimal profit = (BigDecimal) m.get("estimated_profit");

            lblMonthlyRevenue.setText(ConsoleFormatter.formatCurrency(netRev));
            lblMonthlyExpenses.setText(ConsoleFormatter.formatCurrency(exp));
            lblNetProfit.setText(ConsoleFormatter.formatCurrency(profit));
            if (profit != null && profit.signum() < 0) {
                lblNetProfit.setForeground(Theme.DANGER);
            } else {
                lblNetProfit.setForeground(Theme.SUCCESS);
            }

            lblSuppliers.setText(String.valueOf(m.get("total_suppliers")));

            // Refresh recent transactions
            recentTxnModel.setRowCount(0);
            List<Map<String, Object>> txns = reportService.getRecentTransactions(8);
            for (Map<String, Object> t : txns) {
                recentTxnModel.addRow(new Object[]{
                        t.get("txn_type"),
                        t.get("ref_num"),
                        ConsoleFormatter.formatCurrency((BigDecimal) t.get("total_amount")),
                        t.get("status"),
                        ConsoleFormatter.formatDateTime((java.util.Date) t.get("txn_date"))
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading dashboard metrics: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

