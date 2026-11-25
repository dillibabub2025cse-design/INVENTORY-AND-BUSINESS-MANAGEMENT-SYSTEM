package test;

import model.Product;

import java.math.BigDecimal;

/**
 * Unit tests verifying Product model constraints and low-stock indicators.
 */
public class ProductValidationTest {

    public static void runTests() {
        System.out.println("Running ProductValidationTest...");

        Product p = new Product(1, "890100", "Wireless Mouse", 1, "Desc",
                BigDecimal.valueOf(50.00), BigDecimal.valueOf(80.00), 10, 5, true);

        // Test 1: In stock condition
        assert "IN STOCK".equals(p.getStockStatus()) : "Status should be IN STOCK when current stock > min level";
        assert !p.isLowStock() : "isLowStock should be false";
        assert !p.isOutOfStock() : "isOutOfStock should be false";

        // Test 2: Low stock condition
        p.setCurrentStock(4);
        assert "LOW STOCK".equals(p.getStockStatus()) : "Status should be LOW STOCK when current stock <= min level";
        assert p.isLowStock() : "isLowStock should be true";

        // Test 3: Out of stock condition
        p.setCurrentStock(0);
        assert "OUT OF STOCK".equals(p.getStockStatus()) : "Status should be OUT OF STOCK when current stock is 0";
        assert p.isOutOfStock() : "isOutOfStock should be true";

        System.out.println("  [PASS] ProductValidationTest - All 3 assertions passed.");
    }
}

