package main;

import config.DBConnection;
import exception.AppException;
import exception.AuthenticationException;
import model.*;
import service.*;
import util.ConsoleFormatter;
import util.InputValidator;
import util.InvoiceGenerator;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Main application entry point for the Inventory and Business Management
 * System.
 * Provides a professional, menu-driven CLI experience with RBAC protection.
 */
public class Main {

    private static final AuthService authService = new AuthService();
    private static final ProductService productService = new ProductService();
    private static final CategoryService categoryService = new CategoryService();
    private static final SupplierService supplierService = new SupplierService();
    private static final CustomerService customerService = new CustomerService();
    private static final PurchaseService purchaseService = new PurchaseService();
    private static final SalesService salesService = new SalesService();
    private static final InventoryService inventoryService = new InventoryService();
    private static final EmployeeService employeeService = new EmployeeService();
    private static final ExpenseService expenseService = new ExpenseService();
    private static final ReportService reportService = new ReportService();
    private static final UserService userService = new UserService();

    public static void main(String[] args) {
        printSystemStartupBanner();
        ensureDatabaseReady();

        boolean running = true;
        while (running) {
            if (!authService.isLoggedIn()) {
                boolean loggedIn = handleLoginScreen();
                if (!loggedIn) {
                    break; // Exit program
                }
            } else {
                handleMainMenu();
            }
        }

        ConsoleFormatter.printBanner("THANK YOU FOR USING INVENTORY & BUSINESS MANAGEMENT SYSTEM");
        System.out.println("System terminated cleanly. Goodbye!");
    }

    private static void printSystemStartupBanner() {
        System.out.println("\n" +
                " ==========================================================================\n" +
                " |                                                                        |\n" +
                " |               INVENTORY AND BUSINESS MANAGEMENT SYSTEM                 |\n" +
                " |            Enterprise Java & MySQL JDBC Console Application            |\n" +
                " |                                                                        |\n" +
                " ==========================================================================");
    }

    private static void ensureDatabaseReady() {
        System.out.println("\n[INIT] Checking MySQL database connectivity...");
        boolean connected = DBConnection.testConnection();
        if (!connected) {
            ConsoleFormatter.printWarning("Unable to connect to MySQL database with default settings ("
                    + DBConnection.getUser() + "@" + DBConnection.getUrl() + ")");
            System.out.println("Would you like to configure custom database credentials now?");
            if (InputValidator.readConfirmation("Configure database credentials?")) {
                promptDatabaseConfig();
            } else {
                ConsoleFormatter
                        .printError("Cannot proceed without a valid MySQL connection. Please check MySQL service.");
            }
        } else {
            ConsoleFormatter.printSuccess("Connected to MySQL database successfully.");
            DBConnection.ensureDatabaseInitialized();
        }
    }

    private static void promptDatabaseConfig() {
        ConsoleFormatter.printSectionHeader("Database Configuration");
        String host = InputValidator.readOptionalString("MySQL Host", "localhost");
        int port = InputValidator.readInt("MySQL Port (default 3306): ", 1, 65535);
        String dbName = InputValidator.readOptionalString("Database Name", "inventory_business_db");
        String dbUser = InputValidator.readString("Database Username: ");
        String dbPass = InputValidator.readOptionalString("Database Password", "");

        String newUrl = String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true",
                host, port, dbName);
        DBConnection.setCredentials(newUrl, dbUser, dbPass);

        if (DBConnection.testConnection()) {
            ConsoleFormatter.printSuccess("Database connection established successfully!");
            DBConnection.ensureDatabaseInitialized();
        } else {
            ConsoleFormatter
                    .printError("Connection failed with provided credentials. Please check your MySQL service.");
        }
    }

    // =========================================================================
    // 1. LOGIN & AUTHENTICATION
    // =========================================================================

    private static boolean handleLoginScreen() {
        ConsoleFormatter.printSectionHeader("User Authentication");
        System.out.println("  [1] Login with Credentials");
        System.out.println("  [2] Configure Database Connection");
        System.out.println("  [0] Exit Application");
        System.out.println("  (Default Admin: admin / admin123 | Default Staff: staff / staff123)");

        int choice = InputValidator.readInt("\nSelect an option [0-2]: ", 0, 2);
        if (choice == 0) {
            return false;
        } else if (choice == 2) {
            promptDatabaseConfig();
            return false;
        }

        String username = InputValidator.readString("Enter Username: ");
        String password = InputValidator.readString("Enter Password: ");

        try {
            User user = authService.login(username, password);
            ConsoleFormatter.printSuccess("Welcome, " + user.getFullName() + "! [Role: " + user.getRoleName() + "]");
            return true;
        } catch (AppException e) {
            ConsoleFormatter.printError(e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // MAIN NAVIGATION MENU
    // =========================================================================

    private static void handleMainMenu() {
        User user = authService.getCurrentUser();
        ConsoleFormatter.printBanner("MAIN NAVIGATION MENU [" + user.getRoleName() + ": " + user.getFullName() + "]");

        System.out.println("  1. Executive Dashboard");
        System.out.println("  2. Product Management");
        System.out.println("  3. Category Management");
        System.out.println("  4. Supplier Management");
        System.out.println("  5. Customer Management");
        System.out.println("  6. Purchase Management");
        System.out.println("  7. Sales Management");
        System.out.println("  8. Inventory Management");
        System.out.println("  9. Employee Management " + (user.isAdmin() ? "" : "(Admin Restricted)"));
        System.out.println(" 10. Expense Management");
        System.out.println(" 11. Business Reports");
        System.out.println(" 12. User Management " + (user.isAdmin() ? "" : "(Admin Restricted)"));
        System.out.println(" 13. Change My Password");
        System.out.println(" 14. Logout");
        System.out.println("  0. Exit System");

        int choice = InputValidator.readInt("\nEnter menu choice [0-14]: ", 0, 14);
        try {
            switch (choice) {
                case 1:
                    reportService.printDashboard();
                    InputValidator.pressEnterToContinue();
                    break;
                case 2:
                    handleProductMenu();
                    break;
                case 3:
                    handleCategoryMenu();
                    break;
                case 4:
                    handleSupplierMenu();
                    break;
                case 5:
                    handleCustomerMenu();
                    break;
                case 6:
                    handlePurchaseMenu();
                    break;
                case 7:
                    handleSalesMenu();
                    break;
                case 8:
                    handleInventoryMenu();
                    break;
                case 9:
                    if (user.isAdmin()) {
                        handleEmployeeMenu();
                    } else {
                        ConsoleFormatter
                                .printError("Access Denied: Employee Management requires Administrator privileges.");
                        InputValidator.pressEnterToContinue();
                    }
                    break;
                case 10:
                    handleExpenseMenu();
                    break;
                case 11:
                    handleReportsMenu();
                    break;
                case 12:
                    if (user.isAdmin()) {
                        handleUserManagementMenu();
                    } else {
                        ConsoleFormatter
                                .printError("Access Denied: User Management requires Administrator privileges.");
                        InputValidator.pressEnterToContinue();
                    }
                    break;
                case 13:
                    handleChangePassword();
                    break;
                case 14:
                    authService.logout();
                    ConsoleFormatter.printSuccess("Logged out successfully.");
                    break;
                case 0:
                    if (InputValidator.readConfirmation("Are you sure you want to exit?")) {
                        authService.logout();
                        System.exit(0);
                    }
                    break;
            }
        } catch (AppException e) {
            ConsoleFormatter.printError(e.getMessage());
            InputValidator.pressEnterToContinue();
        } catch (Exception e) {
            ConsoleFormatter.printError("Unexpected system error: " + e.getMessage());
            InputValidator.pressEnterToContinue();
        }
    }

    // =========================================================================
    // 2. PRODUCT MANAGEMENT
    // =========================================================================

    private static void handleProductMenu() {
        while (true) {
            ConsoleFormatter.printSectionHeader("Product Management");
            System.out.println("  1. View All Products");
            System.out.println("  2. Add New Product");
            System.out.println("  3. Search Products (ID, Barcode, Name, Category)");
            System.out.println("  4. Update Product Details");
            System.out.println("  5. Delete Product");
            System.out.println("  6. View Low-Stock Alerts");
            System.out.println("  7. View Out-of-Stock Alerts");
            System.out.println("  0. Return to Main Menu");

            int choice = InputValidator.readInt("\nChoice [0-7]: ", 0, 7);
            if (choice == 0)
                break;

            try {
                switch (choice) {
                    case 1:
                        displayProductsTable(productService.getAllProducts());
                        InputValidator.pressEnterToContinue();
                        break;
                    case 2:
                        addNewProduct();
                        break;
                    case 3:
                        String query = InputValidator.readString("Enter search keyword: ");
                        displayProductsTable(productService.searchProducts(query));
                        InputValidator.pressEnterToContinue();
                        break;
                    case 4:
                        updateProductDetails();
                        break;
                    case 5:
                        deleteProduct();
                        break;
                    case 6:
                        ConsoleFormatter.printSectionHeader("Low-Stock Alert Products");
                        displayProductsTable(productService.getLowStockProducts());
                        InputValidator.pressEnterToContinue();
                        break;
                    case 7:
                        ConsoleFormatter.printSectionHeader("Out-of-Stock Alert Products");
                        displayProductsTable(productService.getOutOfStockProducts());
                        InputValidator.pressEnterToContinue();
                        break;
                }
            } catch (AppException e) {
                ConsoleFormatter.printError(e.getMessage());
                InputValidator.pressEnterToContinue();
            }
        }
    }

    private static void displayProductsTable(List<Product> products) {
        if (products.isEmpty()) {
            System.out.println("  No products found matching criteria.");
            return;
        }
        System.out.printf("\n  %-5s %-14s %-30s %-18s %10s %10s %6s %6s %-12s\n",
                "ID", "Barcode", "Product Name", "Category", "Cost", "Price", "Stock", "Min", "Status");
        ConsoleFormatter.printDivider(115);
        for (Product p : products) {
            String name = p.getName().length() > 30 ? p.getName().substring(0, 27) + "..." : p.getName();
            String cat = p.getCategoryName().length() > 18 ? p.getCategoryName().substring(0, 15) + "..."
                    : p.getCategoryName();
            System.out.printf("  %-5d %-14s %-30s %-18s %10s %10s %6d %6d %-12s\n",
                    p.getId(),
                    p.getBarcode(),
                    name,
                    cat,
                    ConsoleFormatter.formatCurrency(p.getPurchasePrice()),
                    ConsoleFormatter.formatCurrency(p.getSellingPrice()),
                    p.getCurrentStock(),
                    p.getMinStockLevel(),
                    "[" + p.getStockStatus() + "]");
        }
        ConsoleFormatter.printDivider(115);
    }

    private static void addNewProduct() {
        ConsoleFormatter.printSectionHeader("Add New Product");
        List<Category> categories = categoryService.getAllCategories();
        if (categories.isEmpty()) {
            ConsoleFormatter.printWarning("Please add at least one category before adding products.");
            return;
        }

        System.out.println("Available Categories:");
        for (Category c : categories) {
            System.out.printf("  [%d] %s\n", c.getId(), c.getName());
        }

        int catId = InputValidator.readInt("Select Category ID: ", 1, Integer.MAX_VALUE);
        String barcode = InputValidator.readString("Enter Barcode / SKU: ");
        String name = InputValidator.readString("Enter Product Name: ");
        String desc = InputValidator.readOptionalString("Enter Description", "");
        BigDecimal purchaseCost = InputValidator.readBigDecimal("Purchase Price ($): ", BigDecimal.ZERO, true);
        BigDecimal sellingPrice = InputValidator.readBigDecimal("Selling Price ($): ", purchaseCost, true);
        int initialStock = InputValidator.readNonNegativeInt("Initial Stock Quantity: ");
        int minStock = InputValidator.readNonNegativeInt("Minimum Alert Stock Level: ");

        Product p = new Product(0, barcode, name, catId, desc, purchaseCost, sellingPrice, initialStock, minStock,
                true);
        if (productService.addProduct(p)) {
            ConsoleFormatter.printSuccess("Product added successfully with ID: " + p.getId());
        }
        InputValidator.pressEnterToContinue();
    }

    private static void updateProductDetails() {
        ConsoleFormatter.printSectionHeader("Update Product");
        int id = InputValidator.readPositiveInt("Enter Product ID to update: ");
        Product p = productService.getProductById(id);

        System.out.println("Editing Product: " + p.getName() + " (" + p.getBarcode() + ")");
        String newName = InputValidator.readOptionalString("New Name", p.getName());
        String newBarcode = InputValidator.readOptionalString("New Barcode", p.getBarcode());
        BigDecimal newCost = InputValidator.readBigDecimal("New Purchase Cost ($) [" + p.getPurchasePrice() + "]: ",
                BigDecimal.ZERO, true);
        BigDecimal newPrice = InputValidator.readBigDecimal("New Selling Price ($) [" + p.getSellingPrice() + "]: ",
                newCost, true);
        int newMinStock = InputValidator.readInt("New Min Stock Alert [" + p.getMinStockLevel() + "]: ", 0, 10000);
        boolean active = InputValidator.readConfirmation("Is Product Active? (current: " + p.isActive() + ")");

        p.setName(newName);
        p.setBarcode(newBarcode);
        p.setPurchasePrice(newCost);
        p.setSellingPrice(newPrice);
        p.setMinStockLevel(newMinStock);
        p.setActive(active);

        if (productService.updateProduct(p)) {
            ConsoleFormatter.printSuccess("Product updated successfully!");
        }
        InputValidator.pressEnterToContinue();
    }

    private static void deleteProduct() {
        ConsoleFormatter.printSectionHeader("Delete Product");
        int id = InputValidator.readPositiveInt("Enter Product ID to delete: ");
        Product p = productService.getProductById(id);
        if (InputValidator.readConfirmation("Are you sure you want to permanently delete '" + p.getName() + "'?")) {
            if (productService.deleteProduct(id)) {
                ConsoleFormatter.printSuccess("Product deleted successfully.");
            }
        } else {
            System.out.println("Deletion cancelled.");
        }
        InputValidator.pressEnterToContinue();
    }

    // =========================================================================
    // 3. CATEGORY MANAGEMENT
    // =========================================================================

    private static void handleCategoryMenu() {
        while (true) {
            ConsoleFormatter.printSectionHeader("Category Management");
            System.out.println("  1. View All Categories");
            System.out.println("  2. Add New Category");
            System.out.println("  3. Update Category");
            System.out.println("  4. Delete Category");
            System.out.println("  0. Back to Main Menu");

            int choice = InputValidator.readInt("\nChoice [0-4]: ", 0, 4);
            if (choice == 0)
                break;

            try {
                switch (choice) {
                    case 1:
                        List<Category> list = categoryService.getAllCategories();
                        System.out.printf("\n  %-5s %-25s %-45s\n", "ID", "Category Name", "Description");
                        ConsoleFormatter.printDivider(78);
                        for (Category c : list) {
                            System.out.printf("  %-5d %-25s %-45s\n", c.getId(), c.getName(), c.getDescription());
                        }
                        ConsoleFormatter.printDivider(78);
                        InputValidator.pressEnterToContinue();
                        break;
                    case 2:
                        String name = InputValidator.readString("Enter Category Name: ");
                        String desc = InputValidator.readOptionalString("Enter Description", "");
                        if (categoryService.addCategory(name, desc)) {
                            ConsoleFormatter.printSuccess("Category created successfully.");
                        }
                        InputValidator.pressEnterToContinue();
                        break;
                    case 3:
                        int id = InputValidator.readPositiveInt("Enter Category ID to edit: ");
                        Category cat = categoryService.getCategoryById(id);
                        String editName = InputValidator.readOptionalString("New Name", cat.getName());
                        String editDesc = InputValidator.readOptionalString("New Description", cat.getDescription());
                        if (categoryService.updateCategory(id, editName, editDesc)) {
                            ConsoleFormatter.printSuccess("Category updated successfully.");
                        }
                        InputValidator.pressEnterToContinue();
                        break;
                    case 4:
                        int delId = InputValidator.readPositiveInt("Enter Category ID to delete: ");
                        if (InputValidator.readConfirmation("Delete Category ID " + delId + "?")) {
                            if (categoryService.deleteCategory(delId)) {
                                ConsoleFormatter.printSuccess("Category deleted successfully.");
                            }
                        }
                        InputValidator.pressEnterToContinue();
                        break;
                }
            } catch (AppException e) {
                ConsoleFormatter.printError(e.getMessage());
                InputValidator.pressEnterToContinue();
            }
        }
    }

    // =========================================================================
    // 4. SUPPLIER MANAGEMENT
    // =========================================================================

    private static void handleSupplierMenu() {
        while (true) {
            ConsoleFormatter.printSectionHeader("Supplier Management");
            System.out.println("  1. View All Suppliers");
            System.out.println("  2. Add New Supplier");
            System.out.println("  3. Search Suppliers");
            System.out.println("  4. Update Supplier");
            System.out.println("  5. Delete Supplier");
            System.out.println("  0. Back to Main Menu");

            int choice = InputValidator.readInt("\nChoice [0-5]: ", 0, 5);
            if (choice == 0)
                break;

            try {
                switch (choice) {
                    case 1:
                        displaySuppliers(supplierService.getAllSuppliers());
                        InputValidator.pressEnterToContinue();
                        break;
                    case 2:
                        String compName = InputValidator.readString("Company Name: ");
                        String contact = InputValidator.readString("Contact Person: ");
                        String email = InputValidator.readOptionalEmail("Email: ", "");
                        String phone = InputValidator.readPhone("Phone Number: ");
                        String addr = InputValidator.readOptionalString("Physical Address", "");
                        Supplier s = new Supplier(0, compName, contact, email, phone, addr, true);
                        if (supplierService.addSupplier(s)) {
                            ConsoleFormatter.printSuccess("Supplier added successfully with ID " + s.getId());
                        }
                        InputValidator.pressEnterToContinue();
                        break;
                    case 3:
                        String query = InputValidator.readString("Search Supplier (name/phone): ");
                        displaySuppliers(supplierService.searchSuppliers(query));
                        InputValidator.pressEnterToContinue();
                        break;
                    case 4:
                        int supId = InputValidator.readPositiveInt("Supplier ID to update: ");
                        Supplier exist = supplierService.getSupplierById(supId);
                        exist.setCompanyName(InputValidator.readOptionalString("Company Name", exist.getCompanyName()));
                        exist.setContactName(
                                InputValidator.readOptionalString("Contact Person", exist.getContactName()));
                        exist.setEmail(InputValidator.readOptionalEmail("Email", exist.getEmail()));
                        exist.setPhone(InputValidator.readOptionalString("Phone", exist.getPhone()));
                        exist.setAddress(InputValidator.readOptionalString("Address", exist.getAddress()));
                        exist.setActive(InputValidator.readConfirmation("Is Active?"));
                        if (supplierService.updateSupplier(exist)) {
                            ConsoleFormatter.printSuccess("Supplier updated successfully.");
                        }
                        InputValidator.pressEnterToContinue();
                        break;
                    case 5:
                        int delId = InputValidator.readPositiveInt("Supplier ID to delete: ");
                        if (InputValidator.readConfirmation("Delete supplier #" + delId + "?")) {
                            if (supplierService.deleteSupplier(delId)) {
                                ConsoleFormatter.printSuccess("Supplier deleted successfully.");
                            }
                        }
                        InputValidator.pressEnterToContinue();
                        break;
                }
            } catch (AppException e) {
                ConsoleFormatter.printError(e.getMessage());
                InputValidator.pressEnterToContinue();
            }
        }
    }

    private static void displaySuppliers(List<Supplier> suppliers) {
        if (suppliers.isEmpty()) {
            System.out.println("  No suppliers registered.");
            return;
        }
        System.out.printf("\n  %-4s %-25s %-18s %-22s %-16s %-8s\n",
                "ID", "Company Name", "Contact Person", "Email", "Phone", "Status");
        ConsoleFormatter.printDivider(100);
        for (Supplier s : suppliers) {
            System.out.printf("  %-4d %-25s %-18s %-22s %-16s %-8s\n",
                    s.getId(), s.getCompanyName(), s.getContactName(), s.getEmail(), s.getPhone(),
                    s.isActive() ? "ACTIVE" : "INACTIVE");
        }
        ConsoleFormatter.printDivider(100);
    }

    // =========================================================================
    // 5. CUSTOMER MANAGEMENT
    // =========================================================================

    private static void handleCustomerMenu() {
        while (true) {
            ConsoleFormatter.printSectionHeader("Customer Management");
            System.out.println("  1. View All Customers");
            System.out.println("  2. Register New Customer");
            System.out.println("  3. Search Customers");
            System.out.println("  4. Update Customer Details");
            System.out.println("  5. View Customer Purchase History");
            System.out.println("  6. Delete Customer");
            System.out.println("  0. Back to Main Menu");

            int choice = InputValidator.readInt("\nChoice [0-6]: ", 0, 6);
            if (choice == 0)
                break;

            try {
                switch (choice) {
                    case 1:
                        displayCustomers(customerService.getAllCustomers());
                        InputValidator.pressEnterToContinue();
                        break;
                    case 2:
                        String name = InputValidator.readString("Customer Name: ");
                        String email = InputValidator.readOptionalEmail("Email: ", "");
                        String phone = InputValidator.readPhone("Phone: ");
                        String addr = InputValidator.readOptionalString("Address", "");
                        Customer c = new Customer(0, name, email, phone, addr);
                        if (customerService.addCustomer(c)) {
                            ConsoleFormatter.printSuccess("Customer registered with ID " + c.getId());
                        }
                        InputValidator.pressEnterToContinue();
                        break;
                    case 3:
                        String query = InputValidator.readString("Search Customer (name/phone/email): ");
                        displayCustomers(customerService.searchCustomers(query));
                        InputValidator.pressEnterToContinue();
                        break;
                    case 4:
                        int custId = InputValidator.readPositiveInt("Customer ID to update: ");
                        Customer existing = customerService.getCustomerById(custId);
                        existing.setName(InputValidator.readOptionalString("Name", existing.getName()));
                        existing.setEmail(InputValidator.readOptionalEmail("Email", existing.getEmail()));
                        existing.setPhone(InputValidator.readOptionalString("Phone", existing.getPhone()));
                        existing.setAddress(InputValidator.readOptionalString("Address", existing.getAddress()));
                        if (customerService.updateCustomer(existing)) {
                            ConsoleFormatter.printSuccess("Customer details updated successfully.");
                        }
                        InputValidator.pressEnterToContinue();
                        break;
                    case 5:
                        int histId = InputValidator.readPositiveInt("Enter Customer ID: ");
                        Customer cust = customerService.getCustomerById(histId);
                        List<Sale> sales = customerService.getCustomerSalesHistory(histId);
                        ConsoleFormatter.printSectionHeader("Purchase History for " + cust.getName());
                        displaySalesTable(sales);
                        InputValidator.pressEnterToContinue();
                        break;
                    case 6:
                        int delId = InputValidator.readPositiveInt("Customer ID to delete: ");
                        if (InputValidator.readConfirmation("Delete Customer #" + delId + "?")) {
                            if (customerService.deleteCustomer(delId)) {
                                ConsoleFormatter.printSuccess("Customer deleted successfully.");
                            }
                        }
                        InputValidator.pressEnterToContinue();
                        break;
                }
            } catch (AppException e) {
                ConsoleFormatter.printError(e.getMessage());
                InputValidator.pressEnterToContinue();
            }
        }
    }

    private static void displayCustomers(List<Customer> customers) {
        if (customers.isEmpty()) {
            System.out.println("  No customers found.");
            return;
        }
        System.out.printf("\n  %-5s %-25s %-25s %-16s %-25s\n", "ID", "Name", "Email", "Phone", "Address");
        ConsoleFormatter.printDivider(100);
        for (Customer c : customers) {
            System.out.printf("  %-5d %-25s %-25s %-16s %-25s\n",
                    c.getId(), c.getName(), c.getEmail() != null ? c.getEmail() : "N/A", c.getPhone(),
                    c.getAddress() != null ? c.getAddress() : "N/A");
        }
        ConsoleFormatter.printDivider(100);
    }

    // =========================================================================
    // 6. PURCHASE MANAGEMENT
    // =========================================================================

    private static void handlePurchaseMenu() {
        while (true) {
            ConsoleFormatter.printSectionHeader("Purchase Management");
            System.out.println("  1. View All Purchase Orders");
            System.out.println("  2. Create New Purchase Order (Stock In)");
            System.out.println("  3. View Purchase Order Receipt");
            System.out.println("  4. Cancel Purchase Order (Stock Reversal)");
            System.out.println("  0. Back to Main Menu");

            int choice = InputValidator.readInt("\nChoice [0-4]: ", 0, 4);
            if (choice == 0)
                break;

            try {
                switch (choice) {
                    case 1:
                        displayPurchasesTable(purchaseService.getAllPurchases());
                        InputValidator.pressEnterToContinue();
                        break;
                    case 2:
                        createNewPurchaseOrder();
                        break;
                    case 3:
                        int poId = InputValidator.readPositiveInt("Enter Purchase Order ID: ");
                        Purchase p = purchaseService.getPurchaseById(poId);
                        String receipt = InvoiceGenerator.generatePurchaseOrder(p, false);
                        System.out.println("\n" + receipt);
                        InputValidator.pressEnterToContinue();
                        break;
                    case 4:
                        cancelPurchaseOrder();
                        break;
                }
            } catch (AppException e) {
                ConsoleFormatter.printError(e.getMessage());
                InputValidator.pressEnterToContinue();
            }
        }
    }

    private static void displayPurchasesTable(List<Purchase> purchases) {
        if (purchases.isEmpty()) {
            System.out.println("  No purchase orders recorded.");
            return;
        }
        System.out.printf("\n  %-5s %-18s %-25s %-14s %-12s %-20s\n",
                "ID", "PO Number", "Supplier", "Total", "Status", "Date");
        ConsoleFormatter.printDivider(100);
        for (Purchase p : purchases) {
            System.out.printf("  %-5d %-18s %-25s %-14s %-12s %-20s\n",
                    p.getId(), p.getPurchaseNumber(), p.getSupplierName(),
                    ConsoleFormatter.formatCurrency(p.getTotalAmount()), p.getStatus(),
                    ConsoleFormatter.formatDateTime(p.getPurchaseDate()));
        }
        ConsoleFormatter.printDivider(100);
    }

    private static void createNewPurchaseOrder() {
        ConsoleFormatter.printSectionHeader("Create New Purchase Order");
        List<Supplier> suppliers = supplierService.getAllSuppliers();
        if (suppliers.isEmpty()) {
            ConsoleFormatter.printWarning("Please register a supplier first.");
            return;
        }

        System.out.println("Select Supplier:");
        for (Supplier s : suppliers) {
            System.out.printf("  [%d] %s (%s)\n", s.getId(), s.getCompanyName(), s.getContactName());
        }
        int supId = InputValidator.readInt("Enter Supplier ID: ", 1, Integer.MAX_VALUE);

        List<PurchaseItem> items = new ArrayList<>();
        boolean adding = true;
        while (adding) {
            System.out.println("\nAdd Item to PO:");
            int prodId = InputValidator.readPositiveInt("Enter Product ID: ");
            Product prod = productService.getProductById(prodId);
            System.out.println("  Selected: " + prod.getName() + " [Current Stock: " + prod.getCurrentStock()
                    + ", Current Cost: $" + prod.getPurchasePrice() + "]");

            int qty = InputValidator.readPositiveInt("Enter Purchase Quantity: ");
            BigDecimal unitCost = InputValidator
                    .readBigDecimal("Enter Unit Cost ($) [" + prod.getPurchasePrice() + "]: ", BigDecimal.ZERO, true);

            PurchaseItem item = new PurchaseItem(prodId, prod.getName(), qty, unitCost);
            items.add(item);
            ConsoleFormatter.printSuccess(
                    "Item added: " + qty + "x " + prod.getName() + " @ $" + unitCost + " = $" + item.getSubtotal());

            adding = InputValidator.readConfirmation("Add another product to this purchase order?");
        }

        String notes = InputValidator.readOptionalString("Purchase Notes / Remarks", "");
        System.out.println("\nSummary of Purchase Order:");
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseItem it : items) {
            total = total.add(it.getSubtotal());
            System.out.printf("  - %-25s x %-4d @ %-10s = %s\n",
                    it.getProductName(), it.getQuantity(), ConsoleFormatter.formatCurrency(it.getUnitCost()),
                    ConsoleFormatter.formatCurrency(it.getSubtotal()));
        }
        System.out.println("  Total Payable: " + ConsoleFormatter.formatCurrency(total));

        if (InputValidator.readConfirmation("Confirm and process purchase order?")) {
            Purchase po = purchaseService.createPurchaseOrder(supId, authService.getCurrentUser().getId(), items,
                    notes);
            ConsoleFormatter.printSuccess("Purchase order created successfully: " + po.getPurchaseNumber());
            System.out.println("\n" + InvoiceGenerator.generatePurchaseOrder(po, false));
        } else {
            System.out.println("Purchase order cancelled.");
        }
        InputValidator.pressEnterToContinue();
    }

    private static void cancelPurchaseOrder() {
        ConsoleFormatter.printSectionHeader("Cancel Purchase Order");
        int poId = InputValidator.readPositiveInt("Enter Purchase ID to cancel: ");
        Purchase p = purchaseService.getPurchaseById(poId);
        System.out.println("Cancelling: " + p.getPurchaseNumber() + " | Supplier: " + p.getSupplierName() + " | Total: "
                + ConsoleFormatter.formatCurrency(p.getTotalAmount()));
        String reason = InputValidator.readString("Enter cancellation reason: ");
        if (InputValidator.readConfirmation("Are you sure? This will reverse and deduct inventory stock.")) {
            if (purchaseService.cancelPurchaseOrder(poId, authService.getCurrentUser().getId(), reason)) {
                ConsoleFormatter.printSuccess("Purchase Order cancelled and stock safely reversed!");
            }
        }
        InputValidator.pressEnterToContinue();
    }

    // =========================================================================
    // 7. SALES MANAGEMENT
    // =========================================================================

    private static void handleSalesMenu() {
        while (true) {
            ConsoleFormatter.printSectionHeader("Sales Management");
            System.out.println("  1. View All Sales Invoices");
            System.out.println("  2. Create New Sales Transaction (Checkout)");
            System.out.println("  3. View Sale Invoice Receipt");
            System.out.println("  4. Cancel Sales Order (Stock Restoration)");
            System.out.println("  0. Back to Main Menu");

            int choice = InputValidator.readInt("\nChoice [0-4]: ", 0, 4);
            if (choice == 0)
                break;

            try {
                switch (choice) {
                    case 1:
                        displaySalesTable(salesService.getAllSales());
                        InputValidator.pressEnterToContinue();
                        break;
                    case 2:
                        createNewSaleTransaction();
                        break;
                    case 3:
                        int saleId = InputValidator.readPositiveInt("Enter Sale ID: ");
                        Sale s = salesService.getSaleById(saleId);
                        String invoice = InvoiceGenerator.generateSalesInvoice(s, false);
                        System.out.println("\n" + invoice);
                        InputValidator.pressEnterToContinue();
                        break;
                    case 4:
                        cancelSaleTransaction();
                        break;
                }
            } catch (AppException e) {
                ConsoleFormatter.printError(e.getMessage());
                InputValidator.pressEnterToContinue();
            }
        }
    }

    private static void displaySalesTable(List<Sale> sales) {
        if (sales.isEmpty()) {
            System.out.println("  No sales records found.");
            return;
        }
        System.out.printf("\n  %-5s %-18s %-25s %-12s %-10s %-12s %-20s\n",
                "ID", "Invoice No", "Customer", "Total", "Payment", "Status", "Date");
        ConsoleFormatter.printDivider(110);
        for (Sale s : sales) {
            System.out.printf("  %-5d %-18s %-25s %-12s %-10s %-12s %-20s\n",
                    s.getId(), s.getInvoiceNumber(), s.getCustomerName(),
                    ConsoleFormatter.formatCurrency(s.getTotalAmount()), s.getPaymentMethod(),
                    s.getStatus(), ConsoleFormatter.formatDateTime(s.getSaleDate()));
        }
        ConsoleFormatter.printDivider(110);
    }

    private static void createNewSaleTransaction() {
        ConsoleFormatter.printSectionHeader("Create New Sales Transaction (Checkout)");

        Integer custId = null;
        if (InputValidator.readConfirmation("Is this for a registered customer?")) {
            List<Customer> customers = customerService.getAllCustomers();
            if (customers.isEmpty()) {
                System.out.println("  No customers registered. Defaulting to Walk-in Customer.");
            } else {
                for (Customer c : customers) {
                    System.out.printf("  [%d] %s (%s)\n", c.getId(), c.getName(), c.getPhone());
                }
                custId = InputValidator.readPositiveInt("Select Customer ID: ");
            }
        }

        List<SaleItem> items = new ArrayList<>();
        boolean adding = true;
        while (adding) {
            System.out.println("\nAdd Item to Sale:");
            String query = InputValidator.readString("Enter Product ID or Barcode: ");
            Product prod;
            try {
                int id = Integer.parseInt(query);
                prod = productService.getProductById(id);
            } catch (NumberFormatException e) {
                prod = productService.getProductByBarcode(query);
            }

            if (prod == null) {
                ConsoleFormatter.printError("Product not found. Try again.");
                continue;
            }

            if (prod.getCurrentStock() <= 0) {
                ConsoleFormatter.printError("Product '" + prod.getName() + "' is OUT OF STOCK!");
                continue;
            }

            System.out.printf("  Selected: %s | Price: %s | Available Stock: %d units\n",
                    prod.getName(), ConsoleFormatter.formatCurrency(prod.getSellingPrice()), prod.getCurrentStock());

            int qty = InputValidator.readInt("Enter Quantity to Sell: ", 1, prod.getCurrentStock());
            SaleItem item = new SaleItem(prod.getId(), prod.getName(), qty, prod.getSellingPrice(),
                    prod.getPurchasePrice());
            items.add(item);
            ConsoleFormatter.printSuccess("Added: " + qty + "x " + prod.getName() + " = "
                    + ConsoleFormatter.formatCurrency(item.getSubtotal()));

            adding = InputValidator.readConfirmation("Add another item to this sale?");
        }

        BigDecimal discountPct = InputValidator.readBigDecimal("Discount Percentage (% e.g. 5 for 5%): ",
                BigDecimal.ZERO, true);
        BigDecimal taxPct = InputValidator.readBigDecimal("Tax / VAT Percentage (% e.g. 8 for 8%): ", BigDecimal.ZERO,
                true);

        System.out.println("Select Payment Method:");
        System.out.println("  [1] CASH\n  [2] CARD\n  [3] ONLINE\n  [4] OTHER");
        int payChoice = InputValidator.readInt("Choice [1-4]: ", 1, 4);
        String paymentMethod = payChoice == 1 ? "CASH"
                : (payChoice == 2 ? "CARD" : (payChoice == 3 ? "ONLINE" : "OTHER"));

        if (InputValidator.readConfirmation("Confirm and complete sale?")) {
            Sale sale = salesService.createSale(custId, authService.getCurrentUser().getId(), items, discountPct,
                    taxPct, paymentMethod);
            ConsoleFormatter.printSuccess("Sale completed successfully! Invoice Number: " + sale.getInvoiceNumber());
            System.out.println("\n" + InvoiceGenerator.generateSalesInvoice(sale, false));
        } else {
            System.out.println("Sale aborted.");
        }
        InputValidator.pressEnterToContinue();
    }

    private static void cancelSaleTransaction() {
        ConsoleFormatter.printSectionHeader("Cancel Sale Invoice");
        int saleId = InputValidator.readPositiveInt("Enter Sale ID to cancel: ");
        Sale s = salesService.getSaleById(saleId);
        System.out.println("Cancelling: " + s.getInvoiceNumber() + " | Customer: " + s.getCustomerName() + " | Total: "
                + ConsoleFormatter.formatCurrency(s.getTotalAmount()));
        String reason = InputValidator.readString("Enter cancellation reason: ");
        if (InputValidator.readConfirmation("Confirm cancellation? This will restore stock to inventory.")) {
            if (salesService.cancelSale(saleId, authService.getCurrentUser().getId(), reason)) {
                ConsoleFormatter.printSuccess("Sale invoice cancelled and inventory restored!");
            }
        }
        InputValidator.pressEnterToContinue();
    }

    // =========================================================================
    // 8. INVENTORY MANAGEMENT
    // ========= ================================================================

    private static void handleInventoryMenu() {
        while (true) {
            ConsoleFormatter.printSectionHeader("Inventory Management");
            System.out.println("  1. Display Current Inventory Stock");
            System.out.println("  2. View Stock Movement Audit Log");
            System.out.println("  3. Manual Stock Adjustment (Stock In / Stock Out / Audit Adjustment)");
            System.out.println("  4. Inventory Valuation Report");
            System.out.println("  0. Back to Main Menu");

            int choice = InputValidator.readInt("\nChoice [0-4]: ", 0, 4);
            if (choice == 0)
                break;

            try {
                switch (choice) {
                    case 1:
                        displayProductsTable(inventoryService.getCurrentStockList());
                        InputValidator.pressEnterToContinue();
                        break;
                    case 2:
                        viewStockMovements();
                        break;
                    case 3:
                        performManualStockAdjustment();
                        break;
                    case 4:
                        displayInventoryValuation();
                        break;
                }
            } catch (AppException e) {
                ConsoleFormatter.printError(e.getMessage());
                InputValidator.pressEnterToContinue();
            }
        }
    }

    private static void viewStockMovements() {
        ConsoleFormatter.printSectionHeader("Stock Movement Audit Trail");
        Integer prodFilter = null;
        if (InputValidator.readConfirmation("Filter movements by a specific product?")) {
            prodFilter = InputValidator.readPositiveInt("Enter Product ID: ");
        }
        int limit = InputValidator.readInt("Number of records to display (default 25): ", 1, 200);
        List<StockMovement> list = inventoryService.getStockMovements(prodFilter, limit);

        if (list.isEmpty()) {
            System.out.println("  No stock movements recorded.");
            return;
        }

        System.out.printf("\n  %-5s %-20s %-18s %6s %6s %6s %-16s %-20s %-20s\n",
                "ID", "Product", "Movement Type", "Prev", "Delta", "New", "Reference", "User", "Timestamp");
        ConsoleFormatter.printDivider(125);
        for (StockMovement sm : list) {
            String pName = sm.getProductName().length() > 20 ? sm.getProductName().substring(0, 17) + "..."
                    : sm.getProductName();
            System.out.printf("  %-5d %-20s %-18s %6d %+6d %6d %-16s %-20s %-20s\n",
                    sm.getId(), pName, sm.getMovementType(), sm.getPreviousStock(), sm.getQuantity(),
                    sm.getNewStock(), sm.getReferenceId() != null ? sm.getReferenceId() : "N/A",
                    sm.getCreatedByName() != null ? sm.getCreatedByName() : "System",
                    ConsoleFormatter.formatDateTime(sm.getCreatedAt()));
        }
        ConsoleFormatter.printDivider(125);
        InputValidator.pressEnterToContinue();
    }

    private static void performManualStockAdjustment() {
        ConsoleFormatter.printSectionHeader("Manual Stock Adjustment");
        int prodId = InputValidator.readPositiveInt("Enter Product ID: ");
        Product prod = productService.getProductById(prodId);

        System.out.printf("  Selected: %s | Current Stock: %d units\n", prod.getName(), prod.getCurrentStock());
        System.out.println("  [1] Stock In (Damaged stock found / returned without PO)");
        System.out.println("  [2] Stock Out (Damage / Theft / Breakage write-off)");
        System.out.println("  [3] Complete Audit Re-count Adjustment");

        int mode = InputValidator.readInt("Select adjustment mode [1-3]: ", 1, 3);
        int delta = 0;
        StockMovementType type = StockMovementType.ADJUSTMENT;

        if (mode == 1) {
            int qty = InputValidator.readPositiveInt("Enter quantity to ADD: ");
            delta = qty;
            type = StockMovementType.MANUAL_IN;
        } else if (mode == 2) {
            int qty = InputValidator.readInt("Enter quantity to REMOVE: ", 1, prod.getCurrentStock());
            delta = -qty;
            type = StockMovementType.MANUAL_OUT;
        } else {
            int countedStock = InputValidator.readNonNegativeInt("Enter actual physical count: ");
            delta = countedStock - prod.getCurrentStock();
            type = StockMovementType.ADJUSTMENT;
        }

        String reason = InputValidator.readString("Enter reason for manual adjustment: ");
        if (InputValidator
                .readConfirmation("Confirm adjusting stock by " + (delta >= 0 ? "+" + delta : delta) + " units?")) {
            if (inventoryService.adjustStock(prodId, delta, type, reason, authService.getCurrentUser().getId())) {
                ConsoleFormatter
                        .printSuccess("Stock adjusted successfully! New Stock: " + (prod.getCurrentStock() + delta));
            }
        }
        InputValidator.pressEnterToContinue();
    }

    private static void displayInventoryValuation() {
        ConsoleFormatter.printSectionHeader("Inventory Valuation Statement");
        Map<String, Object> val = inventoryService.getInventoryValuation();

        ConsoleFormatter.printMetric("Total Active Products", val.get("total_items"), 30);
        ConsoleFormatter.printMetric("Total Physical Units in Stock", val.get("total_units"), 30);
        ConsoleFormatter.printMetric("Total Inventory Cost Valuation",
                ConsoleFormatter.formatCurrency((BigDecimal) val.get("cost_valuation")), 30);
        ConsoleFormatter.printMetric("Total Expected Retail Valuation",
                ConsoleFormatter.formatCurrency((BigDecimal) val.get("retail_valuation")), 30);
        ConsoleFormatter.printMetric("Potential Gross Margin",
                ConsoleFormatter.formatCurrency((BigDecimal) val.get("potential_profit")), 30);
        InputValidator.pressEnterToContinue();
    }

    // =========================================================================
    // 9. EMPLOYEE MANAGEMENT
    // =========================================================================

    private static void handleEmployeeMenu() {
        while (true) {
            ConsoleFormatter.printSectionHeader("Employee & HR Management");
            System.out.println("  1. View All Employees");
            System.out.println("  2. Add New Employee");
            System.out.println("  3. Update Employee Record");
            System.out.println("  4. Set Employee Status (ACTIVE / INACTIVE / TERMINATED)");
            System.out.println("  5. Create System Login Account for Employee");
            System.out.println("  0. Back to Main Menu");

            int choice = InputValidator.readInt("\nChoice [0-5]: ", 0, 5);
            if (choice == 0)
                break;

            try {
                switch (choice) {
                    case 1:
                        displayEmployees(employeeService.getAllEmployees());
                        InputValidator.pressEnterToContinue();
                        break;
                    case 2:
                        String fName = InputValidator.readString("First Name: ");
                        String lName = InputValidator.readString("Last Name: ");
                        String email = InputValidator.readEmail("Email: ");
                        String phone = InputValidator.readPhone("Phone: ");
                        String dept = InputValidator.readString("Department: ");
                        String desig = InputValidator.readString("Designation: ");
                        BigDecimal salary = InputValidator.readBigDecimal("Monthly Salary ($): ", BigDecimal.ZERO,
                                true);
                        Date hireDate = InputValidator.readDate("Hire Date", true);

                        Employee emp = new Employee(0, null, fName, lName, email, phone, dept, desig, salary, hireDate,
                                "ACTIVE");
                        if (employeeService.addEmployee(emp)) {
                            ConsoleFormatter.printSuccess("Employee added with ID #" + emp.getId());
                        }
                        InputValidator.pressEnterToContinue();
                        break;
                    case 3:
                        int empId = InputValidator.readPositiveInt("Employee ID to edit: ");
                        Employee ex = employeeService.getEmployeeById(empId);
                        ex.setFirstName(InputValidator.readOptionalString("First Name", ex.getFirstName()));
                        ex.setLastName(InputValidator.readOptionalString("Last Name", ex.getLastName()));
                        ex.setEmail(InputValidator.readOptionalString("Email", ex.getEmail()));
                        ex.setPhone(InputValidator.readOptionalString("Phone", ex.getPhone()));
                        ex.setDepartment(InputValidator.readOptionalString("Department", ex.getDepartment()));
                        ex.setDesignation(InputValidator.readOptionalString("Designation", ex.getDesignation()));
                        ex.setSalary(InputValidator.readBigDecimal("Salary ($) [" + ex.getSalary() + "]: ",
                                BigDecimal.ZERO, true));
                        if (employeeService.updateEmployee(ex)) {
                            ConsoleFormatter.printSuccess("Employee updated successfully.");
                        }
                        InputValidator.pressEnterToContinue();
                        break;
                    case 4:
                        int stId = InputValidator.readPositiveInt("Employee ID: ");
                        System.out.println("  [1] ACTIVE\n  [2] INACTIVE\n  [3] TERMINATED");
                        int stChoice = InputValidator.readInt("Select Status [1-3]: ", 1, 3);
                        String st = stChoice == 1 ? "ACTIVE" : (stChoice == 2 ? "INACTIVE" : "TERMINATED");
                        if (employeeService.setEmployeeStatus(stId, st)) {
                            ConsoleFormatter.printSuccess("Status updated to " + st);
                        }
                        InputValidator.pressEnterToContinue();
                        break;
                    case 5:
                        int loginEmpId = InputValidator.readPositiveInt("Employee ID to create login for: ");
                        String uName = InputValidator.readString("Desired Username: ");
                        String uPass = InputValidator.readString("Password (min 6 chars): ");
                        System.out.println("Select Role:\n  [1] ADMIN\n  [2] STAFF");
                        int rChoice = InputValidator.readInt("Role [1-2]: ", 1, 2);
                        User newUser = employeeService.createLoginForEmployee(loginEmpId, uName, uPass, rChoice);
                        ConsoleFormatter.printSuccess("User account created successfully! ID: " + newUser.getId()
                                + " [Role: " + (rChoice == 1 ? "ADMIN" : "STAFF") + "]");
                        InputValidator.pressEnterToContinue();
                        break;
                }
            } catch (AppException e) {
                ConsoleFormatter.printError(e.getMessage());
                InputValidator.pressEnterToContinue();
            }
        }
    }

    private static void displayEmployees(List<Employee> employees) {
        if (employees.isEmpty()) {
            System.out.println("  No employees registered.");
            return;
        }
        System.out.printf("\n  %-4s %-20s %-22s %-16s %-15s %-18s %12s %-10s\n",
                "ID", "Name", "Email", "Phone", "Department", "Designation", "Salary", "Status");
        ConsoleFormatter.printDivider(125);
        for (Employee e : employees) {
            System.out.printf("  %-4d %-20s %-22s %-16s %-15s %-18s %12s %-10s\n",
                    e.getId(), e.getFullName(), e.getEmail(), e.getPhone(),
                    e.getDepartment(), e.getDesignation(),
                    ConsoleFormatter.formatCurrency(e.getSalary()), e.getStatus());
        }
        ConsoleFormatter.printDivider(125);
    }

    // =========================================================================
    // 10. EXPENSE MANAGEMENT
    // =========================================================================

    private static void handleExpenseMenu() {
        while (true) {
            ConsoleFormatter.printSectionHeader("Business Expense Management");
            System.out.println("  1. View All Expenses");
            System.out.println("  2. Record New Business Expense");
            System.out.println("  3. View Expenses by Date Range");
            System.out.println("  4. Manage Expense Categories");
            System.out.println("  5. Delete Expense Record");
            System.out.println("  0. Back to Main Menu");

            int choice = InputValidator.readInt("\nChoice [0-5]: ", 0, 5);
            if (choice == 0)
                break;

            try {
                switch (choice) {
                    case 1:
                        displayExpensesTable(expenseService.getAllExpenses());
                        InputValidator.pressEnterToContinue();
                        break;
                    case 2:
                        recordNewExpense();
                        break;
                    case 3:
                        Date start = InputValidator.readDate("Start Date", false);
                        Date end = InputValidator.readDate("End Date", true);
                        displayExpensesTable(expenseService.getExpensesByDateRange(start, end));
                        BigDecimal total = expenseService.getTotalExpenses(start, end);
                        System.out.println("\n  Total Expenses for Period: " + ConsoleFormatter.formatCurrency(total));
                        InputValidator.pressEnterToContinue();
                        break;
                    case 4:
                        manageExpenseCategories();
                        break;
                    case 5:
                        int delId = InputValidator.readPositiveInt("Expense ID to delete: ");
                        if (InputValidator.readConfirmation("Delete Expense ID #" + delId + "?")) {
                            if (expenseService.deleteExpense(delId)) {
                                ConsoleFormatter.printSuccess("Expense record deleted.");
                            }
                        }
                        InputValidator.pressEnterToContinue();
                        break;
                }
            } catch (AppException e) {
                ConsoleFormatter.printError(e.getMessage());
                InputValidator.pressEnterToContinue();
            }
        }
    }

    private static void displayExpensesTable(List<Expense> expenses) {
        if (expenses.isEmpty()) {
            System.out.println("  No expenses recorded for selected criteria.");
            return;
        }
        System.out.printf("\n  %-5s %-25s %-22s %12s %-12s %-20s\n",
                "ID", "Title", "Category", "Amount", "Date", "Recorded By");
        ConsoleFormatter.printDivider(105);
        BigDecimal sum = BigDecimal.ZERO;
        for (Expense e : expenses) {
            sum = sum.add(e.getAmount());
            System.out.printf("  %-5d %-25s %-22s %12s %-12s %-20s\n",
                    e.getId(), e.getTitle(), e.getCategoryName(),
                    ConsoleFormatter.formatCurrency(e.getAmount()),
                    ConsoleFormatter.formatDate(e.getExpenseDate()),
                    e.getUserName());
        }
        ConsoleFormatter.printDivider(105);
        System.out.printf("  Total Expense Amount: %s\n", ConsoleFormatter.formatCurrency(sum));
    }

    private static void recordNewExpense() {
        ConsoleFormatter.printSectionHeader("Record New Expense");
        List<ExpenseCategory> categories = expenseService.getAllCategories();
        if (categories.isEmpty()) {
            ConsoleFormatter.printWarning("Please create an expense category first.");
            return;
        }

        System.out.println("Categories:");
        for (ExpenseCategory ec : categories) {
            System.out.printf("  [%d] %s\n", ec.getId(), ec.getName());
        }

        int catId = InputValidator.readInt("Select Category ID: ", 1, Integer.MAX_VALUE);
        String title = InputValidator.readString("Expense Title/Subject: ");
        BigDecimal amount = InputValidator.readBigDecimal("Amount ($): ", BigDecimal.ZERO, false);
        Date date = InputValidator.readDate("Expense Date", true);
        String notes = InputValidator.readOptionalString("Notes / Reference No", "");

        Expense exp = new Expense(0, title, catId, "", amount, date, notes, authService.getCurrentUser().getId(), "");
        if (expenseService.recordExpense(exp)) {
            ConsoleFormatter.printSuccess("Expense recorded successfully! ID: " + exp.getId());
        }
        InputValidator.pressEnterToContinue();
    }

    private static void manageExpenseCategories() {
        ConsoleFormatter.printSectionHeader("Expense Categories");
        List<ExpenseCategory> list = expenseService.getAllCategories();
        for (ExpenseCategory c : list) {
            System.out.printf("  [%d] %-25s - %s\n", c.getId(), c.getName(), c.getDescription());
        }

        if (InputValidator.readConfirmation("Add a new expense category?")) {
            String name = InputValidator.readString("Category Name: ");
            String desc = InputValidator.readOptionalString("Description", "");
            if (expenseService.addCategory(name, desc)) {
                ConsoleFormatter.printSuccess("Expense category created.");
            }
        }
        InputValidator.pressEnterToContinue();
    }

    // =========================================================================
    // 11. BUSINESS REPORTS
    // =========================================================================

    private static void handleReportsMenu() {
        while (true) {
            ConsoleFormatter.printSectionHeader("Business Intelligence Reports");
            System.out.println("  1. Daily Sales Report (Today)");
            System.out.println("  2. Weekly Sales Report (Past 7 Days)");
            System.out.println("  3. Monthly Sales Report (Current Month)");
            System.out.println("  4. Annual Sales Report (Current Year)");
            System.out.println("  5. Custom Period Sales Report");
            System.out.println("  6. Purchase Orders Report");
            System.out.println("  7. Profit and Loss Financial Statement");
            System.out.println("  8. Top-Selling Products Performance");
            System.out.println("  0. Back to Main Menu");

            int choice = InputValidator.readInt("\nChoice [0-8]: ", 0, 8);
            if (choice == 0)
                break;

            try {
                switch (choice) {
                    case 1:
                        displayReportResults("DAILY SALES REPORT (TODAY)",
                                reportService.getDailySalesReport(LocalDate.now()));
                        break;
                    case 2:
                        displayReportResults("WEEKLY SALES REPORT (PAST 7 DAYS)", reportService.getWeeklySalesReport());
                        break;
                    case 3:
                        LocalDate now = LocalDate.now();
                        displayReportResults("MONTHLY SALES REPORT (" + now.getMonth() + " " + now.getYear() + ")",
                                reportService.getMonthlySalesReport(now.getYear(), now.getMonthValue()));
                        break;
                    case 4:
                        int year = LocalDate.now().getYear();
                        displayReportResults("ANNUAL SALES REPORT (" + year + ")",
                                reportService.getAnnualSalesReport(year));
                        break;
                    case 5:
                        Date sDate = InputValidator.readDate("Start Date", false);
                        Date eDate = InputValidator.readDate("End Date", true);
                        displayReportResults("SALES REPORT (" + sDate + " to " + eDate + ")",
                                reportService.getCustomSalesReport(sDate, eDate));
                        break;
                    case 6:
                        Date pStart = InputValidator.readDate("Start Date", false);
                        Date pEnd = InputValidator.readDate("End Date", true);
                        displayPurchaseReportResults(reportService.getPurchaseReport(pStart, pEnd));
                        break;
                    case 7:
                        displayProfitAndLossStatement();
                        break;
                    case 8:
                        displayTopSellingProducts();
                        break;
                }
            } catch (AppException e) {
                ConsoleFormatter.printError(e.getMessage());
                InputValidator.pressEnterToContinue();
            }
        }
    }

    private static void displayReportResults(String title, List<Map<String, Object>> rows) {
        ConsoleFormatter.printBanner(title);
        if (rows.isEmpty()) {
            System.out.println("  No transactions found for this period.");
            InputValidator.pressEnterToContinue();
            return;
        }

        System.out.printf("  %-16s %-14s %-20s %10s %10s %10s %12s %-8s\n",
                "Invoice No", "Date", "Customer", "Subtotal", "Discount", "Tax", "Total", "Status");
        ConsoleFormatter.printDivider(110);
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        for (Map<String, Object> r : rows) {
            BigDecimal sub = (BigDecimal) r.get("subtotal");
            BigDecimal tot = (BigDecimal) r.get("total_amount");
            totalGross = totalGross.add(sub);
            totalNet = totalNet.add(tot);

            System.out.printf("  %-16s %-14s %-20s %10s %10s %10s %12s %-8s\n",
                    r.get("invoice_number"),
                    ConsoleFormatter.formatDate((java.util.Date) r.get("sale_date")),
                    r.get("customer_name"),
                    ConsoleFormatter.formatCurrency(sub),
                    ConsoleFormatter.formatCurrency((BigDecimal) r.get("discount_amount")),
                    ConsoleFormatter.formatCurrency((BigDecimal) r.get("tax_amount")),
                    ConsoleFormatter.formatCurrency(tot),
                    r.get("status"));
        }
        ConsoleFormatter.printDivider(110);
        System.out.printf("  Total Transactions: %-5d | Total Subtotal: %-15s | Net Revenue: %s\n",
                rows.size(), ConsoleFormatter.formatCurrency(totalGross), ConsoleFormatter.formatCurrency(totalNet));
        InputValidator.pressEnterToContinue();
    }

    private static void displayPurchaseReportResults(List<Map<String, Object>> rows) {
        ConsoleFormatter.printBanner("PURCHASE ORDERS REPORT");
        if (rows.isEmpty()) {
            System.out.println("  No purchases found for this period.");
            InputValidator.pressEnterToContinue();
            return;
        }

        System.out.printf("  %-18s %-14s %-25s %14s %-10s %-18s\n",
                "PO Number", "Date", "Supplier", "Total Amount", "Status", "Ordered By");
        ConsoleFormatter.printDivider(105);
        BigDecimal sum = BigDecimal.ZERO;
        for (Map<String, Object> r : rows) {
            BigDecimal amt = (BigDecimal) r.get("total_amount");
            sum = sum.add(amt);
            System.out.printf("  %-18s %-14s %-25s %14s %-10s %-18s\n",
                    r.get("purchase_number"),
                    ConsoleFormatter.formatDate((java.util.Date) r.get("purchase_date")),
                    r.get("supplier_name"),
                    ConsoleFormatter.formatCurrency(amt),
                    r.get("status"),
                    r.get("ordered_by"));
        }
        ConsoleFormatter.printDivider(105);
        System.out.printf("  Total Purchase Volume: %s\n", ConsoleFormatter.formatCurrency(sum));
        InputValidator.pressEnterToContinue();
    }

    private static void displayProfitAndLossStatement() {
        ConsoleFormatter.printSectionHeader("Profit & Loss Statement");
        Date start = InputValidator.readDate("Statement Start Date", false);
        Date end = InputValidator.readDate("Statement End Date", true);

        Map<String, BigDecimal> pnl = reportService.getProfitAndLossReport(start, end);

        ConsoleFormatter.printBanner("INCOME STATEMENT (P&L)");
        System.out.println("  Reporting Period: " + start + " to " + end);
        ConsoleFormatter.printDivider(70);

        System.out.println("  [ REVENUE / TURNOVER ]");
        ConsoleFormatter.printMetric("Gross Sales", ConsoleFormatter.formatCurrency(pnl.get("gross_sales")), 30);
        ConsoleFormatter.printMetric("Discounts Allowed (-)",
                ConsoleFormatter.formatCurrency(pnl.get("total_discounts")), 30);
        ConsoleFormatter.printMetric("NET REVENUE", ConsoleFormatter.formatCurrency(pnl.get("net_revenue")), 30);

        System.out.println("\n  [ COST OF GOODS SOLD ]");
        ConsoleFormatter.printMetric("Cost of Goods Sold (COGS)", ConsoleFormatter.formatCurrency(pnl.get("cogs")), 30);
        ConsoleFormatter.printDivider(70);
        ConsoleFormatter.printMetric("GROSS PROFIT", ConsoleFormatter.formatCurrency(pnl.get("gross_profit")), 30);

        System.out.println("\n  [ OPERATING EXPENSES ]");
        ConsoleFormatter.printMetric("Total Business Expenses",
                ConsoleFormatter.formatCurrency(pnl.get("operating_expenses")), 30);
        ConsoleFormatter.printDoubleDivider(70);

        BigDecimal netProfit = pnl.get("net_profit");
        String profitLabel = netProfit.compareTo(BigDecimal.ZERO) >= 0 ? "NET PROFIT" : "NET LOSS";
        ConsoleFormatter.printMetric(profitLabel, ConsoleFormatter.formatCurrency(netProfit), 30);
        ConsoleFormatter.printDoubleDivider(70);
        InputValidator.pressEnterToContinue();
    }

    private static void displayTopSellingProducts() {
        ConsoleFormatter.printSectionHeader("Top-Selling Products");
        int limit = InputValidator.readInt("Enter ranking count (default 5): ", 1, 50);
        List<Map<String, Object>> top = reportService.getTopSellingProducts(limit);

        if (top.isEmpty()) {
            System.out.println("  No completed sales data available.");
            InputValidator.pressEnterToContinue();
            return;
        }

        System.out.printf("\n  %-4s %-14s %-30s %-20s %10s %14s\n",
                "Rank", "Barcode", "Product Name", "Category", "Units Sold", "Total Revenue");
        ConsoleFormatter.printDivider(100);
        int rank = 1;
        for (Map<String, Object> p : top) {
            System.out.printf("  %-4d %-14s %-30s %-20s %10d %14s\n",
                    rank++,
                    p.get("barcode"),
                    p.get("product_name"),
                    p.get("category_name"),
                    p.get("units_sold"),
                    ConsoleFormatter.formatCurrency((BigDecimal) p.get("total_revenue")));
        }
        ConsoleFormatter.printDivider(100);
        InputValidator.pressEnterToContinue();
    }

    // =========================================================================
    // 12. USER MANAGEMENT
    // =========================================================================

    private static void handleUserManagementMenu() {
        while (true) {
            ConsoleFormatter.printSectionHeader("User & Role Administration");
            System.out.println("  1. View All System Users");
            System.out.println("  2. Create New System User");
            System.out.println("  3. Reset User Password");
            System.out.println("  4. Toggle User Activation Status");
            System.out.println("  0. Back to Main Menu");

            int choice = InputValidator.readInt("\nChoice [0-4]: ", 0, 4);
            if (choice == 0)
                break;

            try {
                switch (choice) {
                    case 1:
                        displayUsersTable(userService.getAllUsers());
                        InputValidator.pressEnterToContinue();
                        break;
                    case 2:
                        String uName = InputValidator.readString("Username: ");
                        String uPass = InputValidator.readString("Password (min 6 chars): ");
                        String fName = InputValidator.readString("Full Name: ");
                        String email = InputValidator.readEmail("Email: ");
                        System.out.println("Roles:\n  [1] ADMIN\n  [2] STAFF");
                        int roleId = InputValidator.readInt("Role [1-2]: ", 1, 2);
                        if (userService.createUser(uName, uPass, fName, email, roleId)) {
                            ConsoleFormatter.printSuccess("User created successfully.");
                        }
                        InputValidator.pressEnterToContinue();
                        break;
                    case 3:
                        int uid = InputValidator.readPositiveInt("User ID to reset password: ");
                        String newP = InputValidator.readString("Enter New Password (min 6 chars): ");
                        if (userService.resetPassword(uid, newP)) {
                            ConsoleFormatter.printSuccess("Password updated successfully.");
                        }
                        InputValidator.pressEnterToContinue();
                        break;
                    case 4:
                        int tUid = InputValidator.readPositiveInt("User ID to toggle status: ");
                        User targetUser = userService.getUserById(tUid);
                        if (targetUser.getId() == authService.getCurrentUser().getId()) {
                            ConsoleFormatter.printError("You cannot deactivate your own account while logged in!");
                            InputValidator.pressEnterToContinue();
                            break;
                        }
                        boolean newActive = !targetUser.isActive();
                        if (userService.toggleUserStatus(tUid, newActive)) {
                            ConsoleFormatter
                                    .printSuccess("User status changed to: " + (newActive ? "ACTIVE" : "INACTIVE"));
                        }
                        InputValidator.pressEnterToContinue();
                        break;
                }
            } catch (AppException e) {
                ConsoleFormatter.printError(e.getMessage());
                InputValidator.pressEnterToContinue();
            }
        }
    }

    private static void displayUsersTable(List<User> users) {
        System.out.printf("\n  %-4s %-16s %-25s %-25s %-8s %-8s\n",
                "ID", "Username", "Full Name", "Email", "Role", "Status");
        ConsoleFormatter.printDivider(95);
        for (User u : users) {
            System.out.printf("  %-4d %-16s %-25s %-25s %-8s %-8s\n",
                    u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getRoleName(),
                    u.isActive() ? "ACTIVE" : "INACTIVE");
        }
        ConsoleFormatter.printDivider(95);
    }

    private static void handleChangePassword() {
        ConsoleFormatter.printSectionHeader("Change My Password");
        String oldPass = InputValidator.readString("Enter Current Password: ");
        String newPass = InputValidator.readString("Enter New Password (min 6 chars): ");
        String confirmPass = InputValidator.readString("Confirm New Password: ");

        if (!newPass.equals(confirmPass)) {
            ConsoleFormatter.printError("Passwords do not match!");
            InputValidator.pressEnterToContinue();
            return;
        }

        try {
            if (authService.changePassword(oldPass, newPass)) {
                ConsoleFormatter.printSuccess("Password changed successfully!");
            }
        } catch (AppException e) {
            ConsoleFormatter.printError(e.getMessage());
        }
        InputValidator.pressEnterToContinue();
    }
}
