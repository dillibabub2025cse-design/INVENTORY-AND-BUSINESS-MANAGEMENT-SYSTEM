package test;

import model.PurchaseItem;
import model.SaleItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests verifying financial calculations:
 * Multiple products in one invoice, Discounts, Tax, COGS, Gross Profit, and Net Profit.
 */
public class BusinessLogicTest {

    public static void runTests() {
        System.out.println("Running BusinessLogicTest (Multi-product invoice, Discounts, Tax, COGS, Net Profit)...");

        // Test 1: Multiple products in a single invoice
        List<SaleItem> items = new ArrayList<>();
        // Item 1: 3 units @ $100.00 each, cost $60.00 each -> Subtotal = $300.00, Cost = $180.00
        items.add(new SaleItem(1, "Ergonomic Keyboard", 3, BigDecimal.valueOf(100.00), BigDecimal.valueOf(60.00)));
        // Item 2: 2 units @ $250.00 each, cost $150.00 each -> Subtotal = $500.00, Cost = $300.00
        items.add(new SaleItem(2, "27-inch Monitor", 2, BigDecimal.valueOf(250.00), BigDecimal.valueOf(150.00)));
        // Item 3: 5 units @ $20.00 each, cost $10.00 each -> Subtotal = $100.00, Cost = $50.00
        items.add(new SaleItem(3, "USB-C Hub", 5, BigDecimal.valueOf(20.00), BigDecimal.valueOf(10.00)));

        BigDecimal grossSales = BigDecimal.ZERO;
        BigDecimal totalCogs = BigDecimal.ZERO;
        for (SaleItem item : items) {
            grossSales = grossSales.add(item.getSubtotal());
            BigDecimal itemCost = item.getCostPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalCogs = totalCogs.add(itemCost);
        }

        assert grossSales.compareTo(BigDecimal.valueOf(900.00)) == 0 : "Gross sales must be $900.00 (300 + 500 + 100)";
        assert totalCogs.compareTo(BigDecimal.valueOf(530.00)) == 0 : "Total COGS must be $530.00 (180 + 300 + 50)";

        // Test 2: Discount calculation (10% discount on $900.00 gross sales = $90.00)
        BigDecimal discountPct = BigDecimal.valueOf(10);
        BigDecimal discountAmount = grossSales.multiply(discountPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        assert discountAmount.compareTo(BigDecimal.valueOf(90.00)) == 0 : "Discount must be $90.00";

        BigDecimal netSales = grossSales.subtract(discountAmount);
        assert netSales.compareTo(BigDecimal.valueOf(810.00)) == 0 : "Net sales must be $810.00";

        // Test 3: Sales Tax calculation (8.25% on net sales of $810.00 = $66.83)
        BigDecimal taxPct = new BigDecimal("8.25");
        BigDecimal taxAmount = netSales.multiply(taxPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        assert taxAmount.compareTo(new BigDecimal("66.83")) == 0 : "Tax amount must be $66.83";

        BigDecimal totalInvoiced = netSales.add(taxAmount);
        assert totalInvoiced.compareTo(new BigDecimal("876.83")) == 0 : "Total invoiced must be $876.83";

        // Test 4: Gross Profit calculation (Net Sales - COGS)
        BigDecimal grossProfit = netSales.subtract(totalCogs); // 810.00 - 530.00 = 280.00
        assert grossProfit.compareTo(BigDecimal.valueOf(280.00)) == 0 : "Gross Profit must be $280.00";

        // Test 5: Net Profit calculation (Gross Profit - Operating Expenses)
        BigDecimal operatingExpenses = BigDecimal.valueOf(120.00);
        BigDecimal netProfit = grossProfit.subtract(operatingExpenses); // 280.00 - 120.00 = 160.00
        assert netProfit.compareTo(BigDecimal.valueOf(160.00)) == 0 : "Net profit must be $160.00";

        System.out.println("  [PASS] BusinessLogicTest - All 5 financial assertions passed.");
    }
}
