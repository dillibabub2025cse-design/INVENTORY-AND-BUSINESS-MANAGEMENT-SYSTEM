package service;

import dao.ProductDAO;
import dao.PurchaseDAO;
import dao.SupplierDAO;
import exception.ValidationException;
import model.Product;
import model.Purchase;
import model.PurchaseItem;
import model.Supplier;
import util.InvoiceGenerator;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Service managing Purchase Orders, line calculations, stock replenishment, and order cancellation.
 */
public class PurchaseService {

    private final PurchaseDAO purchaseDAO;
    private final SupplierDAO supplierDAO;
    private final ProductDAO productDAO;

    public PurchaseService() {
        this.purchaseDAO = new PurchaseDAO();
        this.supplierDAO = new SupplierDAO();
        this.productDAO = new ProductDAO();
    }

    public PurchaseService(PurchaseDAO purchaseDAO, SupplierDAO supplierDAO, ProductDAO productDAO) {
        this.purchaseDAO = purchaseDAO;
        this.supplierDAO = supplierDAO;
        this.productDAO = productDAO;
    }

    public List<Purchase> getAllPurchases() {
        return purchaseDAO.findAll();
    }

    public Purchase getPurchaseById(int id) {
        Purchase p = purchaseDAO.findById(id);
        if (p == null) {
            throw new ValidationException("Purchase Order #" + id + " does not exist.");
        }
        return p;
    }

    public Purchase createPurchaseOrder(int supplierId, int userId, List<PurchaseItem> items, String notes) {
        Supplier supplier = supplierDAO.findById(supplierId);
        if (supplier == null) {
            throw new ValidationException("Supplier ID " + supplierId + " does not exist.");
        }
        if (items == null || items.isEmpty()) {
            throw new ValidationException("Cannot create an empty purchase order. Add at least one product item.");
        }

        BigDecimal grandTotal = BigDecimal.ZERO;
        for (PurchaseItem item : items) {
            if (item.getQuantity() <= 0) {
                throw new ValidationException("Quantity must be positive for all purchase items.");
            }
            if (item.getUnitCost() == null || item.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Unit cost must be non-negative.");
            }
            Product prod = productDAO.findById(item.getProductId());
            if (prod == null) {
                throw new ValidationException("Product ID " + item.getProductId() + " does not exist.");
            }
            item.setProductName(prod.getName());
            item.setProductBarcode(prod.getBarcode());
            BigDecimal itemTotal = item.getUnitCost().multiply(BigDecimal.valueOf(item.getQuantity()));
            item.setSubtotal(itemTotal);
            grandTotal = grandTotal.add(itemTotal);
        }

        Purchase purchase = new Purchase();
        purchase.setPurchaseNumber(generatePoNumber());
        purchase.setSupplierId(supplierId);
        purchase.setSupplierName(supplier.getCompanyName());
        purchase.setUserId(userId);
        purchase.setTotalAmount(grandTotal);
        purchase.setStatus("COMPLETED");
        purchase.setNotes(notes);
        purchase.setPurchaseDate(new Timestamp(System.currentTimeMillis()));
        purchase.setItems(items);

        boolean success = purchaseDAO.createPurchase(purchase);
        if (success) {
            // Generate and save printable invoice receipt
            InvoiceGenerator.generatePurchaseOrder(purchase, true);
        }
        return purchase;
    }

    public boolean cancelPurchaseOrder(int purchaseId, int userId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new ValidationException("Cancellation reason must be provided.");
        }
        return purchaseDAO.cancelPurchase(purchaseId, userId, reason.trim());
    }

    public static String generatePoNumber() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());
        int randomPart = 1000 + new Random().nextInt(9000);
        return "PO-" + dateStr + "-" + randomPart;
    }
}

