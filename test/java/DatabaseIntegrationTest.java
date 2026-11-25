package test;

import config.DBConnection;
import dao.*;
import exception.InsufficientStockException;
import exception.ValidationException;
import model.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Real database integration tests testing:
 * - Product, Category, Supplier, and Customer CRUD
 * - Purchase Order creation, stock increment, and safe cancellation
 * - Sales Order creation, multi-item stock decrement, and stock restoration
 * - Prevention of double cancellation (idempotency guard)
 * - Atomic database rollback on stock violation
 *
 * Guarantees zero deletion or corruption of existing business records.
 */
public class DatabaseIntegrationTest {

    public static void runTests() {
        System.out.println("Running DatabaseIntegrationTest against active MySQL database...");

        if (!DBConnection.testConnection()) {
            System.err.println("  [SKIP] Database connection not available. Skipping live database integration tests.");
            return;
        }

        ProductDAO productDAO = new ProductDAO();
        CategoryDAO categoryDAO = new CategoryDAO();
        SupplierDAO supplierDAO = new SupplierDAO();
        CustomerDAO customerDAO = new CustomerDAO();
        PurchaseDAO purchaseDAO = new PurchaseDAO();
        SalesDAO salesDAO = new SalesDAO();
        InventoryDAO inventoryDAO = new InventoryDAO();

        int createdCategoryId = -1;
        int createdSupplierId = -1;
        int createdCustomerId = -1;
        int testProductId1 = -1;
        int testProductId2 = -1;
        int testPurchaseId = -1;
        int testSaleId = -1;

        try {
            // -------------------------------------------------------------
            // 1. Test Category CRUD
            // -------------------------------------------------------------
            Category testCat = new Category(0, "TestCategory_" + System.currentTimeMillis(), "Integration test category");
            assert categoryDAO.create(testCat) : "Category creation failed";
            createdCategoryId = testCat.getId();
            assert createdCategoryId > 0 : "Category ID must be generated";
            Category fetchedCat = categoryDAO.findById(createdCategoryId);
            assert fetchedCat != null && fetchedCat.getName().equals(testCat.getName()) : "Fetched category mismatch";

            // -------------------------------------------------------------
            // 2. Test Supplier CRUD
            // -------------------------------------------------------------
            Supplier testSup = new Supplier(0, "TestVendor_" + System.currentTimeMillis(), "John Doe",
                    "vendor_" + System.currentTimeMillis() + "@test.com", "+1-555-9988", "123 Test St", true);
            assert supplierDAO.create(testSup) : "Supplier creation failed";
            createdSupplierId = testSup.getId();
            assert createdSupplierId > 0 : "Supplier ID must be generated";

            // -------------------------------------------------------------
            // 3. Test Customer CRUD
            // -------------------------------------------------------------
            Customer testCust = new Customer(0, "TestClient_" + System.currentTimeMillis(),
                    "client_" + System.currentTimeMillis() + "@test.com", "+1-555-7766", "456 Client Ave");
            assert customerDAO.create(testCust) : "Customer creation failed";
            createdCustomerId = testCust.getId();
            assert createdCustomerId > 0 : "Customer ID must be generated";

            // -------------------------------------------------------------
            // 4. Test Product CRUD
            // -------------------------------------------------------------
            String barcode1 = "TEST-BARCODE-A-" + System.currentTimeMillis();
            Product p1 = new Product(0, barcode1, "Test Gadget Alpha", createdCategoryId, "Test Description",
                    new BigDecimal("40.00"), new BigDecimal("75.00"), 20, 5, true);
            assert productDAO.create(p1) : "Product 1 creation failed";
            testProductId1 = p1.getId();

            String barcode2 = "TEST-BARCODE-B-" + System.currentTimeMillis();
            Product p2 = new Product(0, barcode2, "Test Gadget Beta", createdCategoryId, "Test Description",
                    new BigDecimal("60.00"), new BigDecimal("110.00"), 15, 3, true);
            assert productDAO.create(p2) : "Product 2 creation failed";
            testProductId2 = p2.getId();

            Product fetchedP1 = productDAO.findById(testProductId1);
            assert fetchedP1 != null && fetchedP1.getCurrentStock() == 20 : "Initial stock for P1 must be 20";

            // -------------------------------------------------------------
            // 5. Test Purchase Order & Stock Replenishment
            // -------------------------------------------------------------
            Purchase po = new Purchase();
            po.setPurchaseNumber("TEST-PO-" + System.currentTimeMillis());
            po.setSupplierId(createdSupplierId);
            po.setUserId(1); // Admin user
            po.setStatus("COMPLETED");
            po.setNotes("Integration test PO");
            po.setPurchaseDate(new java.sql.Timestamp(System.currentTimeMillis()));

            PurchaseItem poItem1 = new PurchaseItem(testProductId1, p1.getName(), 10, new BigDecimal("40.00"));
            po.addItem(poItem1);
            po.setTotalAmount(poItem1.getSubtotal());

            assert purchaseDAO.createPurchase(po) : "Purchase creation failed";
            testPurchaseId = po.getId();

            // Verify stock incremented by 10 (20 -> 30)
            Product p1AfterPurchase = productDAO.findById(testProductId1);
            assert p1AfterPurchase.getCurrentStock() == 30 : "Stock after purchase must be 30, but was " + p1AfterPurchase.getCurrentStock();

            // Test Purchase Cancellation (safe stock reversal: 30 -> 20)
            assert purchaseDAO.cancelPurchase(testPurchaseId, 1, "Integration test cancellation") : "Purchase cancellation failed";
            Product p1AfterPoCancel = productDAO.findById(testProductId1);
            assert p1AfterPoCancel.getCurrentStock() == 20 : "Stock after PO cancellation must be 20, but was " + p1AfterPoCancel.getCurrentStock();

            // Verify Double-Cancellation Guard on Purchase
            boolean doublePoCancelCaught = false;
            try {
                purchaseDAO.cancelPurchase(testPurchaseId, 1, "Attempt duplicate cancel");
            } catch (ValidationException ve) {
                doublePoCancelCaught = true;
            }
            assert doublePoCancelCaught : "Double purchase cancellation must be rejected";
            assert productDAO.findById(testProductId1).getCurrentStock() == 20 : "Stock must remain unchanged after rejected double cancellation";

            // -------------------------------------------------------------
            // 6. Test Sales Transaction with Multiple Products & Stock Decrement
            // -------------------------------------------------------------
            Sale sale = new Sale();
            sale.setInvoiceNumber("TEST-INV-" + System.currentTimeMillis());
            sale.setCustomerId(createdCustomerId);
            sale.setUserId(1);
            sale.setPaymentMethod("CARD");
            sale.setStatus("COMPLETED");
            sale.setSaleDate(new java.sql.Timestamp(System.currentTimeMillis()));

            SaleItem si1 = new SaleItem(testProductId1, p1.getName(), 4, new BigDecimal("75.00"), new BigDecimal("40.00")); // 4 * 75 = 300
            SaleItem si2 = new SaleItem(testProductId2, p2.getName(), 3, new BigDecimal("110.00"), new BigDecimal("60.00")); // 3 * 110 = 330
            sale.addItem(si1);
            sale.addItem(si2);

            BigDecimal subtotal = new BigDecimal("630.00");
            BigDecimal discount = new BigDecimal("30.00");
            BigDecimal tax = new BigDecimal("48.00");
            BigDecimal total = new BigDecimal("648.00");

            sale.setSubtotal(subtotal);
            sale.setDiscountAmount(discount);
            sale.setTaxAmount(tax);
            sale.setTotalAmount(total);

            assert salesDAO.createSale(sale) : "Sale transaction creation failed";
            testSaleId = sale.getId();

            // Verify stock decreased: P1: 20 - 4 = 16, P2: 15 - 3 = 12
            assert productDAO.findById(testProductId1).getCurrentStock() == 16 : "Stock for P1 after sale must be 16";
            assert productDAO.findById(testProductId2).getCurrentStock() == 12 : "Stock for P2 after sale must be 12";

            // Verify Sale Cancellation (Stock restoration: P1 -> 20, P2 -> 15)
            assert salesDAO.cancelSale(testSaleId, 1, "Integration test return") : "Sale cancellation failed";
            assert productDAO.findById(testProductId1).getCurrentStock() == 20 : "Stock for P1 after cancellation must be restored to 20";
            assert productDAO.findById(testProductId2).getCurrentStock() == 15 : "Stock for P2 after cancellation must be restored to 15";

            // Verify Double-Cancellation Guard on Sale
            boolean doubleSaleCancelCaught = false;
            try {
                salesDAO.cancelSale(testSaleId, 1, "Attempt duplicate cancel");
            } catch (ValidationException ve) {
                doubleSaleCancelCaught = true;
            }
            assert doubleSaleCancelCaught : "Double sale cancellation must be rejected";
            assert productDAO.findById(testProductId1).getCurrentStock() == 20 : "Stock for P1 must remain 20 after rejected duplicate cancel";

            // -------------------------------------------------------------
            // 7. Test Atomic Database Rollback on Insufficient Stock
            // -------------------------------------------------------------
            Sale failingSale = new Sale();
            failingSale.setInvoiceNumber("TEST-FAIL-INV-" + System.currentTimeMillis());
            failingSale.setCustomerId(createdCustomerId);
            failingSale.setUserId(1);
            failingSale.setPaymentMethod("CASH");
            failingSale.setStatus("COMPLETED");
            failingSale.setSaleDate(new java.sql.Timestamp(System.currentTimeMillis()));

            // Valid quantity on P1 (2 units), but impossible quantity on P2 (999 units > 15)
            SaleItem validItem = new SaleItem(testProductId1, p1.getName(), 2, new BigDecimal("75.00"), new BigDecimal("40.00"));
            SaleItem impossibleItem = new SaleItem(testProductId2, p2.getName(), 999, new BigDecimal("110.00"), new BigDecimal("60.00"));
            failingSale.addItem(validItem);
            failingSale.addItem(impossibleItem);
            failingSale.setSubtotal(new BigDecimal("1000.00"));
            failingSale.setDiscountAmount(BigDecimal.ZERO);
            failingSale.setTaxAmount(BigDecimal.ZERO);
            failingSale.setTotalAmount(new BigDecimal("1000.00"));

            boolean rollbackTriggered = false;
            try {
                salesDAO.createSale(failingSale);
            } catch (InsufficientStockException ise) {
                rollbackTriggered = true;
            }
            assert rollbackTriggered : "Sale with insufficient stock on item 2 must throw InsufficientStockException";

            // Verify atomic rollback: P1 stock must NOT have decreased by 2, must remain 20!
            assert productDAO.findById(testProductId1).getCurrentStock() == 20 : "P1 stock must remain 20 due to atomic rollback!";
            assert productDAO.findById(testProductId2).getCurrentStock() == 15 : "P2 stock must remain 15!";

            System.out.println("  [PASS] DatabaseIntegrationTest - All 7 database integration assertions passed successfully!");
        } finally {
            // Clean up test data safely to preserve all existing business records
            cleanupTestData(testSaleId, testPurchaseId, testProductId1, testProductId2, createdCustomerId, createdSupplierId, createdCategoryId);
        }
    }

    private static void cleanupTestData(int saleId, int poId, int p1, int p2, int custId, int supId, int catId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (saleId > 0) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM sale_items WHERE sale_id = ?")) {
                    ps.setInt(1, saleId); ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM sales WHERE id = ?")) {
                    ps.setInt(1, saleId); ps.executeUpdate();
                }
            }
            if (poId > 0) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM purchase_items WHERE purchase_id = ?")) {
                    ps.setInt(1, poId); ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM purchases WHERE id = ?")) {
                    ps.setInt(1, poId); ps.executeUpdate();
                }
            }
            if (p1 > 0) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM stock_movements WHERE product_id = ?")) {
                    ps.setInt(1, p1); ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM products WHERE id = ?")) {
                    ps.setInt(1, p1); ps.executeUpdate();
                }
            }
            if (p2 > 0) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM stock_movements WHERE product_id = ?")) {
                    ps.setInt(1, p2); ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM products WHERE id = ?")) {
                    ps.setInt(1, p2); ps.executeUpdate();
                }
            }
            if (custId > 0) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM customers WHERE id = ?")) {
                    ps.setInt(1, custId); ps.executeUpdate();
                }
            }
            if (supId > 0) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM suppliers WHERE id = ?")) {
                    ps.setInt(1, supId); ps.executeUpdate();
                }
            }
            if (catId > 0) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM categories WHERE id = ?")) {
                    ps.setInt(1, catId); ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Notice during test cleanup: " + e.getMessage());
        }
    }
}

