package ui;

import model.User;
import service.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Main Application Window hosting the navigation sidebar and modular CardLayout views.
 */
public class MainFrame extends JFrame {

    private final User currentUser;
    private final AuthService authService;
    private final Runnable onLogoutCallback;

    // Services
    private final ProductService productService;
    private final CategoryService categoryService;
    private final SalesService salesService;
    private final PurchaseService purchaseService;
    private final InventoryService inventoryService;
    private final CustomerService customerService;
    private final SupplierService supplierService;
    private final EmployeeService employeeService;
    private final ExpenseService expenseService;
    private final ReportService reportService;
    private final UserService userService;

    // Panels
    private DashboardPanel dashboardPanel;
    private SalesPanel salesPanel;
    private ProductPanel productPanel;
    private CategoryPanel categoryPanel;
    private PurchasePanel purchasePanel;
    private InventoryPanel inventoryPanel;
    private CustomerPanel customerPanel;
    private SupplierPanel supplierPanel;
    private EmployeePanel employeePanel;
    private ExpensePanel expensePanel;
    private ReportPanel reportPanel;
    private UserManagementPanel userManagementPanel;

    private CardLayout cardLayout;
    private JPanel contentContainer;
    private final Map<String, JButton> navButtons = new HashMap<>();
    private String activeNav = "DASHBOARD";

    public MainFrame(User currentUser, AuthService authService, Runnable onLogoutCallback) {
        super("Inventory and Business Management System - Enterprise Edition");
        this.currentUser = currentUser;
        this.authService = authService;
        this.onLogoutCallback = onLogoutCallback;

        // Initialize Services
        this.productService = new ProductService();
        this.categoryService = new CategoryService();
        this.salesService = new SalesService();
        this.purchaseService = new PurchaseService();
        this.inventoryService = new InventoryService();
        this.customerService = new CustomerService();
        this.supplierService = new SupplierService();
        this.employeeService = new EmployeeService();
        this.expenseService = new ExpenseService();
        this.reportService = new ReportService();
        this.userService = new UserService();

        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1340, 840);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Left Sidebar Navigation
        JPanel sidebar = buildSidebar();
        add(sidebar, BorderLayout.WEST);

        // Center Content with CardLayout
        cardLayout = new CardLayout();
        contentContainer = new JPanel(cardLayout);
        contentContainer.setBackground(Theme.APP_BG);

        // Instantiate modular panels
        dashboardPanel = new DashboardPanel(reportService, currentUser);
        salesPanel = new SalesPanel(salesService, productService, customerService, currentUser);
        productPanel = new ProductPanel(productService, categoryService);
        categoryPanel = new CategoryPanel(categoryService);
        purchasePanel = new PurchasePanel(purchaseService, supplierService, productService, currentUser);
        inventoryPanel = new InventoryPanel(inventoryService, productService, currentUser);
        customerPanel = new CustomerPanel(customerService);
        supplierPanel = new SupplierPanel(supplierService);
        employeePanel = new EmployeePanel(employeeService, currentUser);
        expensePanel = new ExpensePanel(expenseService, currentUser);
        reportPanel = new ReportPanel(reportService);
        userManagementPanel = new UserManagementPanel(userService, currentUser);

        // Add to CardLayout
        contentContainer.add(dashboardPanel, "DASHBOARD");
        contentContainer.add(salesPanel, "SALES");
        contentContainer.add(productPanel, "PRODUCTS");
        contentContainer.add(categoryPanel, "CATEGORIES");
        contentContainer.add(purchasePanel, "PURCHASES");
        contentContainer.add(inventoryPanel, "INVENTORY");
        contentContainer.add(customerPanel, "CUSTOMERS");
        contentContainer.add(supplierPanel, "SUPPLIERS");
        contentContainer.add(employeePanel, "EMPLOYEES");
        contentContainer.add(expensePanel, "EXPENSES");
        contentContainer.add(reportPanel, "REPORTS");
        contentContainer.add(userManagementPanel, "USERS");

        add(contentContainer, BorderLayout.CENTER);

        // Activate Default
        switchNav("DASHBOARD");
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBackground(Theme.SIDEBAR_BG);

        // Header / Branding
        JPanel brandPanel = new JPanel();
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setBackground(Theme.SIDEBAR_BG);
        brandPanel.setBorder(new EmptyBorder(22, 18, 18, 18));

        JLabel appTitle = new JLabel("AGY INVENTORY");
        appTitle.setFont(Theme.FONT_TITLE_LARGE);
        appTitle.setForeground(Color.WHITE);
        appTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel appSub = new JLabel("Enterprise Edition v2.0");
        appSub.setFont(Theme.FONT_SMALL);
        appSub.setForeground(Theme.TEXT_MUTED);
        appSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        brandPanel.add(appTitle);
        brandPanel.add(Box.createVerticalStrut(4));
        brandPanel.add(appSub);
        brandPanel.add(Box.createVerticalStrut(14));

        // User badge card
        JPanel userBadge = new JPanel(new BorderLayout(8, 0));
        userBadge.setBackground(new Color(30, 41, 59));
        userBadge.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(51, 65, 85), 1),
                new EmptyBorder(8, 10, 8, 10)
        ));

        JLabel avatar = new JLabel(currentUser.isAdmin() ? "👑" : "👤");
        avatar.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));

        JPanel userText = new JPanel(new GridLayout(0, 1, 2, 2));
        userText.setOpaque(false);

        JLabel nameLbl = new JLabel(currentUser.getFullName());
        nameLbl.setFont(Theme.FONT_BODY_BOLD);
        nameLbl.setForeground(Color.WHITE);

        JLabel roleLbl = new JLabel(currentUser.getRoleName() != null ? currentUser.getRoleName() : (currentUser.isAdmin() ? "ADMINISTRATOR" : "STAFF"));
        roleLbl.setFont(Theme.FONT_SMALL);
        roleLbl.setForeground(currentUser.isAdmin() ? new Color(251, 191, 36) : new Color(147, 197, 253));

        userText.add(nameLbl);
        userText.add(roleLbl);

        userBadge.add(avatar, BorderLayout.WEST);
        userBadge.add(userText, BorderLayout.CENTER);
        userBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandPanel.add(userBadge);

        sidebar.add(brandPanel, BorderLayout.NORTH);

        // Menu items
        JPanel menuContainer = new JPanel();
        menuContainer.setLayout(new BoxLayout(menuContainer, BoxLayout.Y_AXIS));
        menuContainer.setBackground(Theme.SIDEBAR_BG);
        menuContainer.setBorder(new EmptyBorder(8, 10, 8, 10));

        addNavButton(menuContainer, "DASHBOARD", "📊  Dashboard");
        addNavButton(menuContainer, "SALES", "🛒  POS & Sales Counter");
        addNavButton(menuContainer, "PRODUCTS", "📦  Products & Catalog");
        addNavButton(menuContainer, "CATEGORIES", "🗂  Categories");
        addNavButton(menuContainer, "PURCHASES", "📥  Purchases & POs");
        addNavButton(menuContainer, "INVENTORY", "📋  Inventory & Audits");
        addNavButton(menuContainer, "CUSTOMERS", "👥  Customer Directory");
        addNavButton(menuContainer, "SUPPLIERS", "🚚  Suppliers & Vendors");
        addNavButton(menuContainer, "EMPLOYEES", "💼  Employee Records");
        addNavButton(menuContainer, "EXPENSES", "💳  Operating Expenses");
        addNavButton(menuContainer, "REPORTS", "📈  Financial Reports");

        if (currentUser.isAdmin()) {
            addNavButton(menuContainer, "USERS", "⚙  User Administration");
        }

        JScrollPane menuScroll = new JScrollPane(menuContainer);
        menuScroll.setBorder(null);
        menuScroll.setOpaque(false);
        menuScroll.getViewport().setOpaque(false);
        sidebar.add(menuScroll, BorderLayout.CENTER);

        // Footer buttons: Change Password & Logout
        JPanel footer = new JPanel(new GridLayout(2, 1, 6, 6));
        footer.setBackground(Theme.SIDEBAR_BG);
        footer.setBorder(new EmptyBorder(12, 14, 16, 14));

        JButton passBtn = new JButton("🔑 Change Password");
        styleSidebarActionBtn(passBtn, new Color(51, 65, 85));
        passBtn.addActionListener(e -> showChangePasswordDialog());

        JButton logoutBtn = new JButton("🚪 Log Out");
        styleSidebarActionBtn(logoutBtn, new Color(153, 27, 27));
        logoutBtn.addActionListener(e -> handleLogout());

        footer.add(passBtn);
        footer.add(logoutBtn);
        sidebar.add(footer, BorderLayout.SOUTH);

        return sidebar;
    }

    private void styleSidebarActionBtn(JButton btn, Color bg) {
        btn.setFont(Theme.FONT_BODY);
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(bg.brighter(), 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void addNavButton(JPanel container, String key, String title) {
        JButton btn = new JButton(title);
        btn.setFont(Theme.FONT_BODY);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setPreferredSize(new Dimension(230, 40));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);

        updateNavButtonStyle(btn, false);

        btn.addActionListener(e -> switchNav(key));
        container.add(btn);
        container.add(Box.createVerticalStrut(4));
        navButtons.put(key, btn);
    }

    private void updateNavButtonStyle(JButton btn, boolean active) {
        if (active) {
            btn.setBackground(Theme.PRIMARY);
            btn.setForeground(Color.WHITE);
            btn.setFont(Theme.FONT_BODY_BOLD);
        } else {
            btn.setBackground(Theme.SIDEBAR_BG);
            btn.setForeground(new Color(203, 213, 225));
            btn.setFont(Theme.FONT_BODY);
        }
    }

    public void switchNav(String key) {
        activeNav = key;
        cardLayout.show(contentContainer, key);

        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            updateNavButtonStyle(entry.getValue(), entry.getKey().equals(key));
        }

        // Trigger dynamic refresh on the displayed view
        switch (key) {
            case "DASHBOARD":
                dashboardPanel.refreshData();
                break;
            case "PRODUCTS":
                productPanel.refreshTable();
                break;
            case "CATEGORIES":
                categoryPanel.refreshTable();
                break;
            case "SALES":
                salesPanel.refreshTable();
                break;
            case "PURCHASES":
                purchasePanel.refreshTable();
                break;
            case "INVENTORY":
                inventoryPanel.refreshAll();
                break;
            case "CUSTOMERS":
                customerPanel.refreshTable();
                break;
            case "SUPPLIERS":
                supplierPanel.refreshTable();
                break;
            case "EMPLOYEES":
                employeePanel.refreshTable();
                break;
            case "EXPENSES":
                expensePanel.refreshTable();
                break;
            case "USERS":
                if (currentUser.isAdmin()) {
                    userManagementPanel.refreshTable();
                }
                break;
        }
    }

    private void showChangePasswordDialog() {
        JPasswordField oldPass = new JPasswordField(15);
        JPasswordField newPass = new JPasswordField(15);
        JPasswordField confirmPass = new JPasswordField(15);

        JPanel p = new JPanel(new GridLayout(0, 1, 6, 6));
        p.add(new JLabel("Current Password:"));
        p.add(oldPass);
        p.add(new JLabel("New Password (min 6 characters):"));
        p.add(newPass);
        p.add(new JLabel("Confirm New Password:"));
        p.add(confirmPass);

        int res = JOptionPane.showConfirmDialog(this, p, "Change Account Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String oldP = new String(oldPass.getPassword());
            String newP = new String(newPass.getPassword());
            String confP = new String(confirmPass.getPassword());

            if (!newP.equals(confP)) {
                JOptionPane.showMessageDialog(this, "New passwords do not match.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                boolean ok = authService.changePassword(oldP, newP);
                if (ok) {
                    JOptionPane.showMessageDialog(this, "Password successfully updated with PBKDF2WithHmacSHA256!", "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to change password: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to log out of your session?", "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            authService.logout();
            dispose();
            if (onLogoutCallback != null) {
                onLogoutCallback.run();
            }
        }
    }
}

