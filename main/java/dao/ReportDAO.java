package dao;

import config.DBConnection;
import exception.DatabaseOperationException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for analytical business reports, metrics, and Executive Dashboard.
 * Ensures consistent financial definitions and avoids fan-out duplication in aggregates.
 */
public class ReportDAO {

    /**
     * Aggregates the 10 real-time KPIs for the Executive Dashboard.
     * Uses independent subqueries with consistent discount and COGS allocation.
     */
    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        String sqlKpis = "SELECT " +
                "(SELECT COUNT(*) FROM products WHERE is_active = TRUE) AS total_products, " +
                "(SELECT IFNULL(SUM(current_stock), 0) FROM products WHERE is_active = TRUE) AS total_stock_qty, " +
                "(SELECT COUNT(*) FROM products WHERE current_stock <= min_stock_level AND current_stock > 0 AND is_active = TRUE) AS low_stock_count, " +
                "(SELECT COUNT(*) FROM products WHERE current_stock <= 0 AND is_active = TRUE) AS out_of_stock_count, " +
                "(SELECT COUNT(*) FROM suppliers WHERE is_active = TRUE) AS total_suppliers, " +
                "(SELECT COUNT(*) FROM customers) AS total_customers, " +
                "(SELECT IFNULL(SUM(total_amount), 0.00) FROM sales WHERE DATE(sale_date) = CURDATE() AND status = 'COMPLETED') AS today_sales_amount, " +
                "(SELECT COUNT(*) FROM sales WHERE DATE(sale_date) = CURDATE() AND status = 'COMPLETED') AS today_sales_count, " +
                "(SELECT IFNULL(SUM(subtotal), 0.00) FROM sales WHERE YEAR(sale_date) = YEAR(CURDATE()) AND MONTH(sale_date) = MONTH(CURDATE()) AND status = 'COMPLETED') AS monthly_gross_sales, " +
                "(SELECT IFNULL(SUM(discount_amount), 0.00) FROM sales WHERE YEAR(sale_date) = YEAR(CURDATE()) AND MONTH(sale_date) = MONTH(CURDATE()) AND status = 'COMPLETED') AS monthly_discounts, " +
                "(SELECT IFNULL(SUM(total_amount), 0.00) FROM sales WHERE YEAR(sale_date) = YEAR(CURDATE()) AND MONTH(sale_date) = MONTH(CURDATE()) AND status = 'COMPLETED') AS monthly_invoiced_total, " +
                "(SELECT IFNULL(SUM(si.quantity * si.cost_price), 0.00) FROM sale_items si JOIN sales s ON si.sale_id = s.id WHERE YEAR(s.sale_date) = YEAR(CURDATE()) AND MONTH(s.sale_date) = MONTH(CURDATE()) AND s.status = 'COMPLETED') AS monthly_cogs, " +
                "(SELECT IFNULL(SUM(amount), 0.00) FROM expenses WHERE YEAR(expense_date) = YEAR(CURDATE()) AND MONTH(expense_date) = MONTH(CURDATE())) AS monthly_expenses";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlKpis);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                metrics.put("total_products", rs.getInt("total_products"));
                metrics.put("total_stock_qty", rs.getInt("total_stock_qty"));
                metrics.put("low_stock_count", rs.getInt("low_stock_count"));
                metrics.put("out_of_stock_count", rs.getInt("out_of_stock_count"));
                metrics.put("total_suppliers", rs.getInt("total_suppliers"));
                metrics.put("total_customers", rs.getInt("total_customers"));
                metrics.put("today_sales_amount", rs.getBigDecimal("today_sales_amount"));
                metrics.put("today_sales_count", rs.getInt("today_sales_count"));

                BigDecimal grossSales = rs.getBigDecimal("monthly_gross_sales");
                BigDecimal discounts = rs.getBigDecimal("monthly_discounts");
                BigDecimal netRevenue = grossSales.subtract(discounts);
                BigDecimal cogs = rs.getBigDecimal("monthly_cogs");
                BigDecimal grossProfit = netRevenue.subtract(cogs);
                BigDecimal expenses = rs.getBigDecimal("monthly_expenses");
                BigDecimal netProfit = grossProfit.subtract(expenses);

                metrics.put("monthly_gross_sales", grossSales);
                metrics.put("monthly_discounts", discounts);
                metrics.put("monthly_revenue", netRevenue); // Net revenue (Gross - Discounts)
                metrics.put("monthly_invoiced_total", rs.getBigDecimal("monthly_invoiced_total"));
                metrics.put("monthly_cogs", cogs);
                metrics.put("monthly_gross_profit", grossProfit);
                metrics.put("monthly_expenses", expenses);
                metrics.put("estimated_profit", netProfit);
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error calculating dashboard metrics: " + e.getMessage(), e);
        }
        return metrics;
    }

    /**
     * Retrieves recent transactions (sales and purchases) for dashboard stream.
     */
    public List<Map<String, Object>> getRecentTransactions(int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "(SELECT 'SALE' AS txn_type, invoice_number AS ref_num, total_amount, status, sale_date AS txn_date FROM sales ORDER BY sale_date DESC LIMIT ?) " +
                     "UNION ALL " +
                     "(SELECT 'PURCHASE' AS txn_type, purchase_number AS ref_num, total_amount, status, purchase_date AS txn_date FROM purchases ORDER BY purchase_date DESC LIMIT ?) " +
                     "ORDER BY txn_date DESC LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, limit);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("txn_type", rs.getString("txn_type"));
                    map.put("ref_num", rs.getString("ref_num"));
                    map.put("total_amount", rs.getBigDecimal("total_amount"));
                    map.put("status", rs.getString("status"));
                    map.put("txn_date", rs.getTimestamp("txn_date"));
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching recent transactions: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Generates a Sales Report for a given date range.
     */
    public List<Map<String, Object>> getSalesReport(Date start, Date end) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT s.invoice_number, s.sale_date, IFNULL(c.name, 'Walk-in') AS customer_name, " +
                     "s.subtotal, s.discount_amount, s.tax_amount, s.total_amount, s.payment_method, s.status, u.full_name AS cashier " +
                     "FROM sales s " +
                     "LEFT JOIN customers c ON s.customer_id = c.id " +
                     "JOIN users u ON s.user_id = u.id " +
                     "WHERE DATE(s.sale_date) BETWEEN ? AND ? " +
                     "ORDER BY s.sale_date ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, start);
            ps.setDate(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("invoice_number", rs.getString("invoice_number"));
                    row.put("sale_date", rs.getTimestamp("sale_date"));
                    row.put("customer_name", rs.getString("customer_name"));
                    row.put("subtotal", rs.getBigDecimal("subtotal"));
                    row.put("discount_amount", rs.getBigDecimal("discount_amount"));
                    row.put("tax_amount", rs.getBigDecimal("tax_amount"));
                    row.put("total_amount", rs.getBigDecimal("total_amount"));
                    row.put("payment_method", rs.getString("payment_method"));
                    row.put("status", rs.getString("status"));
                    row.put("cashier", rs.getString("cashier"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error generating sales report: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Generates a Purchase Report for a given date range.
     */
    public List<Map<String, Object>> getPurchaseReport(Date start, Date end) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT p.purchase_number, p.purchase_date, s.company_name AS supplier_name, " +
                     "p.total_amount, p.status, u.full_name AS ordered_by " +
                     "FROM purchases p " +
                     "JOIN suppliers s ON p.supplier_id = s.id " +
                     "JOIN users u ON p.user_id = u.id " +
                     "WHERE DATE(p.purchase_date) BETWEEN ? AND ? " +
                     "ORDER BY p.purchase_date ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, start);
            ps.setDate(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("purchase_number", rs.getString("purchase_number"));
                    row.put("purchase_date", rs.getTimestamp("purchase_date"));
                    row.put("supplier_name", rs.getString("supplier_name"));
                    row.put("total_amount", rs.getBigDecimal("total_amount"));
                    row.put("status", rs.getString("status"));
                    row.put("ordered_by", rs.getString("ordered_by"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error generating purchase report: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Generates a comprehensive Profit and Loss Statement.
     * Computes Gross Sales, Discounts, and Net Revenue directly from sales headers,
     * computes Cost of Goods Sold (COGS) independently from completed sale_items,
     * and computes Operating Expenses independently from expenses.
     * Eliminates row fan-out duplication.
     */
    public Map<String, BigDecimal> getProfitAndLossReport(Date start, Date end) {
        Map<String, BigDecimal> pnl = new LinkedHashMap<>();

        // 1. Sales Header Aggregates (Independent query: avoids fan-out duplicate amounts)
        String sqlSalesHeaders = "SELECT " +
                                 "IFNULL(SUM(subtotal), 0.00) AS gross_sales, " +
                                 "IFNULL(SUM(discount_amount), 0.00) AS total_discounts, " +
                                 "IFNULL(SUM(tax_amount), 0.00) AS total_tax, " +
                                 "IFNULL(SUM(subtotal - discount_amount), 0.00) AS net_revenue, " +
                                 "IFNULL(SUM(total_amount), 0.00) AS total_invoiced " +
                                 "FROM sales " +
                                 "WHERE DATE(sale_date) BETWEEN ? AND ? AND status = 'COMPLETED'";

        // 2. Cost of Goods Sold from completed sale items only
        String sqlCogs = "SELECT IFNULL(SUM(si.quantity * si.cost_price), 0.00) AS cogs " +
                         "FROM sale_items si " +
                         "JOIN sales s ON si.sale_id = s.id " +
                         "WHERE DATE(s.sale_date) BETWEEN ? AND ? AND s.status = 'COMPLETED'";

        // 3. Operating Expenses
        String sqlExpenses = "SELECT IFNULL(SUM(amount), 0.00) AS total_expenses " +
                             "FROM expenses " +
                             "WHERE expense_date BETWEEN ? AND ?";

        BigDecimal grossSales = BigDecimal.ZERO;
        BigDecimal totalDiscounts = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal netRevenue = BigDecimal.ZERO;
        BigDecimal totalInvoiced = BigDecimal.ZERO;
        BigDecimal cogs = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        try (Connection conn = DBConnection.getConnection()) {
            // Execute Header Aggregates
            try (PreparedStatement ps = conn.prepareStatement(sqlSalesHeaders)) {
                ps.setDate(1, start);
                ps.setDate(2, end);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        grossSales = rs.getBigDecimal("gross_sales");
                        totalDiscounts = rs.getBigDecimal("total_discounts");
                        totalTax = rs.getBigDecimal("total_tax");
                        netRevenue = rs.getBigDecimal("net_revenue");
                        totalInvoiced = rs.getBigDecimal("total_invoiced");
                    }
                }
            }

            // Execute COGS query
            try (PreparedStatement ps = conn.prepareStatement(sqlCogs)) {
                ps.setDate(1, start);
                ps.setDate(2, end);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        cogs = rs.getBigDecimal("cogs");
                    }
                }
            }

            // Execute Expenses query
            try (PreparedStatement ps = conn.prepareStatement(sqlExpenses)) {
                ps.setDate(1, start);
                ps.setDate(2, end);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        totalExpenses = rs.getBigDecimal("total_expenses");
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error generating profit and loss report: " + e.getMessage(), e);
        }

        BigDecimal grossProfit = netRevenue.subtract(cogs);
        BigDecimal netProfit = grossProfit.subtract(totalExpenses);

        pnl.put("gross_sales", grossSales);
        pnl.put("total_discounts", totalDiscounts);
        pnl.put("net_revenue", netRevenue);
        pnl.put("total_tax", totalTax);
        pnl.put("total_invoiced", totalInvoiced);
        pnl.put("cogs", cogs);
        pnl.put("gross_profit", grossProfit);
        pnl.put("operating_expenses", totalExpenses);
        pnl.put("net_profit", netProfit);

        return pnl;
    }

    /**
     * Retrieves the top-selling products ranked by units sold and generated revenue.
     */
    public List<Map<String, Object>> getTopSellingProducts(int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT p.id, p.barcode, p.name, c.name AS category_name, " +
                     "SUM(si.quantity) AS units_sold, " +
                     "SUM(si.subtotal) AS total_revenue " +
                     "FROM sale_items si " +
                     "JOIN sales s ON si.sale_id = s.id " +
                     "JOIN products p ON si.product_id = p.id " +
                     "JOIN categories c ON p.category_id = c.id " +
                     "WHERE s.status = 'COMPLETED' " +
                     "GROUP BY p.id, p.barcode, p.name, c.name " +
                     "ORDER BY units_sold DESC, total_revenue DESC " +
                     "LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("product_id", rs.getInt("id"));
                    map.put("barcode", rs.getString("barcode"));
                    map.put("product_name", rs.getString("name"));
                    map.put("category_name", rs.getString("category_name"));
                    map.put("units_sold", rs.getInt("units_sold"));
                    map.put("total_revenue", rs.getBigDecimal("total_revenue"));
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching top selling products: " + e.getMessage(), e);
        }
        return list;
    }
}
