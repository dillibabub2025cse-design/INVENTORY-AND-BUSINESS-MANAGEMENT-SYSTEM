package service;

import dao.ExpenseDAO;
import exception.ValidationException;
import model.Expense;
import model.ExpenseCategory;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;

/**
 * Service managing Business Expenses and category tracking.
 */
public class ExpenseService {

    private final ExpenseDAO expenseDAO;

    public ExpenseService() {
        this.expenseDAO = new ExpenseDAO();
    }

    public ExpenseService(ExpenseDAO expenseDAO) {
        this.expenseDAO = expenseDAO;
    }

    public List<Expense> getAllExpenses() {
        return expenseDAO.findAll();
    }

    public Expense getExpenseById(int id) {
        Expense e = expenseDAO.findById(id);
        if (e == null) {
            throw new ValidationException("Expense with ID " + id + " does not exist.");
        }
        return e;
    }

    public List<Expense> getExpensesByDateRange(Date start, Date end) {
        if (start.after(end)) {
            throw new ValidationException("Start date cannot be after end date.");
        }
        return expenseDAO.findByDateRange(start, end);
    }

    public List<ExpenseCategory> getAllCategories() {
        return expenseDAO.findAllCategories();
    }

    public boolean addCategory(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Category name cannot be empty.");
        }
        return expenseDAO.createCategory(name.trim(), description);
    }

    public boolean recordExpense(Expense expense) {
        validateExpense(expense);
        return expenseDAO.create(expense);
    }

    public boolean updateExpense(Expense expense) {
        validateExpense(expense);
        return expenseDAO.update(expense);
    }

    public boolean deleteExpense(int id) {
        getExpenseById(id);
        return expenseDAO.delete(id);
    }

    public BigDecimal getTotalExpenses(Date start, Date end) {
        return expenseDAO.getTotalExpenses(start, end);
    }

    private void validateExpense(Expense e) {
        if (e.getTitle() == null || e.getTitle().trim().isEmpty()) {
            throw new ValidationException("Expense title is required.");
        }
        if (e.getAmount() == null || e.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Expense amount must be greater than 0.00.");
        }
        if (e.getExpenseDate() == null) {
            throw new ValidationException("Expense date is required.");
        }
    }
}

