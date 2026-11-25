package util;

import model.Purchase;
import model.PurchaseItem;
import model.Sale;
import model.SaleItem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility for formatting and exporting professional Sales Invoices and Purchase Orders.
 */
public class InvoiceGenerator {

    private static final String INVOICE_DIR = "invoices";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static {
        File dir = new File(INVOICE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Generates a printable Sales Invoice and saves to a text file.
     */
    public static String generateSalesInvoice(Sale sale, boolean saveToFile) {
        StringBuilder sb = new StringBuilder();
        String line = "=".repeat(68);
        String subline = "-".repeat(68);

        sb.append(line).append("\n");
        sb.append(centerString("INVENTORY & BUSINESS MANAGEMENT SYSTEM", 68)).append("\n");
        sb.append(centerString("COMMERCIAL SALES INVOICE", 68)).append("\n");
        sb.append(line).append("\n");

        sb.append(String.format("Invoice No : %-25s Date: %s\n", sale.getInvoiceNumber(), 
                sale.getSaleDate() != null ? DATE_FORMAT.format(sale.getSaleDate()) : DATE_FORMAT.format(new Date())));
        sb.append(String.format("Customer   : %-25s Cashier/Staff: %s\n", sale.getCustomerName(), sale.getUserName()));
        sb.append(String.format("Payment    : %-25s Status: %s\n", sale.getPaymentMethod(), sale.getStatus()));
        sb.append(subline).append("\n");

        sb.append(String.format("%-4s %-28s %6s %12s %14s\n", "#", "Item Description", "Qty", "Unit Price", "Total"));
        sb.append(subline).append("\n");

        int idx = 1;
        for (SaleItem item : sale.getItems()) {
            String name = item.getProductName();
            if (name.length() > 28) {
                name = name.substring(0, 25) + "...";
            }
            sb.append(String.format("%-4d %-28s %6d %12s %14s\n",
                    idx++,
                    name,
                    item.getQuantity(),
                    ConsoleFormatter.formatCurrency(item.getUnitPrice()),
                    ConsoleFormatter.formatCurrency(item.getSubtotal())));
        }

        sb.append(subline).append("\n");
        sb.append(String.format("%40s: %24s\n", "Subtotal", ConsoleFormatter.formatCurrency(sale.getSubtotal())));
        if (sale.getDiscountAmount() != null && sale.getDiscountAmount().signum() > 0) {
            sb.append(String.format("%40s: -%23s\n", "Discount", ConsoleFormatter.formatCurrency(sale.getDiscountAmount())));
        }
        if (sale.getTaxAmount() != null && sale.getTaxAmount().signum() > 0) {
            sb.append(String.format("%40s: +%23s\n", "Tax (VAT)", ConsoleFormatter.formatCurrency(sale.getTaxAmount())));
        }
        sb.append(line).append("\n");
        sb.append(String.format("%40s: %24s\n", "GRAND TOTAL", ConsoleFormatter.formatCurrency(sale.getTotalAmount())));
        sb.append(line).append("\n");
        sb.append(centerString("Thank you for your business!", 68)).append("\n");
        sb.append(line).append("\n");

        String invoiceContent = sb.toString();

        if (saveToFile) {
            saveInvoiceToFile(sale.getInvoiceNumber() + ".txt", invoiceContent);
        }

        return invoiceContent;
    }

    /**
     * Generates a printable Purchase Order and saves to a text file.
     */
    public static String generatePurchaseOrder(Purchase purchase, boolean saveToFile) {
        StringBuilder sb = new StringBuilder();
        String line = "=".repeat(68);
        String subline = "-".repeat(68);

        sb.append(line).append("\n");
        sb.append(centerString("INVENTORY & BUSINESS MANAGEMENT SYSTEM", 68)).append("\n");
        sb.append(centerString("PURCHASE ORDER RECEIPT", 68)).append("\n");
        sb.append(line).append("\n");

        sb.append(String.format("PO Number  : %-25s Date: %s\n", purchase.getPurchaseNumber(), 
                purchase.getPurchaseDate() != null ? DATE_FORMAT.format(purchase.getPurchaseDate()) : DATE_FORMAT.format(new Date())));
        sb.append(String.format("Supplier   : %-25s Created By: %s\n", purchase.getSupplierName(), purchase.getUserName()));
        sb.append(String.format("Status     : %-25s Notes: %s\n", purchase.getStatus(), 
                purchase.getNotes() != null ? purchase.getNotes() : "N/A"));
        sb.append(subline).append("\n");

        sb.append(String.format("%-4s %-28s %6s %12s %14s\n", "#", "Item Description", "Qty", "Unit Cost", "Subtotal"));
        sb.append(subline).append("\n");

        int idx = 1;
        for (PurchaseItem item : purchase.getItems()) {
            String name = item.getProductName();
            if (name != null && name.length() > 28) {
                name = name.substring(0, 25) + "...";
            }
            sb.append(String.format("%-4d %-28s %6d %12s %14s\n",
                    idx++,
                    name != null ? name : "Product #" + item.getProductId(),
                    item.getQuantity(),
                    ConsoleFormatter.formatCurrency(item.getUnitCost()),
                    ConsoleFormatter.formatCurrency(item.getSubtotal())));
        }

        sb.append(line).append("\n");
        sb.append(String.format("%40s: %24s\n", "TOTAL PURCHASE AMOUNT", ConsoleFormatter.formatCurrency(purchase.getTotalAmount())));
        sb.append(line).append("\n");

        String poContent = sb.toString();

        if (saveToFile) {
            saveInvoiceToFile(purchase.getPurchaseNumber() + ".txt", poContent);
        }

        return poContent;
    }

    private static void saveInvoiceToFile(String filename, String content) {
        File file = new File(INVOICE_DIR, filename);
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.print(content);
            ConsoleFormatter.printSuccess("Saved receipt file to: " + file.getAbsolutePath());
        } catch (IOException e) {
            ConsoleFormatter.printWarning("Could not write receipt to disk: " + e.getMessage());
        }
    }

    private static String centerString(String s, int width) {
        int pad = (width - s.length()) / 2;
        if (pad <= 0) return s;
        return " ".repeat(pad) + s;
    }
}

