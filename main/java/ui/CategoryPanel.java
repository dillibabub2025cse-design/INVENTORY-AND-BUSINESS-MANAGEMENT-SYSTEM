package ui;

import exception.AppException;
import model.Category;
import service.CategoryService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Modern Category Management panel.
 */
public class CategoryPanel extends JPanel {

    private final CategoryService categoryService;
    private JTable categoryTable;
    private DefaultTableModel tableModel;

    public CategoryPanel(CategoryService categoryService) {
        this.categoryService = categoryService;
        initUI();
        refreshTable();
    }

    private void initUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(Theme.APP_BG);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel title = new JLabel("Category Hierarchy Management");
        title.setFont(Theme.FONT_TITLE_LARGE);
        title.setForeground(Theme.TEXT_PRIMARY);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionButtons.setOpaque(false);

        JButton addBtn = Theme.createPrimaryButton("+ Add Category");
        addBtn.addActionListener(e -> showAddCategoryDialog());

        JButton editBtn = Theme.createSecondaryButton("✏ Edit");
        editBtn.addActionListener(e -> showEditCategoryDialog());

        JButton deleteBtn = Theme.createDangerButton("🗑 Delete");
        deleteBtn.addActionListener(e -> handleDeleteCategory());

        JButton refreshBtn = Theme.createSecondaryButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> refreshTable());

        actionButtons.add(addBtn);
        actionButtons.add(editBtn);
        actionButtons.add(deleteBtn);
        actionButtons.add(refreshBtn);

        headerRow.add(title, BorderLayout.WEST);
        headerRow.add(actionButtons, BorderLayout.EAST);
        add(headerRow, BorderLayout.NORTH);

        String[] columns = {"ID", "Category Name", "Description"};
        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        categoryTable = new JTable(tableModel);
        Theme.styleTable(categoryTable);
        categoryTable.getColumnModel().getColumn(0).setMaxWidth(80);

        JScrollPane scroll = new JScrollPane(categoryTable);
        scroll.setBorder(new LineBorder(Theme.BORDER, 1));
        add(scroll, BorderLayout.CENTER);
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        List<Category> list = categoryService.getAllCategories();
        for (Category c : list) {
            tableModel.addRow(new Object[]{c.getId(), c.getName(), c.getDescription()});
        }
    }

    private void showAddCategoryDialog() {
        JTextField nameField = Theme.createTextField(20);
        JTextField descField = Theme.createTextField(20);

        JPanel p = new JPanel(new GridLayout(0, 1, 6, 6));
        p.add(new JLabel("Category Name:"));
        p.add(nameField);
        p.add(new JLabel("Description:"));
        p.add(descField);

        int res = JOptionPane.showConfirmDialog(this, p, "Create Category", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            try {
                categoryService.addCategory(nameField.getText().trim(), descField.getText().trim());
                JOptionPane.showMessageDialog(this, "Category created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshTable();
            } catch (AppException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditCategoryDialog() {
        int row = categoryTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a category to edit.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) categoryTable.getValueAt(row, 0);
        Category c = categoryService.getCategoryById(id);

        JTextField nameField = Theme.createTextField(20);
        nameField.setText(c.getName());
        JTextField descField = Theme.createTextField(20);
        descField.setText(c.getDescription());

        JPanel p = new JPanel(new GridLayout(0, 1, 6, 6));
        p.add(new JLabel("Category Name:"));
        p.add(nameField);
        p.add(new JLabel("Description:"));
        p.add(descField);

        int res = JOptionPane.showConfirmDialog(this, p, "Edit Category #" + id, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            try {
                categoryService.updateCategory(id, nameField.getText().trim(), descField.getText().trim());
                JOptionPane.showMessageDialog(this, "Category updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshTable();
            } catch (AppException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleDeleteCategory() {
        int row = categoryTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a category to delete.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) categoryTable.getValueAt(row, 0);
        String name = (String) categoryTable.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this, "Delete Category '" + name + "'?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                categoryService.deleteCategory(id);
                JOptionPane.showMessageDialog(this, "Category deleted.", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshTable();
            } catch (AppException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot Delete", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

