package service;

import dao.CategoryDAO;
import exception.ValidationException;
import model.Category;

import java.util.List;

/**
 * Service managing product categories and dependency validations.
 */
public class CategoryService {

    private final CategoryDAO categoryDAO;

    public CategoryService() {
        this.categoryDAO = new CategoryDAO();
    }

    public CategoryService(CategoryDAO categoryDAO) {
        this.categoryDAO = categoryDAO;
    }

    public List<Category> getAllCategories() {
        return categoryDAO.findAll();
    }

    public Category getCategoryById(int id) {
        Category cat = categoryDAO.findById(id);
        if (cat == null) {
            throw new ValidationException("Category with ID " + id + " does not exist.");
        }
        return cat;
    }

    public boolean addCategory(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Category name cannot be empty.");
        }
        if (categoryDAO.findByName(name.trim()) != null) {
            throw new ValidationException("Category with name '" + name.trim() + "' already exists.");
        }
        Category cat = new Category();
        cat.setName(name.trim());
        cat.setDescription(description != null ? description.trim() : "");
        return categoryDAO.create(cat);
    }

    public boolean updateCategory(int id, String name, String description) {
        Category existing = getCategoryById(id);
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Category name cannot be empty.");
        }
        Category withName = categoryDAO.findByName(name.trim());
        if (withName != null && withName.getId() != id) {
            throw new ValidationException("Another category already has the name '" + name.trim() + "'.");
        }
        existing.setName(name.trim());
        existing.setDescription(description != null ? description.trim() : "");
        return categoryDAO.update(existing);
    }

    public boolean deleteCategory(int id) {
        getCategoryById(id);
        if (categoryDAO.hasProducts(id)) {
            throw new ValidationException("Cannot delete Category ID " + id + ": Products are currently assigned to it. Reassign products first.");
        }
        return categoryDAO.delete(id);
    }
}

