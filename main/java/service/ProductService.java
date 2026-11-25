package service;

import dao.CategoryDAO;
import dao.ProductDAO;
import exception.ValidationException;
import model.Product;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service managing product catalog, pricing rules, stock validations, and category assignments.
 */
public class ProductService {

    private final ProductDAO productDAO;
    private final CategoryDAO categoryDAO;

    public ProductService() {
        this.productDAO = new ProductDAO();
        this.categoryDAO = new CategoryDAO();
    }

    public ProductService(ProductDAO productDAO, CategoryDAO categoryDAO) {
        this.productDAO = productDAO;
        this.categoryDAO = categoryDAO;
    }

    public List<Product> getAllProducts() {
        return productDAO.findAll();
    }

    public Product getProductById(int id) {
        Product p = productDAO.findById(id);
        if (p == null) {
            throw new ValidationException("Product with ID " + id + " not found.");
        }
        return p;
    }

    public Product getProductByBarcode(String barcode) {
        return productDAO.findByBarcode(barcode);
    }

    public List<Product> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllProducts();
        }
        return productDAO.search(keyword.trim());
    }

    public List<Product> getLowStockProducts() {
        return productDAO.findLowStock();
    }

    public List<Product> getOutOfStockProducts() {
        return productDAO.findOutOfStock();
    }

    public boolean addProduct(Product product) {
        validateProduct(product, true);
        return productDAO.create(product);
    }

    public boolean updateProduct(Product product) {
        validateProduct(product, false);
        return productDAO.update(product);
    }

    public boolean deleteProduct(int productId) {
        Product existing = getProductById(productId);
        if (productDAO.hasTransactions(productId)) {
            throw new ValidationException("Cannot delete product '" + existing.getName() + 
                    "': It has associated sales or purchase order records. Deactivate it instead.");
        }
        return productDAO.delete(productId);
    }

    private void validateProduct(Product p, boolean isNew) {
        if (p.getName() == null || p.getName().trim().isEmpty()) {
            throw new ValidationException("Product name cannot be empty.");
        }
        if (p.getBarcode() == null || p.getBarcode().trim().isEmpty()) {
            throw new ValidationException("Product barcode/SKU cannot be empty.");
        }

        // Check category existence
        if (categoryDAO.findById(p.getCategoryId()) == null) {
            throw new ValidationException("Category ID " + p.getCategoryId() + " does not exist.");
        }

        // Check barcode uniqueness
        Product existingByBarcode = productDAO.findByBarcode(p.getBarcode().trim());
        if (existingByBarcode != null) {
            if (isNew || existingByBarcode.getId() != p.getId()) {
                throw new ValidationException("A product with barcode '" + p.getBarcode() + "' already exists (" + existingByBarcode.getName() + ").");
            }
        }

        if (p.getPurchasePrice() == null || p.getPurchasePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Purchase price must be greater than or equal to 0.00.");
        }
        if (p.getSellingPrice() == null || p.getSellingPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Selling price must be greater than or equal to 0.00.");
        }
        if (p.getSellingPrice().compareTo(p.getPurchasePrice()) < 0) {
            throw new ValidationException("Selling price (" + p.getSellingPrice() + 
                    ") cannot be less than purchase cost (" + p.getPurchasePrice() + ").");
        }
        if (p.getCurrentStock() < 0) {
            throw new ValidationException("Initial stock cannot be negative.");
        }
        if (p.getMinStockLevel() < 0) {
            throw new ValidationException("Minimum stock level cannot be negative.");
        }
    }
}

