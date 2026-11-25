package model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * Model representing a Business Expense.
 */
public class Expense {
    private int id;
    private String title;
    private int categoryId;
    private String categoryName;
    private BigDecimal amount;
    private Date expenseDate;
    private String notes;
    private int userId;
    private String userName;
    private Timestamp createdAt;

    public Expense() {}

    public Expense(int id, String title, int categoryId, String categoryName, BigDecimal amount, 
                   Date expenseDate, String notes, int userId, String userName) {
        this.id = id;
        this.title = title;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.amount = amount;
        this.expenseDate = expenseDate;
        this.notes = notes;
        this.userId = userId;
        this.userName = userName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Date getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(Date expenseDate) {
        this.expenseDate = expenseDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}

