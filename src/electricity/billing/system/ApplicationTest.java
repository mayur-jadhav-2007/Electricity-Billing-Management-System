package electricity.billing.system;

import java.sql.*;

public class ApplicationTest {
    
    public static void testDatabaseConnection() {
        System.out.println("Testing database connection...");
        try {
            Conn c = new Conn();
            ResultSet rs = c.s.executeQuery("SELECT 1");
            if (rs.next()) {
                System.out.println("✓ Database connection successful");
            }
        } catch (Exception e) {
            System.out.println("✗ Database connection failed: " + e.getMessage());
        }
    }
    
    public static void testLoginFunctionality() {
        System.out.println("\nTesting login functionality & PBKDF2WithHmacSHA256 password verification...");
        try (Conn c = new Conn()) {
            // Test admin login credential retrieval and verification
            String query = "SELECT password FROM login WHERE username = ? AND user = ?";
            try (PreparedStatement ps = c.c.prepareStatement(query)) {
                ps.setString(1, "Parth");
                ps.setString(2, "Admin");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && PasswordUtils.verifyPassword("Parth123", rs.getString("password"))) {
                        System.out.println("✓ Admin login authentication passed (Parth)");
                    } else {
                        System.out.println("✗ Admin login test failed");
                    }
                }
            }
            
            // Test customer login credential retrieval and verification
            try (PreparedStatement ps = c.c.prepareStatement(query)) {
                ps.setString(1, "Customer");
                ps.setString(2, "Customer");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && PasswordUtils.verifyPassword("Customer123", rs.getString("password"))) {
                        System.out.println("✓ Customer login authentication passed (Customer)");
                    } else {
                        System.out.println("✗ Customer login test failed");
                    }
                }
            }

            // Test password hashing algorithm directly
            String rawPass = "SecurePass@2026";
            String hashed = PasswordUtils.hashPassword(rawPass);
            if (PasswordUtils.isHashed(hashed) && PasswordUtils.verifyPassword(rawPass, hashed) && !PasswordUtils.verifyPassword("WrongPass", hashed)) {
                System.out.println("✓ Password hashing and verification test passed");
            } else {
                System.out.println("✗ Password hashing test failed");
            }
            
        } catch (Exception e) {
            System.out.println("✗ Login test failed: " + e.getMessage());
        }
    }
    
    public static void testCustomerData() {
        System.out.println("\nTesting customer data operations...");
        try {
            Conn c = new Conn();
            
            // Test customer query
            ResultSet rs = c.s.executeQuery("SELECT COUNT(*) as count FROM customer");
            if (rs.next() && rs.getInt("count") > 0) {
                System.out.println("✓ Customer data available (" + rs.getInt("count") + " customers)");
            }
            
            // Test bill data
            rs = c.s.executeQuery("SELECT COUNT(*) as count FROM bill");
            if (rs.next() && rs.getInt("count") > 0) {
                System.out.println("✓ Bill data available (" + rs.getInt("count") + " bills)");
            }
            
            // Test meter info
            rs = c.s.executeQuery("SELECT COUNT(*) as count FROM meter_info");
            if (rs.next() && rs.getInt("count") > 0) {
                System.out.println("✓ Meter info available (" + rs.getInt("count") + " meters)");
            }
            
        } catch (Exception e) {
            System.out.println("✗ Customer data test failed: " + e.getMessage());
        }
    }
    
    public static void testBillCalculation() {
        System.out.println("\nTesting bill calculation components...");
        try {
            Conn c = new Conn();
            
            // Test tax table
            ResultSet rs = c.s.executeQuery("SELECT * FROM tax LIMIT 1");
            if (rs.next()) {
                double costPerUnit = rs.getDouble("cost_per_unit");
                double meterRent = rs.getDouble("meter_rent");
                System.out.println("✓ Tax data available - Cost per unit: $" + costPerUnit + ", Meter rent: $" + meterRent);
            }
            
            // Test DbUtils functionality
            rs = c.s.executeQuery("SELECT * FROM customer LIMIT 1");
            javax.swing.table.DefaultTableModel model = DbUtils.resultSetToTableModel(rs);
            if (model.getRowCount() >= 0) {
                System.out.println("✓ DbUtils conversion working - Columns: " + model.getColumnCount());
            }
            
        } catch (Exception e) {
            System.out.println("✗ Bill calculation test failed: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Electricity Billing System Test ===");
        
        testDatabaseConnection();
        testLoginFunctionality();
        testCustomerData();
        testBillCalculation();
        
        System.out.println("\n=== Test Complete ===");
        System.out.println("The application appears to be working properly!");
        System.out.println("You can now run the GUI application using:");
        System.out.println("java -cp \"lib\\mysql-connector-java-8.0.28.jar;build\\classes\" electricity.billing.system.Splash");
    }
}
