package service;

import dao.ReportDAO;
import util.ConsoleFormatter;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service managing Business Intelligence reports, analytical statements, and Executive Dashboard.
 */
public class ReportService {

    private final ReportDAO reportDAO;

    public ReportService() {
        this.reportDAO = new ReportDAO();
    }

    public ReportService(ReportDAO reportDAO) {
        this.reportDAO = reportDAO;
    }

    public Map<String, Object> getDashboardMetrics() {
        return reportDAO.getDashboardMetrics();
    }

    public List<Map<String, Object>> getRecentTransactions(int limit) {
        return reportDAO.getRecentTransactions(limit);
    }

    public List<Map<String, Object>> getDailySalesReport(LocalDate date) {
        Date sqlDate = Date.valueOf(date);
        return reportDAO.getSalesReport(sqlDate, sqlDate);
    }

    public List<Map<String, Object>> getWeeklySalesReport() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(7);
        return reportDAO.getSalesReport(Date.valueOf(start), Date.valueOf(end));
    }

    public List<Map<String, Object>> getMonthlySalesReport(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        return reportDAO.getSalesReport(Date.valueOf(start), Date.valueOf(end));
    }

    public List<Map<String, Object>> getAnnualSalesReport(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return reportDAO.getSalesReport(Date.valueOf(start), Date.valueOf(end));
    }

    public List<Map<String, Object>> getCustomSalesReport(Date start, Date end) {
        return reportDAO.getSalesReport(start, end);
    }

    public List<Map<String, Object>> getPurchaseReport(Date start, Date end) {
        return reportDAO.getPurchaseReport(start, end);
    }

    public Map<String, BigDecimal> getProfitAndLossReport(Date start, Date end) {
        return reportDAO.getProfitAndLossReport(start, end);
    }

    public List<Map<String, Object>> getTopSellingProducts(int limit) {
        return reportDAO.getTopSellingProducts(limit);
    }

    /**
     * Prints the complete executive dashboard view.
     */
    public void printDashboard() {
        Map<String, Object> m = getDashboardMetrics();

        ConsoleFormatter.printBanner("EXECUTIVE BUSINESS DASHBOARD");
        System.out.println("  Report Timestamp: " + ConsoleFormatter.formatDateTime(new java.util.Date()));
        ConsoleFormatter.printDoubleDivider(78);

        System.out.println("\n [ INVENTORY HEALTH ]");
        ConsoleFormatter.printMetric("Total Catalog Products", m.get("total_products"), 28);
        ConsoleFormatter.printMetric("Total Stock Units", m.get("total_stock_qty"), 28);
        ConsoleFormatter.printMetric("Low Stock Items (< Min)", m.get("low_stock_count"), 28);
        ConsoleFormatter.printMetric("Out of Stock Items", m.get("out_of_stock_count"), 28);

        System.out.println("\n [ STAKEHOLDERS ]");
        ConsoleFormatter.printMetric("Active Suppliers", m.get("total_suppliers"), 28);
        ConsoleFormatter.printMetric("Registered Customers", m.get("total_customers"), 28);

        System.out.println("\n [ FINANCIAL PERFORMANCE ]");
        ConsoleFormatter.printMetric("Today's Completed Sales", m.get("today_sales_count") + " orders (" + 
                ConsoleFormatter.formatCurrency((BigDecimal) m.get("today_sales_amount")) + ")", 28);
        ConsoleFormatter.printMetric("Current Month Revenue", ConsoleFormatter.formatCurrency((BigDecimal) m.get("monthly_revenue")), 28);
        ConsoleFormatter.printMetric("Current Month Expenses", ConsoleFormatter.formatCurrency((BigDecimal) m.get("monthly_expenses")), 28);
        ConsoleFormatter.printMetric("Estimated Monthly Net Profit", ConsoleFormatter.formatCurrency((BigDecimal) m.get("estimated_profit")), 28);

        System.out.println("\n [ RECENT ACTIVITY STREAM ]");
        List<Map<String, Object>> recent = getRecentTransactions(5);
        if (recent.isEmpty()) {
            System.out.println("  No recent transactions found.");
        } else {
            System.out.printf("  %-10s %-20s %-15s %-12s %-20s\n", "Type", "Reference", "Amount", "Status", "Date");
            ConsoleFormatter.printDivider(78);
            for (Map<String, Object> t : recent) {
                System.out.printf("  %-10s %-20s %-15s %-12s %-20s\n",
                        t.get("txn_type"),
                        t.get("ref_num"),
                        ConsoleFormatter.formatCurrency((BigDecimal) t.get("total_amount")),
                        t.get("status"),
                        ConsoleFormatter.formatDateTime((java.util.Date) t.get("txn_date")));
            }
        }
        ConsoleFormatter.printDoubleDivider(78);
    }
}

