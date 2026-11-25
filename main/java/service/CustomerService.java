package service;

import dao.CustomerDAO;
import dao.SalesDAO;
import exception.ValidationException;
import model.Customer;
import model.Sale;

import java.util.List;

/**
 * Service managing Customer registrations, contact records, and sales history.
 */
public class CustomerService {

    private final CustomerDAO customerDAO;
    private final SalesDAO salesDAO;

    public CustomerService() {
        this.customerDAO = new CustomerDAO();
        this.salesDAO = new SalesDAO();
    }

    public CustomerService(CustomerDAO customerDAO, SalesDAO salesDAO) {
        this.customerDAO = customerDAO;
        this.salesDAO = salesDAO;
    }

    public List<Customer> getAllCustomers() {
        return customerDAO.findAll();
    }

    public Customer getCustomerById(int id) {
        Customer c = customerDAO.findById(id);
        if (c == null) {
            throw new ValidationException("Customer with ID " + id + " does not exist.");
        }
        return c;
    }

    public List<Customer> searchCustomers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllCustomers();
        }
        return customerDAO.search(keyword.trim());
    }

    public boolean addCustomer(Customer customer) {
        validateCustomer(customer);
        return customerDAO.create(customer);
    }

    public boolean updateCustomer(Customer customer) {
        validateCustomer(customer);
        return customerDAO.update(customer);
    }

    public boolean deleteCustomer(int id) {
        getCustomerById(id);
        if (customerDAO.hasSales(id)) {
            throw new ValidationException("Cannot delete Customer ID " + id + ": Customer has sales order history.");
        }
        return customerDAO.delete(id);
    }

    public List<Sale> getCustomerSalesHistory(int customerId) {
        getCustomerById(customerId);
        return salesDAO.getCustomerPurchaseHistory(customerId);
    }

    private void validateCustomer(Customer c) {
        if (c.getName() == null || c.getName().trim().isEmpty()) {
            throw new ValidationException("Customer name cannot be empty.");
        }
        if (c.getPhone() == null || c.getPhone().trim().isEmpty()) {
            throw new ValidationException("Customer phone number cannot be empty.");
        }
    }
}

