package service;

import dao.CustomerDAO;
import dao.ProductDAO;
import dao.SalesDAO;
import exception.InsufficientStockException;
import exception.ValidationException;
import model.Customer;
import model.Product;
import model.Sale;
import model.SaleItem;
import util.InvoiceGenerator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Service managing Sales orders, calculations, discounts, taxes, and invoice issuance.
 */
public class SalesService {

    private final SalesDAO salesDAO;
    private final ProductDAO productDAO;
    private final CustomerDAO customerDAO;

    public SalesService() {
        this.salesDAO = new SalesDAO();
        this.productDAO = new ProductDAO();
        this.customerDAO = new CustomerDAO();
    }

    public SalesService(SalesDAO salesDAO, ProductDAO productDAO, CustomerDAO customerDAO) {
        this.salesDAO = salesDAO;
        this.productDAO = productDAO;
        this.customerDAO = customerDAO;
    }

    public List<Sale> getAllSales() {
        return salesDAO.findAll();
    }

    public Sale getSaleById(int id) {
        Sale s = salesDAO.findById(id);
        if (s == null) {
            throw new ValidationException("Sale Invoice #" + id + " does not exist.");
        }
        return s;
    }

    public Sale getSaleByInvoiceNumber(String invNumber) {
        Sale s = salesDAO.findByInvoiceNumber(invNumber);
        if (s == null) {
            throw new ValidationException("Sale Invoice '" + invNumber + "' does not exist.");
        }
        return s;
    }

    /**
     * Creates a sales transaction with stock verification, discounts, and tax computation.
     */
    public Sale createSale(Integer customerId, int userId, List<SaleItem> items, 
                           BigDecimal discountPercent, BigDecimal taxPercent, String paymentMethod) {
        if (items == null || items.isEmpty()) {
            throw new ValidationException("Sale must contain at least one item.");
        }

        String customerName = "Walk-in Customer";
        if (customerId != null) {
            Customer c = customerDAO.findById(customerId);
            if (c == null) {
                throw new ValidationException("Customer ID " + customerId + " does not exist.");
            }
            customerName = c.getName();
        }

        BigDecimal subtotal = BigDecimal.ZERO;

        // Verify stock and prepare items
        for (SaleItem item : items) {
            if (item.getQuantity() <= 0) {
                throw new ValidationException("Quantity must be positive for all sale items.");
            }
            Product product = productDAO.findById(item.getProductId());
            if (product == null) {
                throw new ValidationException("Product ID " + item.getProductId() + " not found.");
            }
            if (product.getCurrentStock() < item.getQuantity()) {
                throw new InsufficientStockException(
                    "Insufficient stock for '" + product.getName() + "'. Available: " + product.getCurrentStock() + ", Requested: " + item.getQuantity()
                );
            }
            item.setProductName(product.getName());
            item.setProductBarcode(product.getBarcode());
            if (item.getUnitPrice() == null) {
                item.setUnitPrice(product.getSellingPrice());
            }
            item.setCostPrice(product.getPurchasePrice());
            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            item.setSubtotal(lineTotal);
            subtotal = subtotal.add(lineTotal);
        }

        // Calculate discount
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (discountPercent != null && discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            discountAmount = subtotal.multiply(discountPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        BigDecimal discountedSubtotal = subtotal.subtract(discountAmount);
        if (discountedSubtotal.compareTo(BigDecimal.ZERO) < 0) {
            discountedSubtotal = BigDecimal.ZERO;
        }

        // Calculate tax
        BigDecimal taxAmount = BigDecimal.ZERO;
        if (taxPercent != null && taxPercent.compareTo(BigDecimal.ZERO) > 0) {
            taxAmount = discountedSubtotal.multiply(taxPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        BigDecimal grandTotal = discountedSubtotal.add(taxAmount);

        Sale sale = new Sale();
        sale.setInvoiceNumber(generateInvoiceNumber());
        sale.setCustomerId(customerId);
        sale.setCustomerName(customerName);
        sale.setUserId(userId);
        sale.setSubtotal(subtotal);
        sale.setDiscountAmount(discountAmount);
        sale.setTaxAmount(taxAmount);
        sale.setTotalAmount(grandTotal);
        sale.setPaymentMethod(paymentMethod != null ? paymentMethod.toUpperCase() : "CASH");
        sale.setStatus("COMPLETED");
        sale.setSaleDate(new Timestamp(System.currentTimeMillis()));
        sale.setItems(items);

        boolean success = salesDAO.createSale(sale);
        if (success) {
            InvoiceGenerator.generateSalesInvoice(sale, true);
        }
        return sale;
    }

    public boolean cancelSale(int saleId, int userId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new ValidationException("Cancellation reason is required.");
        }
        return salesDAO.cancelSale(saleId, userId, reason.trim());
    }

    public static String generateInvoiceNumber() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());
        int randomPart = 1000 + new Random().nextInt(9000);
        return "INV-" + dateStr + "-" + randomPart;
    }
}

