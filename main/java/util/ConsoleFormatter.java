package util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility for rendering ANSI/ASCII UI banners, tables, borders, and formatted text.
 */
public class ConsoleFormatter {

    private static final DecimalFormat CURRENCY_FORMAT;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setCurrencySymbol("₹");
        CURRENCY_FORMAT = new DecimalFormat("₹#,##0.00", symbols);
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "₹0.00";
        }
        synchronized (CURRENCY_FORMAT) {
            return CURRENCY_FORMAT.format(amount);
        }
    }

    public static String formatCurrency(double amount) {
        synchronized (CURRENCY_FORMAT) {
            return CURRENCY_FORMAT.format(amount);
        }
    }

    public static String formatDate(Date date) {
        if (date == null) return "N/A";
        return DATE_FORMAT.format(date);
    }

    public static String formatDateTime(Date date) {
        if (date == null) return "N/A";
        return DATETIME_FORMAT.format(date);
    }

    public static void printBanner(String title) {
        int width = 76;
        String border = "=".repeat(width);
        System.out.println();
        System.out.println("+" + border + "+");
        int padding = (width - title.length()) / 2;
        String paddedTitle = " ".repeat(Math.max(0, padding)) + title;
        paddedTitle = paddedTitle + " ".repeat(Math.max(0, width - paddedTitle.length()));
        System.out.println("|" + paddedTitle + "|");
        System.out.println("+" + border + "+");
    }

    public static void printSectionHeader(String sectionTitle) {
        System.out.println();
        System.out.println(">>> " + sectionTitle.toUpperCase() + " <<<");
        System.out.println("-".repeat(Math.max(30, sectionTitle.length() + 8)));
    }

    public static void printSuccess(String message) {
        System.out.println("[SUCCESS] " + message);
    }

    public static void printError(String message) {
        System.out.println("[ERROR] " + message);
    }

    public static void printWarning(String message) {
        System.out.println("[WARNING] " + message);
    }

    public static void printInfo(String message) {
        System.out.println("[INFO] " + message);
    }

    public static void printDivider(int length) {
        System.out.println("-".repeat(length));
    }

    public static void printDoubleDivider(int length) {
        System.out.println("=".repeat(length));
    }

    /**
     * Prints a 2-column key-value pair summary row.
     */
    public static void printMetric(String label, Object value, int labelWidth) {
        String formattedLabel = String.format("%-" + labelWidth + "s", label + ":");
        System.out.println("  " + formattedLabel + " " + value);
    }
}

