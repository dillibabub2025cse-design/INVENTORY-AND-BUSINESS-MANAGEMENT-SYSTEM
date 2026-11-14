-- =====================================================================
-- DATABASE: INVENTORY AND BUSINESS MANAGEMENT SYSTEM (MySQL 8.x)
-- =====================================================================

CREATE DATABASE IF NOT EXISTS inventory_business_db 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE inventory_business_db;

-- ---------------------------------------------------------------------
-- 1. Table: roles
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. Table: users
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role_id INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id) ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. Table: employees
-- ---------------------------------------------------------------------
CREATE TABLE employees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    department VARCHAR(50) NOT NULL,
    designation VARCHAR(50) NOT NULL,
    salary DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    hire_date DATE NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE', 'TERMINATED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_employee_salary CHECK (salary >= 0.00),
    CONSTRAINT fk_employees_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 4. Table: categories
-- ---------------------------------------------------------------------
CREATE TABLE categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 5. Table: products
-- ---------------------------------------------------------------------
CREATE TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    barcode VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    category_id INT NOT NULL,
    description TEXT,
    purchase_price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    selling_price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    current_stock INT NOT NULL DEFAULT 0,
    min_stock_level INT NOT NULL DEFAULT 5,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_prod_purchase_price CHECK (purchase_price >= 0.00),
    CONSTRAINT chk_prod_selling_price CHECK (selling_price >= 0.00),
    CONSTRAINT chk_prod_stock CHECK (current_stock >= 0),
    CONSTRAINT chk_prod_min_stock CHECK (min_stock_level >= 0),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id) ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 6. Table: suppliers
-- ---------------------------------------------------------------------
CREATE TABLE suppliers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(150) NOT NULL,
    contact_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(25) NOT NULL,
    address TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 7. Table: customers
-- ---------------------------------------------------------------------
CREATE TABLE customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(25) NOT NULL,
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 8. Table: purchases
-- ---------------------------------------------------------------------
CREATE TABLE purchases (
    id INT AUTO_INCREMENT PRIMARY KEY,
    purchase_number VARCHAR(50) NOT NULL UNIQUE,
    supplier_id INT NOT NULL,
    user_id INT NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    status ENUM('COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'COMPLETED',
    notes TEXT,
    purchase_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_purchase_total CHECK (total_amount >= 0.00),
    CONSTRAINT fk_purchases_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON UPDATE CASCADE,
    CONSTRAINT fk_purchases_user FOREIGN KEY (user_id) REFERENCES users(id) ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 9. Table: purchase_items
-- ---------------------------------------------------------------------
CREATE TABLE purchase_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    purchase_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    unit_cost DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    CONSTRAINT chk_pi_quantity CHECK (quantity > 0),
    CONSTRAINT chk_pi_unit_cost CHECK (unit_cost >= 0.00),
    CONSTRAINT chk_pi_subtotal CHECK (subtotal >= 0.00),
    CONSTRAINT fk_pi_purchase FOREIGN KEY (purchase_id) REFERENCES purchases(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_pi_product FOREIGN KEY (product_id) REFERENCES products(id) ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 10. Table: sales
-- ---------------------------------------------------------------------
CREATE TABLE sales (
    id INT AUTO_INCREMENT PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id INT NULL,
    user_id INT NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    payment_method ENUM('CASH', 'CARD', 'ONLINE', 'OTHER') NOT NULL DEFAULT 'CASH',
    status ENUM('COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'COMPLETED',
    sale_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_sales_subtotal CHECK (subtotal >= 0.00),
    CONSTRAINT chk_sales_discount CHECK (discount_amount >= 0.00),
    CONSTRAINT chk_sales_tax CHECK (tax_amount >= 0.00),
    CONSTRAINT chk_sales_total CHECK (total_amount >= 0.00),
    CONSTRAINT fk_sales_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_sales_user FOREIGN KEY (user_id) REFERENCES users(id) ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 11. Table: sale_items
-- ---------------------------------------------------------------------
CREATE TABLE sale_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sale_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    cost_price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    subtotal DECIMAL(12,2) NOT NULL,
    CONSTRAINT chk_si_quantity CHECK (quantity > 0),
    CONSTRAINT chk_si_unit_price CHECK (unit_price >= 0.00),
    CONSTRAINT chk_si_cost_price CHECK (cost_price >= 0.00),
    CONSTRAINT chk_si_subtotal CHECK (subtotal >= 0.00),
    CONSTRAINT fk_si_sale FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_si_product FOREIGN KEY (product_id) REFERENCES products(id) ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 12. Table: stock_movements
-- ---------------------------------------------------------------------
CREATE TABLE stock_movements (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    movement_type ENUM('PURCHASE', 'SALE', 'PURCHASE_CANCELLED', 'SALE_CANCELLED', 'MANUAL_IN', 'MANUAL_OUT', 'ADJUSTMENT') NOT NULL,
    quantity INT NOT NULL,
    previous_stock INT NOT NULL,
    new_stock INT NOT NULL,
    reference_id VARCHAR(50),
    notes TEXT,
    created_by INT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sm_product FOREIGN KEY (product_id) REFERENCES products(id) ON UPDATE CASCADE,
    CONSTRAINT fk_sm_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 13. Table: expense_categories
-- ---------------------------------------------------------------------
CREATE TABLE expense_categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 14. Table: expenses
-- ---------------------------------------------------------------------
CREATE TABLE expenses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    category_id INT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    expense_date DATE NOT NULL,
    notes TEXT,
    user_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_expense_amount CHECK (amount > 0.00),
    CONSTRAINT fk_expenses_category FOREIGN KEY (category_id) REFERENCES expense_categories(id) ON UPDATE CASCADE,
    CONSTRAINT fk_expenses_user FOREIGN KEY (user_id) REFERENCES users(id) ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- INDEXES FOR PERFORMANCE OPTIMIZATION
-- ---------------------------------------------------------------------
CREATE INDEX idx_products_barcode ON products(barcode);
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_purchases_date ON purchases(purchase_date);
CREATE INDEX idx_purchases_supplier ON purchases(supplier_id);
CREATE INDEX idx_sales_date ON sales(sale_date);
CREATE INDEX idx_sales_customer ON sales(customer_id);
CREATE INDEX idx_stock_movements_prod ON stock_movements(product_id);
CREATE INDEX idx_stock_movements_date ON stock_movements(created_at);
CREATE INDEX idx_expenses_date ON expenses(expense_date);

-- =====================================================================
-- INITIAL SEED DATA
-- =====================================================================

-- 1. Roles
INSERT INTO roles (id, role_name, description) VALUES
(1, 'ADMIN', 'System Administrator with full access to all modules and configurations'),
(2, 'STAFF', 'Operational Staff with access to sales, customer management, inventory view, and purchases');

-- 2. Initial Admin and Staff Users
-- Password for admin: admin123 (PBKDF2WithHmacSHA256 65,536 iterations)
-- Password for staff: staff123 (PBKDF2WithHmacSHA256 65,536 iterations)
INSERT INTO users (id, username, password_hash, salt, full_name, email, role_id, is_active) VALUES
(1, 'admin', '69b933ab584ab1f051aba0978826d4ad5784d19c157c3d9d0d7b341b09e46cfa', '75c970657c015979f422dc0bbcae0612', 'System Administrator', 'admin@business.com', 1, TRUE),
(2, 'staff', '81f46a63436f03f5aeff516132b74818a86abcc8d40642640b4caa3710187216', 'c311e2ea2d20b22c7b35858fda5b6f4e', 'Operations Staff', 'staff@business.com', 2, TRUE);

-- 3. Initial Employees
INSERT INTO employees (id, user_id, first_name, last_name, email, phone, department, designation, salary, hire_date, status) VALUES
(1, 1, 'Robert', 'Taylor', 'admin@business.com', '+1-555-0101', 'Management', 'Operations Director', 85000.00, '2023-01-15', 'ACTIVE'),
(2, 2, 'Emily', 'Clark', 'staff@business.com', '+1-555-0102', 'Sales & Inventory', 'Store Supervisor', 42000.00, '2023-06-01', 'ACTIVE'),
(3, NULL, 'David', 'Miller', 'david.m@business.com', '+1-555-0103', 'Logistics', 'Warehouse Specialist', 36000.00, '2024-02-10', 'ACTIVE');

-- 4. Product Categories
INSERT INTO categories (id, name, description) VALUES
(1, 'Electronics & Gadgets', 'Computing hardware, peripherals, smart gadgets, and consumer electronics'),
(2, 'Office Supplies', 'Paper, pens, desk organizers, filing systems, and stationery'),
(3, 'Furniture & Storage', 'Ergonomic chairs, desks, filing cabinets, and office storage'),
(4, 'Networking & Cables', 'Routers, switches, ethernet cabling, adapters, and patch cords'),
(5, 'Safety & Maintenance', 'Cleaning supplies, protective equipment, and basic facility maintenance');

-- 5. Products
INSERT INTO products (id, barcode, name, category_id, description, purchase_price, selling_price, current_stock, min_stock_level, is_active) VALUES
(1, '890100100001', 'Logitech MX Master 3S Wireless Mouse', 1, 'Ergonomic high-precision bluetooth mouse with quiet clicks', 65.00, 99.99, 45, 10, TRUE),
(2, '890100100002', 'Dell UltraSharp 27" 4K Monitor', 1, 'IPS panel, USB-C hub, 99% sRGB coverage', 380.00, 520.00, 18, 5, TRUE),
(3, '890100100003', 'Keychron K2 Mechanical Keyboard', 1, '75% layout, Gateron brown switches, RGB wireless', 55.00, 89.50, 30, 8, TRUE),
(4, '890100100004', 'Anker 65W GaN Fast Charger', 1, 'Compact 3-port fast wall charger with PowerIQ 3.0', 22.00, 39.99, 50, 15, TRUE),
(5, '890100200001', 'A4 Premium Copy Paper 500 Sheets', 2, 'High-brightness 80gsm white printing paper', 4.50, 8.00, 120, 25, TRUE),
(6, '890100200002', 'Pilot G2 Gel Roller Pens (Pack of 12)', 2, 'Smooth writing black ink 0.7mm gel pens', 9.00, 16.50, 80, 20, TRUE),
(7, '890100300001', 'Ergonomic Mesh Executive Chair', 3, 'Breathable mesh back with lumbar support and 3D armrests', 140.00, 230.00, 12, 4, TRUE),
(8, '890100400001', 'Cat6 Ethernet Cable 15m (Blue)', 4, 'Snagless RJ45 high-speed gigabit network cable', 5.00, 12.00, 65, 15, TRUE),
(9, '890100400002', 'TP-Link 8-Port Gigabit Desktop Switch', 4, 'Unmanaged plug and play steel casing network switch', 16.00, 28.00, 22, 6, TRUE),
(10, '890100500001', 'Microfiber Cleaning Cloths (10-pack)', 5, 'Non-abrasive microfiber cloths for screens and surfaces', 4.00, 8.50, 4, 10, TRUE);

-- 6. Suppliers
INSERT INTO suppliers (id, company_name, contact_name, email, phone, address, is_active) VALUES
(1, 'TechSupply Global Distributors', 'Marcus Vance', 'orders@techsupplyglobal.com', '+1-800-555-0199', '100 Silicon Way, Tech Park, San Jose, CA', TRUE),
(2, 'Apex Office & Paper Mart', 'Sarah Jenkins', 'sales@apexoffice.com', '+1-800-555-0288', '45 Industrial Ave, Suite 300, Chicago, IL', TRUE),
(3, 'ErgoWorks Commercial Solutions', 'Liam Gallagher', 'wholesale@ergoworks.com', '+1-800-555-0377', '890 Industrial Parkway, Grand Rapids, MI', TRUE);

-- 7. Customers
INSERT INTO customers (id, name, email, phone, address) VALUES
(1, 'Acme Innovations LLC', 'procurement@acme.io', '+1-415-555-0144', '500 Market St, Floor 12, San Francisco, CA'),
(2, 'Vertex Digital Studio', 'accounts@vertexstudio.design', '+1-212-555-0182', '120 Broadway, New York, NY'),
(3, 'Nexus Enterprise Solutions', 'purchasing@nexusent.com', '+1-312-555-0167', '233 S Wacker Dr, Chicago, IL'),
(4, 'Walk-in Retail Customer', 'walkin@store.local', '+1-000-000-0000', 'Local Store Front');

-- 8. Sample Purchases
INSERT INTO purchases (id, purchase_number, supplier_id, user_id, total_amount, status, notes, purchase_date) VALUES
(1, 'PO-20261001-001', 1, 1, 2925.00, 'COMPLETED', 'Initial inventory stock for Logitech mice and Anker chargers', '2026-10-01 09:30:00'),
(2, 'PO-20261010-002', 2, 1, 1260.00, 'COMPLETED', 'Restock paper reams and pilot gel pens', '2026-10-10 11:15:00');

-- 9. Sample Purchase Items
INSERT INTO purchase_items (id, purchase_id, product_id, quantity, unit_cost, subtotal) VALUES
(1, 1, 1, 35, 65.00, 2275.00),
(2, 1, 4, 25, 22.00, 550.00),
(3, 2, 5, 100, 4.50, 450.00),
(4, 2, 6, 90, 9.00, 810.00);

-- 10. Sample Sales
INSERT INTO sales (id, invoice_number, customer_id, user_id, subtotal, discount_amount, tax_amount, total_amount, payment_method, status, sale_date) VALUES
(1, 'INV-20261015-001', 1, 2, 899.90, 45.00, 68.39, 923.29, 'CARD', 'COMPLETED', '2026-10-15 14:20:00'),
(2, 'INV-20261020-002', 2, 2, 1040.00, 0.00, 83.20, 1123.20, 'ONLINE', 'COMPLETED', '2026-10-20 16:45:00');

-- 11. Sample Sale Items
INSERT INTO sale_items (id, sale_id, product_id, quantity, unit_price, cost_price, subtotal) VALUES
(1, 1, 1, 5, 99.99, 65.00, 499.95),
(2, 1, 4, 10, 39.99, 22.00, 399.95),
(3, 2, 2, 2, 520.00, 380.00, 1040.00);

-- 12. Sample Stock Movements
INSERT INTO stock_movements (id, product_id, movement_type, quantity, previous_stock, new_stock, reference_id, notes, created_by, created_at) VALUES
(1, 1, 'PURCHASE', 35, 15, 50, 'PO-20261001-001', 'Received purchase order PO-20261001-001', 1, '2026-10-01 09:30:00'),
(2, 4, 'PURCHASE', 25, 35, 60, 'PO-20261001-001', 'Received purchase order PO-20261001-001', 1, '2026-10-01 09:30:00'),
(3, 1, 'SALE', -5, 50, 45, 'INV-20261015-001', 'Sale completed for invoice INV-20261015-001', 2, '2026-10-15 14:20:00'),
(4, 4, 'SALE', -10, 60, 50, 'INV-20261015-001', 'Sale completed for invoice INV-20261015-001', 2, '2026-10-15 14:20:00'),
(5, 2, 'SALE', -2, 20, 18, 'INV-20261020-002', 'Sale completed for invoice INV-20261020-002', 2, '2026-10-20 16:45:00');

-- 13. Expense Categories
INSERT INTO expense_categories (id, name, description) VALUES
(1, 'Office Rent & Facilities', 'Lease, shared building amenities, and property charges'),
(2, 'Utilities & Telecom', 'Electricity, water, fiber internet, phone lines, cloud hosting'),
(3, 'Salaries & Payroll', 'Staff remuneration, contractor fees, and benefits'),
(4, 'Marketing & Advertising', 'Online ads, brochures, trade fairs, client meetings'),
(5, 'Logistics & Packaging', 'Courier, shipping, corrugated boxes, tape, freight fees'),
(6, 'Repairs & Maintenance', 'Equipment servicing, office repairs, software license renewals');

-- 14. Sample Expenses
INSERT INTO expenses (id, title, category_id, amount, expense_date, notes, user_id) VALUES
(1, 'October Warehouse Lease', 1, 1800.00, '2026-10-01', 'Monthly facility rental fee for storage space', 1),
(2, 'High-Speed Fiber Internet', 2, 120.00, '2026-10-05', 'Commercial gigabit internet connection bill', 1),
(3, 'Shipping Supplies & Tape', 5, 85.50, '2026-10-12', 'Boxes, bubble wrap, and packing tape restock', 2);

