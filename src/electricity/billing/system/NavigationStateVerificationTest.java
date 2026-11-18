package electricity.billing.system;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class NavigationStateVerificationTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;
    private static final List<String> results = new ArrayList<>();

    private static void check(String description, boolean condition) {
        if (condition) {
            testsPassed++;
            results.add("  [PASS] " + description);
            System.out.println("  [PASS] " + description);
        } else {
            testsFailed++;
            results.add("  [FAIL] " + description);
            System.out.println("  [FAIL] " + description);
        }
    }

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("  GLOBAL SIDEBAR ACTIVE NAVIGATION STATE VERIFICATION SUITE   ");
        System.out.println("===============================================================\n");

        try {
            testAdminInitialState();
            testCustomerInitialState();
            testAdminAllSectionsOpenClose();
            testCustomerAllSectionsOpenClose();
            testSectionSwitching();
            testClickDashboardNavigation();
            testQuickActionsHighlighting();
            testRepeatedOpenCloseCycles();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("\n===============================================================");
        System.out.println("  VERIFICATION RESULT: " + testsPassed + " PASSED, " + testsFailed + " FAILED");
        System.out.println("===============================================================");

        if (testsFailed > 0) {
            System.exit(1);
        } else {
            System.exit(0);
        }
    }

    private static void testAdminInitialState() {
        System.out.println(">>> Test 1: Admin Initial Navigation State");
        UserSession.setSession(new SessionContext("admin", "Admin", ""));
        Project admin = new Project("Admin", "");
        try {
            check("Admin: Initial active navigation is 'Dashboard'", "Dashboard".equalsIgnoreCase(admin.getActiveNavigation()));
            check("Admin: Dashboard button is active", admin.isCommandActive("Dashboard", "Dashboard"));
            check("Admin: Customer Management is NOT active at startup", !admin.isCommandActive("Customer Management", "Customer Management"));
            check("Admin: Customer Details is NOT active at startup", !admin.isCommandActive("Customer Details", "Customer Details"));
        } finally {
            admin.dispose();
        }
    }

    private static void testCustomerInitialState() {
        System.out.println("\n>>> Test 2: Customer Initial Navigation State");
        UserSession.setSession(new SessionContext("customer", "Customer", "MET001"));
        Project customer = new Project("Customer", "MET001");
        try {
            check("Customer: Initial active navigation is 'Dashboard'", "Dashboard".equalsIgnoreCase(customer.getActiveNavigation()));
            check("Customer: Dashboard button is active", customer.isCommandActive("Dashboard", "Dashboard"));
            check("Customer: View Information is NOT active at startup", !customer.isCommandActive("View Information", "View Information"));
            check("Customer: Pay Bill is NOT active at startup", !customer.isCommandActive("Pay Bill", "Pay Bill"));
        } finally {
            customer.dispose();
        }
    }

    private static void testAdminAllSectionsOpenClose() {
        System.out.println("\n>>> Test 3: Admin All Existing Sections Open & Close Behavior");
        Project admin = new Project("Admin", "");
        try {
            String[] adminSections = {
                "Customer Management",
                "Customer Details",
                "Meter Information",
                "Deposit Details",
                "Calculate Bill",
                "Complaint Management",
                "Complaint History",
                "Message Centre",
                "Analytics & Reports",
                "Audit Logs"
            };

            for (String section : adminSections) {
                // 1. Trigger section open
                admin.actionPerformed(new ActionEvent(admin, ActionEvent.ACTION_PERFORMED, section));
                check("Admin: Opening '" + section + "' highlights correct item", admin.isCommandActive(section, section));
                check("Admin: Opening '" + section + "' removes highlight from Dashboard", !admin.isCommandActive("Dashboard", "Dashboard"));
                
                // Verify window tracked
                JFrame subWin = admin.getActiveSubWindow();
                check("Admin: Sub-window created for '" + section + "'", subWin != null && subWin.isDisplayable());

                // 2. Close the sub-window
                if (subWin != null) {
                    subWin.dispose();
                }
                
                // Allow event queue dispatch
                try { Thread.sleep(50); } catch (Exception ignored) {}

                // Verify return to Dashboard
                check("Admin: Closing '" + section + "' returns highlight to Dashboard", "Dashboard".equalsIgnoreCase(admin.getActiveNavigation()));
                check("Admin: Closing '" + section + "' removes highlight from '" + section + "'", !admin.isCommandActive(section, section));
            }
        } finally {
            admin.dispose();
        }
    }

    private static void testCustomerAllSectionsOpenClose() {
        System.out.println("\n>>> Test 4: Customer All Existing Sections Open & Close Behavior");
        UserSession.setSession(new SessionContext("customer", "Customer", "MET001"));
        Project customer = new Project("Customer", "MET001");
        try {
            String[] customerSections = {
                "View Information",
                "Update Information",
                "Pay Bill",
                "Bill Details",
                "Generate Bill",
                "Submit Complaint",
                "My Complaints",
                "Notifications"
            };

            for (String section : customerSections) {
                // 1. Trigger section open
                customer.actionPerformed(new ActionEvent(customer, ActionEvent.ACTION_PERFORMED, section));
                check("Customer: Opening '" + section + "' highlights correct item", customer.isCommandActive(section, section));
                check("Customer: Opening '" + section + "' removes highlight from Dashboard", !customer.isCommandActive("Dashboard", "Dashboard"));
                
                // Verify window tracked
                JFrame subWin = customer.getActiveSubWindow();
                check("Customer: Sub-window created for '" + section + "'", subWin != null && subWin.isDisplayable());

                // 2. Close the sub-window
                if (subWin != null) {
                    subWin.dispose();
                }
                
                // Allow event queue dispatch
                try { Thread.sleep(50); } catch (Exception ignored) {}

                // Verify return to Dashboard
                check("Customer: Closing '" + section + "' returns highlight to Dashboard", "Dashboard".equalsIgnoreCase(customer.getActiveNavigation()));
                check("Customer: Closing '" + section + "' removes highlight from '" + section + "'", !customer.isCommandActive(section, section));
            }
        } finally {
            customer.dispose();
        }
    }

    private static void testSectionSwitching() {
        System.out.println("\n>>> Test 5: Switching Between Sections Directly");
        Project admin = new Project("Admin", "");
        try {
            // Open Customer Details
            admin.actionPerformed(new ActionEvent(admin, ActionEvent.ACTION_PERFORMED, "Customer Details"));
            check("Switching: 'Customer Details' opened and active", admin.isCommandActive("Customer Details", "Customer Details"));
            JFrame win1 = admin.getActiveSubWindow();
            check("Switching: Window 1 displayable", win1 != null && win1.isDisplayable());

            // Switch to Calculate Bill directly
            admin.actionPerformed(new ActionEvent(admin, ActionEvent.ACTION_PERFORMED, "Calculate Bill"));
            check("Switching: 'Calculate Bill' now active", admin.isCommandActive("Calculate Bill", "Calculate Bill"));
            check("Switching: 'Customer Details' previous highlight removed", !admin.isCommandActive("Customer Details", "Customer Details"));
            check("Switching: Window 1 was disposed cleanly", win1 != null && !win1.isDisplayable());
            JFrame win2 = admin.getActiveSubWindow();
            check("Switching: Window 2 displayable", win2 != null && win2.isDisplayable());

            // Close Window 2
            win2.dispose();
            try { Thread.sleep(50); } catch (Exception ignored) {}
            check("Switching: Closing window returns highlight to Dashboard", "Dashboard".equalsIgnoreCase(admin.getActiveNavigation()));
        } finally {
            admin.dispose();
        }
    }

    private static void testClickDashboardNavigation() {
        System.out.println("\n>>> Test 6: Clicking Dashboard Sidebar Item Directly");
        Project admin = new Project("Admin", "");
        try {
            // Open a section
            admin.actionPerformed(new ActionEvent(admin, ActionEvent.ACTION_PERFORMED, "Meter Information"));
            check("Click Dashboard: 'Meter Information' active", admin.isCommandActive("Meter Information", "Meter Information"));
            JFrame subWin = admin.getActiveSubWindow();
            check("Click Dashboard: Sub-window is displayable", subWin != null && subWin.isDisplayable());

            // Click Dashboard item
            admin.actionPerformed(new ActionEvent(admin, ActionEvent.ACTION_PERFORMED, "Dashboard"));
            check("Click Dashboard: Active is now 'Dashboard'", "Dashboard".equalsIgnoreCase(admin.getActiveNavigation()));
            check("Click Dashboard: Sub-window was closed", subWin != null && !subWin.isDisplayable());
            check("Click Dashboard: Previous section highlight removed", !admin.isCommandActive("Meter Information", "Meter Information"));
        } finally {
            admin.dispose();
        }
    }

    private static void testQuickActionsHighlighting() {
        System.out.println("\n>>> Test 7: Quick Actions Triggering Consistent Sidebar Highlights");
        Project admin = new Project("Admin", "");
        try {
            // New Customer Quick Action (dispatches "New Customer")
            admin.actionPerformed(new ActionEvent(admin, ActionEvent.ACTION_PERFORMED, "New Customer"));
            check("Quick Action: 'New Customer' highlights 'Customer Management' sidebar item", 
                admin.isCommandActive("Customer Management", "Customer Management"));
            JFrame win = admin.getActiveSubWindow();
            if (win != null) win.dispose();
            try { Thread.sleep(50); } catch (Exception ignored) {}
            check("Quick Action: Closed returns to Dashboard", "Dashboard".equalsIgnoreCase(admin.getActiveNavigation()));
        } finally {
            admin.dispose();
        }
    }

    private static void testRepeatedOpenCloseCycles() {
        System.out.println("\n>>> Test 8: Repeated Opening and Closing Cycles");
        Project admin = new Project("Admin", "");
        try {
            for (int i = 1; i <= 5; i++) {
                admin.actionPerformed(new ActionEvent(admin, ActionEvent.ACTION_PERFORMED, "Audit Logs"));
                boolean activeAudit = admin.isCommandActive("Audit Logs", "Audit Logs");
                JFrame win = admin.getActiveSubWindow();
                if (win != null) win.dispose();
                try { Thread.sleep(30); } catch (Exception ignored) {}
                boolean activeDash = "Dashboard".equalsIgnoreCase(admin.getActiveNavigation());
                if (!activeAudit || !activeDash) {
                    check("Repeated Cycle #" + i + " failed", false);
                    return;
                }
            }
            check("Repeated Cycles: 5 consecutive open/close cycles leave no stale highlights", true);
        } finally {
            admin.dispose();
        }
    }
}
