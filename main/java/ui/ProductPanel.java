package ui;

import exception.AppException;
import model.Category;
import model.Product;
import service.CategoryService;
import service.ProductService;
import util.ConsoleFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Modern product catalog and stock inventory management panel.
 */
public class ProductPanel extends JPanel {

    private final ProductService productService;
    private final CategoryService categoryService;

    private JTable productTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> categoryFilterBox;
    private JRadioButton radAll, radLow, radOut;

    public ProductPanel(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
        initUI();
        refreshTable();
    }

    private void initUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(Theme.APP_BG);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        // Top Toolbar & Filters
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel title = new JLabel("Product Catalog & Stock Management");
        title.setFont(Theme.FONT_TITLE_LARGE);
        title.setForeground(Theme.TEXT_PRIMARY);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionButtons.setOpaque(false);

        JButton addBtn = Theme.createPrimaryButton("+ Add Product");
        addBtn.addActionListener(e -> showAddProductDialog());

        JButton editBtn = Theme.createSecondaryButton("✏ Edit Details");
        editBtn.addActionListener(e -> showEditProductDialog());

        JButton deleteBtn = Theme.createDangerButton("🗑 Delete");
        deleteBtn.addActionListener(e -> handleDeleteProduct());

        JButton refreshBtn = Theme.createSecondaryButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> refreshTable());

        actionButtons.add(addBtn);
        actionButtons.add(editBtn);
        actionButtons.add(deleteBtn);
        actionButtons.add(refreshBtn);

        headerRow.add(title, BorderLayout.WEST);
        headerRow.add(actionButtons, BorderLayout.EAST);

        // Filter Bar
        JPanel filterRow = Theme.createCardPanel();
        filterRow.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 6));

        searchField = Theme.createTextField(16);
        searchField.putClientProperty("JTextField.placeholderText", "Search name, barcode, SKU...");
        searchField.addActionListener(e -> filterData());

        JButton searchBtn = Theme.createSecondaryButton("Search");
        searchBtn.addActionListener(e -> filterData());

        categoryFilterBox = new JComboBox<>();
        categoryFilterBox.setFont(Theme.FONT_BODY);
        categoryFilterBox.addItem("All Categories");
        categoryFilterBox.addActionListener(e -> filterData());

        radAll = new JRadioButton("All Stock", true);
        radLow = new JRadioButton("Low Stock Only");
        radOut = new JRadioButton("Out of Stock Only");
        radAll.setOpaque(false);
        radLow.setOpaque(false);
        radOut.setOpaque(false);

        ButtonGroup bg = new ButtonGroup();
        bg.add(radAll);
        bg.add(radLow);
        bg.add(radOut);

        radAll.addActionListener(e -> filterData());
        radLow.addActionListener(e -> filterData());
        radOut.addActionListener(e -> filterData());

        filterRow.add(new JLabel("Search:"));
        filterRow.add(searchField);
        filterRow.add(searchBtn);
        filterRow.add(Box.createHorizontalStrut(10));
        filterRow.add(new JLabel("Category:"));
        filterRow.add(categoryFilterBox);
        filterRow.add(Box.createHorizontalStrut(10));
        filterRow.add(radAll);
        filterRow.add(radLow);
        filterRow.add(radOut);

        topContainer.add(headerRow);
        topContainer.add(Box.createVerticalStrut(12));
        topContainer.add(filterRow);

        add(topContainer, BorderLayout.NORTH);

        // Table Panel
        String[] columns = {"ID", "Barcode / SKU", "Product Name", "Category", "Cost Price", "Selling Price", "Stock Qty", "Min Alert", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };

        productTable = new JTable(tableModel);
        Theme.styleTable(productTable);

        // Custom Renderer for Status column
        productTable.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(CENTER);
                String val = (String) value;
                if ("OUT OF STOCK".equals(val)) {
                    l.setForeground(Theme.DANGER);
                    l.setFont(Theme.FONT_BODY_BOLD);
                } else if ("LOW STOCK".equals(val)) {
                    l.setForeground(Theme.WARNING);
                    l.setFont(Theme.FONT_BODY_BOLD);
                } else {
                    l.setForeground(Theme.SUCCESS);
                    l.setFont(Theme.FONT_BODY_BOLD);
                }
                return l;
            }
        });

        JScrollPane scrollPane = new JScrollPane(productTable);
        scrollPane.setBorder(new LineBorder(Theme.BORDER, 1));
        add(scrollPane, BorderLayout.CENTER);
    }

    public void refreshTable() {
        // Refresh category filter box items
        categoryFilterBox.removeAllItems();
        categoryFilterBox.addItem("All Categories");
        List<Category> cats = categoryService.getAllCategories();
        for (Category c : cats) {
            categoryFilterBox.addItem(c.getName());
        }

        filterData();
    }

    private void filterData() {
        tableModel.setRowCount(0);
        String keyword = searchField.getText().trim();

        List<Product> list;
        if (radLow.isSelected()) {
            list = productService.getLowStockProducts();
        } else if (radOut.isSelected()) {
            list = productService.getOutOfStockProducts();
        } else if (!keyword.isEmpty()) {
            list = productService.searchProducts(keyword);
        } else {
            list = productService.getAllProducts();
        }

        String selectedCat = (String) categoryFilterBox.getSelectedItem();
        for (Product p : list) {
            if (selectedCat != null && !selectedCat.equals("All Categories") && !selectedCat.equalsIgnoreCase(p.getCategoryName())) {
                continue;
            }
            tableModel.addRow(new Object[]{
                    p.getId(),
                    p.getBarcode(),
                    p.getName(),
                    p.getCategoryName(),
                    ConsoleFormatter.formatCurrency(p.getPurchasePrice()),
                    ConsoleFormatter.formatCurrency(p.getSellingPrice()),
                    p.getCurrentStock(),
                    p.getMinStockLevel(),
                    p.getStockStatus()
            });
        }
    }

    private void showAddProductDialog() {
        List<Category> categories = categoryService.getAllCategories();
        if (categories.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please create at least one category before adding products.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField nameField = Theme.createTextField(20);
        JTextField barcodeField = Theme.createTextField(20);
        JComboBox<Category> catBox = new JComboBox<>(categories.toArray(new Category[0]));
        JTextField costField = Theme.createTextField(10);
        JTextField priceField = Theme.createTextField(10);
        JTextField stockField = Theme.createTextField(10);
        JTextField minStockField = Theme.createTextField(10);
        minStockField.setText("5");
        JTextField descField = Theme.createTextField(20);

        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.add(new JLabel("Product Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Barcode / SKU:"));
        panel.add(barcodeField);
        panel.add(new JLabel("Category:"));
        panel.add(catBox);
        panel.add(new JLabel("Purchase Cost (₹):"));
        panel.add(costField);
        panel.add(new JLabel("Selling Price (₹):"));
        panel.add(priceField);
        panel.add(new JLabel("Initial Stock:"));
        panel.add(stockField);
        panel.add(new JLabel("Min Alert Stock:"));
        panel.add(minStockField);
        panel.add(new JLabel("Description:"));
        panel.add(descField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add New Product", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String name = nameField.getText().trim();
                String barcode = barcodeField.getText().trim();
                Category cat = (Category) catBox.getSelectedItem();
                BigDecimal cost = new BigDecimal(costField.getText().trim());
                BigDecimal price = new BigDecimal(priceField.getText().trim());
                int stock = Integer.parseInt(stockField.getText().trim());
                int minStock = Integer.parseInt(minStockField.getText().trim());
                String desc = descField.getText().trim();

                Product p = new Product(0, barcode, name, cat.getId(), desc, cost, price, stock, minStock, true);
                productService.addProduct(p);
                JOptionPane.showMessageDialog(this, "Product added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error adding product: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditProductDialog() {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a product from the table to edit.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int productId = (int) productTable.getValueAt(selectedRow, 0);
        Product p = productService.getProductById(productId);

        List<Category> categories = categoryService.getAllCategories();
        JTextField nameField = Theme.createTextField(20);
        nameField.setText(p.getName());
        JTextField barcodeField = Theme.createTextField(20);
        barcodeField.setText(p.getBarcode());

        JComboBox<Category> catBox = new JComboBox<>(categories.toArray(new Category[0]));
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId() == p.getCategoryId()) {
                catBox.setSelectedIndex(i);
                break;
            }
        }

        JTextField costField = Theme.createTextField(10);
        costField.setText(p.getPurchasePrice().toString());
        JTextField priceField = Theme.createTextField(10);
        priceField.setText(p.getSellingPrice().toString());
        JTextField minStockField = Theme.createTextField(10);
        minStockField.setText(String.valueOf(p.getMinStockLevel()));
        JTextField descField = Theme.createTextField(20);
        descField.setText(p.getDescription());

        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.add(new JLabel("Product Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Barcode / SKU:"));
        panel.add(barcodeField);
        panel.add(new JLabel("Category:"));
        panel.add(catBox);
        panel.add(new JLabel("Purchase Cost (₹):"));
        panel.add(costField);
        panel.add(new JLabel("Selling Price (₹):"));
        panel.add(priceField);
        panel.add(new JLabel("Min Alert Stock:"));
        panel.add(minStockField);
        panel.add(new JLabel("Description:"));
        panel.add(descField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Product #" + p.getId(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                p.setName(nameField.getText().trim());
                p.setBarcode(barcodeField.getText().trim());
                Category cat = (Category) catBox.getSelectedItem();
                p.setCategoryId(cat.getId());
                p.setPurchasePrice(new BigDecimal(costField.getText().trim()));
                p.setSellingPrice(new BigDecimal(priceField.getText().trim()));
                p.setMinStockLevel(Integer.parseInt(minStockField.getText().trim()));
                p.setDescription(descField.getText().trim());

                productService.updateProduct(p);
                JOptionPane.showMessageDialog(this, "Product updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error updating product: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleDeleteProduct() {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a product to delete.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int productId = (int) productTable.getValueAt(selectedRow, 0);
        String name = (String) productTable.getValueAt(selectedRow, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete product '" + name + "' (ID: " + productId + ")?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                productService.deleteProduct(productId);
                JOptionPane.showMessageDialog(this, "Product deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshTable();
            } catch (AppException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot Delete", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

