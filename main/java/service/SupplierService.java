package service;

import dao.SupplierDAO;
import exception.ValidationException;
import model.Supplier;

import java.util.List;

/**
 * Service managing Supplier profiles, validation, and purchase history links.
 */
public class SupplierService {

    private final SupplierDAO supplierDAO;

    public SupplierService() {
        this.supplierDAO = new SupplierDAO();
    }

    public SupplierService(SupplierDAO supplierDAO) {
        this.supplierDAO = supplierDAO;
    }

    public List<Supplier> getAllSuppliers() {
        return supplierDAO.findAll();
    }

    public Supplier getSupplierById(int id) {
        Supplier s = supplierDAO.findById(id);
        if (s == null) {
            throw new ValidationException("Supplier with ID " + id + " does not exist.");
        }
        return s;
    }

    public List<Supplier> searchSuppliers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllSuppliers();
        }
        return supplierDAO.search(keyword.trim());
    }

    public boolean addSupplier(Supplier supplier) {
        validateSupplier(supplier);
        return supplierDAO.create(supplier);
    }

    public boolean updateSupplier(Supplier supplier) {
        validateSupplier(supplier);
        return supplierDAO.update(supplier);
    }

    public boolean deleteSupplier(int id) {
        getSupplierById(id);
        if (supplierDAO.hasPurchases(id)) {
            throw new ValidationException("Cannot delete Supplier ID " + id + ": Purchase order history exists for this supplier.");
        }
        return supplierDAO.delete(id);
    }

    private void validateSupplier(Supplier s) {
        if (s.getCompanyName() == null || s.getCompanyName().trim().isEmpty()) {
            throw new ValidationException("Supplier company name is required.");
        }
        if (s.getContactName() == null || s.getContactName().trim().isEmpty()) {
            throw new ValidationException("Contact person name is required.");
        }
        if (s.getPhone() == null || s.getPhone().trim().isEmpty()) {
            throw new ValidationException("Supplier contact phone is required.");
        }
    }
}

