package electricity.billing.system;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class ComplaintService {

    public static final String[] CATEGORIES = {
        "Billing Issues",
        "Meter Problems",
        "Payment Issues",
        "Electricity Supply Problems",
        "New Connection Issues",
        "Account and Profile Issues",
        "Other"
    };

    public static final String[] PRIORITIES = {
        "Low",
        "Medium",
        "High",
        "Urgent"
    };

    public static final String[] STATUSES = {
        "Pending",
        "In Progress",
        "Resolved",
        "Rejected"
    };

    public static class ServiceResult {
        public final boolean success;
        public final String errorCode;
        public final String message;
        public final String referenceNumber;
        public final int complaintId;

        public ServiceResult(boolean success, String errorCode, String message, String referenceNumber, int complaintId) {
            this.success = success;
            this.errorCode = errorCode;
            this.message = message;
            this.referenceNumber = referenceNumber;
            this.complaintId = complaintId;
        }

        public static ServiceResult ok(String message, String ref, int id) {
            return new ServiceResult(true, "SUCCESS", message, ref, id);
        }

        public static ServiceResult fail(String errorCode, String message) {
            return new ServiceResult(false, errorCode, message, null, -1);
        }
    }

    public static boolean isValidCategory(String category) {
        if (category == null) return false;
        for (String cat : CATEGORIES) {
            if (cat.equalsIgnoreCase(category.trim())) return true;
        }
        return false;
    }

    public static boolean isValidPriority(String priority) {
        if (priority == null) return false;
        for (String pri : PRIORITIES) {
            if (pri.equalsIgnoreCase(priority.trim())) return true;
        }
        return false;
    }

    public static boolean isValidStatus(String status) {
        if (status == null) return false;
        for (String st : STATUSES) {
            if (st.equalsIgnoreCase(status.trim())) return true;
        }
        return false;
    }

    /**
     * Generates a unique complaint reference number in format: CMP-yyyyMMdd-XXXX
     */
    public static synchronized String generateUniqueReference(Connection c) throws SQLException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());
        Random random = new Random();

        for (int attempts = 0; attempts < 100; attempts++) {
            int seq = 1000 + random.nextInt(9000);
            String ref = "CMP-" + dateStr + "-" + seq;
            String checkSql = "SELECT COUNT(*) FROM complaints WHERE reference_number = ?";
            try (PreparedStatement ps = c.prepareStatement(checkSql)) {
                ps.setString(1, ref);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        return ref;
                    }
                }
            }
        }
        // Fallback with timestamp suffix
        return "CMP-" + dateStr + "-" + (System.currentTimeMillis() % 100000);
    }

    /**
     * Submits a customer complaint inside an atomic transaction.
     * Inserts into `complaints` and initial entry into `complaint_history`.
     */
    public static ServiceResult submitComplaint(String meterNo, String category, String subject, 
                                                 String description, String billMonth, String customerIdentifier) {
        // 1. Input validations
        if (meterNo == null || meterNo.trim().isEmpty()) {
            return ServiceResult.fail("INVALID_METER", "Meter number is required.");
        }
        meterNo = meterNo.trim();

        if (category == null || category.trim().isEmpty() || !isValidCategory(category)) {
            return ServiceResult.fail("INVALID_CATEGORY", "Please select a valid complaint category.");
        }
        category = category.trim();

        if (subject == null || subject.trim().isEmpty()) {
            return ServiceResult.fail("EMPTY_SUBJECT", "Complaint subject cannot be empty.");
        }
        subject = subject.trim();
        if (subject.length() > 200) {
            return ServiceResult.fail("SUBJECT_TOO_LONG", "Subject cannot exceed 200 characters.");
        }

        if (description == null || description.trim().isEmpty()) {
            return ServiceResult.fail("EMPTY_DESCRIPTION", "Complaint description cannot be empty.");
        }
        description = description.trim();
        if (description.length() > 2000) {
            return ServiceResult.fail("DESCRIPTION_TOO_LONG", "Description cannot exceed 2000 characters.");
        }

        String cleanBillMonth = (billMonth != null && !billMonth.trim().isEmpty() && !billMonth.equalsIgnoreCase("None") && !billMonth.equalsIgnoreCase("Not Applicable")) ? billMonth.trim() : null;

        // 2. Validate customer meter exists and customer is active
        try (Conn conn = new Conn()) {
            if (conn.c == null) {
                return ServiceResult.fail("DATABASE_ERROR", "Database connection failed.");
            }

            String checkCustomerSql = "SELECT status FROM customer WHERE meter_no = ?";
            try (PreparedStatement ps = conn.c.prepareStatement(checkCustomerSql)) {
                ps.setString(1, meterNo);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return ServiceResult.fail("INVALID_METER", "Meter number " + meterNo + " does not exist.");
                    }
                    String custStatus = rs.getString("status");
                    if ("Inactive".equalsIgnoreCase(custStatus)) {
                        return ServiceResult.fail("CUSTOMER_INACTIVE", "Your account is currently inactive. You cannot submit new complaints.");
                    }
                }
            }

            // 3. Begin atomic transaction
            conn.c.setAutoCommit(false);
            try {
                String refNumber = generateUniqueReference(conn.c);

                String insertComplaintSql = 
                    "INSERT INTO complaints (reference_number, meter_no, category, subject, description, bill_month, priority, status, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, 'Medium', 'Pending', NOW(), NOW())";
                
                int complaintId = -1;
                try (PreparedStatement ps = conn.c.prepareStatement(insertComplaintSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, refNumber);
                    ps.setString(2, meterNo);
                    ps.setString(3, category);
                    ps.setString(4, subject);
                    ps.setString(5, description);
                    if (cleanBillMonth != null) {
                        ps.setString(6, cleanBillMonth);
                    } else {
                        ps.setNull(6, Types.VARCHAR);
                    }
                    ps.executeUpdate();

                    try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            complaintId = generatedKeys.getInt(1);
                        } else {
                            throw new SQLException("Creating complaint failed, no ID obtained.");
                        }
                    }
                }

                // Initial history record
                String insertHistorySql = 
                    "INSERT INTO complaint_history (complaint_id, action, previous_value, new_value, performed_by, remarks, created_at) " +
                    "VALUES (?, 'Complaint Submitted', NULL, 'Pending', ?, 'Initial customer complaint submission', NOW())";
                
                try (PreparedStatement ps = conn.c.prepareStatement(insertHistorySql)) {
                    ps.setInt(1, complaintId);
                    String actor = (customerIdentifier != null && !customerIdentifier.trim().isEmpty()) ? customerIdentifier.trim() : ("Customer (" + meterNo + ")");
                    ps.setString(2, actor);
                    ps.executeUpdate();
                }

                String actor = (customerIdentifier != null && !customerIdentifier.trim().isEmpty()) ? customerIdentifier.trim() : ("Customer (" + meterNo + ")");
                AuditService.logEvent(conn.c, actor, "Customer", AuditService.ACTION_COMPLAINT_SUBMITTED,
                    AuditService.ENTITY_COMPLAINT, refNumber,
                    "Submitted complaint " + refNumber + ": " + subject, null, "Pending");

                conn.c.commit();
                return ServiceResult.ok("Complaint submitted successfully.", refNumber, complaintId);
            } catch (Exception ex) {
                conn.c.rollback();
                ex.printStackTrace();
                return ServiceResult.fail("DATABASE_ERROR", "Transaction rolled back due to error: " + ex.getMessage());
            } finally {
                conn.c.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ServiceResult.fail("DATABASE_ERROR", "Error: " + e.getMessage());
        }
    }

    /**
     * Updates complaint status with validation of permitted transitions and mandatory remarks.
     */
    public static ServiceResult updateStatus(int complaintId, String targetStatus, String adminUsername, String remarks) {
        if (complaintId <= 0) {
            return ServiceResult.fail("INVALID_ID", "Invalid complaint ID.");
        }
        if (targetStatus == null || !isValidStatus(targetStatus)) {
            return ServiceResult.fail("INVALID_STATUS", "Invalid target status: " + targetStatus);
        }
        if (adminUsername == null || adminUsername.trim().isEmpty()) {
            adminUsername = "Admin";
        }
        adminUsername = adminUsername.trim();

        try (Conn conn = new Conn()) {
            if (conn.c == null) {
                return ServiceResult.fail("DATABASE_ERROR", "Database connection failed.");
            }

            // Fetch existing complaint record
            String currentStatus = null;
            String refNumber = null;
            String complaintMeterNo = null;
            String existingRemarks = null;

            String fetchSql = "SELECT reference_number, meter_no, status, resolution_remarks FROM complaints WHERE complaint_id = ?";
            try (PreparedStatement ps = conn.c.prepareStatement(fetchSql)) {
                ps.setInt(1, complaintId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        refNumber = rs.getString("reference_number");
                        complaintMeterNo = rs.getString("meter_no");
                        currentStatus = rs.getString("status");
                        existingRemarks = rs.getString("resolution_remarks");
                    } else {
                        return ServiceResult.fail("NOT_FOUND", "Complaint ID " + complaintId + " does not exist.");
                    }
                }
            }

            if (currentStatus.equalsIgnoreCase(targetStatus)) {
                return ServiceResult.fail("NO_CHANGE", "Complaint is already in status '" + targetStatus + "'.");
            }

            // Validate status transitions:
            // Allowed:
            // 1. Pending -> In Progress
            // 2. Pending -> Rejected (requires explanation)
            // 3. In Progress -> Resolved (requires resolution remarks)
            // 4. In Progress -> Rejected (requires explanation)
            // 5. Resolved -> In Progress (Reopen, requires explanation)
            // 6. Rejected -> In Progress (Reopen, requires explanation)

            String action = "Status Changed";
            String cleanRemarks = (remarks != null) ? remarks.trim() : "";

            if ("Pending".equalsIgnoreCase(currentStatus)) {
                if ("In Progress".equalsIgnoreCase(targetStatus)) {
                    action = "Status Changed";
                } else if ("Rejected".equalsIgnoreCase(targetStatus)) {
                    if (cleanRemarks.isEmpty()) {
                        return ServiceResult.fail("REMARKS_REQUIRED", "Rejection explanation remarks are mandatory when rejecting a complaint.");
                    }
                    action = "Complaint Rejected";
                } else if ("Resolved".equalsIgnoreCase(targetStatus)) {
                    return ServiceResult.fail("INVALID_TRANSITION", "Complaints in 'Pending' status must first be moved to 'In Progress' before being resolved.");
                }
            } else if ("In Progress".equalsIgnoreCase(currentStatus)) {
                if ("Resolved".equalsIgnoreCase(targetStatus)) {
                    if (cleanRemarks.isEmpty()) {
                        return ServiceResult.fail("REMARKS_REQUIRED", "Resolution remarks are mandatory when resolving a complaint.");
                    }
                    action = "Complaint Resolved";
                } else if ("Rejected".equalsIgnoreCase(targetStatus)) {
                    if (cleanRemarks.isEmpty()) {
                        return ServiceResult.fail("REMARKS_REQUIRED", "Rejection explanation remarks are mandatory when rejecting a complaint.");
                    }
                    action = "Complaint Rejected";
                } else if ("Pending".equalsIgnoreCase(targetStatus)) {
                    return ServiceResult.fail("INVALID_TRANSITION", "Cannot revert an in-progress complaint back to Pending.");
                }
            } else if ("Resolved".equalsIgnoreCase(currentStatus)) {
                if ("In Progress".equalsIgnoreCase(targetStatus)) {
                    if (cleanRemarks.isEmpty()) {
                        return ServiceResult.fail("REMARKS_REQUIRED", "An explanation is mandatory when reopening a resolved complaint.");
                    }
                    action = "Complaint Reopened";
                } else {
                    return ServiceResult.fail("INVALID_TRANSITION", "A resolved complaint can only be reopened to 'In Progress' with an explanation.");
                }
            } else if ("Rejected".equalsIgnoreCase(currentStatus)) {
                if ("In Progress".equalsIgnoreCase(targetStatus)) {
                    if (cleanRemarks.isEmpty()) {
                        return ServiceResult.fail("REMARKS_REQUIRED", "An explanation is mandatory when reopening a rejected complaint.");
                    }
                    action = "Complaint Reopened";
                } else {
                    return ServiceResult.fail("INVALID_TRANSITION", "A rejected complaint can only be reopened to 'In Progress' with an explanation.");
                }
            }

            // Execute update in transaction
            conn.c.setAutoCommit(false);
            try {
                String updateSql = "UPDATE complaints SET status = ?, resolution_remarks = ?, updated_at = NOW() WHERE complaint_id = ?";
                try (PreparedStatement ps = conn.c.prepareStatement(updateSql)) {
                    ps.setString(1, targetStatus);
                    if (!cleanRemarks.isEmpty()) {
                        ps.setString(2, cleanRemarks);
                    } else {
                        ps.setString(2, existingRemarks);
                    }
                    ps.setInt(3, complaintId);
                    ps.executeUpdate();
                }

                String insertHistorySql = 
                    "INSERT INTO complaint_history (complaint_id, action, previous_value, new_value, performed_by, remarks, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, NOW())";
                try (PreparedStatement ps = conn.c.prepareStatement(insertHistorySql)) {
                    ps.setInt(1, complaintId);
                    ps.setString(2, action);
                    ps.setString(3, currentStatus);
                    ps.setString(4, targetStatus);
                    ps.setString(5, adminUsername);
                    ps.setString(6, cleanRemarks.isEmpty() ? "Status updated to " + targetStatus : cleanRemarks);
                    ps.executeUpdate();
                }

                String actionType = "Complaint Reopened".equalsIgnoreCase(action) ?
                        AuditService.ACTION_COMPLAINT_REOPENED : AuditService.ACTION_COMPLAINT_STATUS_CHANGED;
                AuditService.logEvent(conn.c, adminUsername, "Admin", actionType,
                    AuditService.ENTITY_COMPLAINT, refNumber,
                    "Status changed from " + currentStatus + " to " + targetStatus, currentStatus, targetStatus);

                conn.c.commit();

                // Automatic in-app notification dispatch (Phase 3.6)
                if (complaintMeterNo != null && !complaintMeterNo.trim().isEmpty()) {
                    try {
                        String cleanMeter = complaintMeterNo.trim();
                        String priority = ("Resolved".equalsIgnoreCase(targetStatus) || "Rejected".equalsIgnoreCase(targetStatus))
                                ? NotificationService.PRIORITY_URGENT
                                : NotificationService.PRIORITY_IMPORTANT;
                        String notifTitle = "Complaint #" + refNumber + " Status: " + targetStatus;
                        StringBuilder bodyBuilder = new StringBuilder();
                        bodyBuilder.append("Dear Customer, the status of your complaint has been updated.\n")
                                .append("Reference Number: ").append(refNumber).append("\n")
                                .append("Previous Status: ").append(currentStatus).append("\n")
                                .append("Current Status: ").append(targetStatus).append("\n");
                        if (!cleanRemarks.isEmpty()) {
                            bodyBuilder.append("Remarks: ").append(cleanRemarks).append("\n");
                        }
                        bodyBuilder.append("You may review complaint tracking details in your customer portal.");

                        NotificationService.ServiceResult notifRes = NotificationService.sendSystemNotification(
                                cleanMeter, notifTitle, bodyBuilder.toString(), priority
                        );
                        if (!notifRes.isSuccess()) {
                            System.err.println("[NOTIFICATION WARNING] Automatic complaint notification failed for " 
                                    + refNumber + ": " + notifRes.getMessage());
                        }
                    } catch (Throwable notifEx) {
                        System.err.println("[NOTIFICATION ERROR] Exception dispatching automatic complaint notification: " + notifEx.getMessage());
                    }
                }

                return ServiceResult.ok("Complaint status updated to " + targetStatus + ".", refNumber, complaintId);
            } catch (Exception ex) {
                conn.c.rollback();
                ex.printStackTrace();
                return ServiceResult.fail("DATABASE_ERROR", "Transaction failed: " + ex.getMessage());
            } finally {
                conn.c.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ServiceResult.fail("DATABASE_ERROR", "Error: " + e.getMessage());
        }
    }

    /**
     * Updates complaint priority and records history.
     */
    public static ServiceResult updatePriority(int complaintId, String targetPriority, String adminUsername, String remarks) {
        if (complaintId <= 0) {
            return ServiceResult.fail("INVALID_ID", "Invalid complaint ID.");
        }
        if (targetPriority == null || !isValidPriority(targetPriority)) {
            return ServiceResult.fail("INVALID_PRIORITY", "Invalid priority: " + targetPriority);
        }
        if (adminUsername == null || adminUsername.trim().isEmpty()) {
            adminUsername = "Admin";
        }
        adminUsername = adminUsername.trim();

        try (Conn conn = new Conn()) {
            if (conn.c == null) {
                return ServiceResult.fail("DATABASE_ERROR", "Database connection failed.");
            }

            String currentPriority = null;
            String refNumber = null;

            String fetchSql = "SELECT reference_number, priority FROM complaints WHERE complaint_id = ?";
            try (PreparedStatement ps = conn.c.prepareStatement(fetchSql)) {
                ps.setInt(1, complaintId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        refNumber = rs.getString("reference_number");
                        currentPriority = rs.getString("priority");
                    } else {
                        return ServiceResult.fail("NOT_FOUND", "Complaint ID " + complaintId + " does not exist.");
                    }
                }
            }

            if (currentPriority.equalsIgnoreCase(targetPriority)) {
                return ServiceResult.fail("NO_CHANGE", "Priority is already " + targetPriority + ".");
            }

            conn.c.setAutoCommit(false);
            try {
                String updateSql = "UPDATE complaints SET priority = ?, updated_at = NOW() WHERE complaint_id = ?";
                try (PreparedStatement ps = conn.c.prepareStatement(updateSql)) {
                    ps.setString(1, targetPriority);
                    ps.setInt(2, complaintId);
                    ps.executeUpdate();
                }

                String cleanRemarks = (remarks != null && !remarks.trim().isEmpty()) ? remarks.trim() : ("Priority changed from " + currentPriority + " to " + targetPriority);

                String insertHistorySql = 
                    "INSERT INTO complaint_history (complaint_id, action, previous_value, new_value, performed_by, remarks, created_at) " +
                    "VALUES (?, 'Priority Changed', ?, ?, ?, ?, NOW())";
                try (PreparedStatement ps = conn.c.prepareStatement(insertHistorySql)) {
                    ps.setInt(1, complaintId);
                    ps.setString(2, currentPriority);
                    ps.setString(3, targetPriority);
                    ps.setString(4, adminUsername);
                    ps.setString(5, cleanRemarks);
                    ps.executeUpdate();
                }

                AuditService.logEvent(conn.c, adminUsername, "Admin", AuditService.ACTION_COMPLAINT_PRIORITY_CHANGED,
                    AuditService.ENTITY_COMPLAINT, refNumber,
                    "Priority changed from " + currentPriority + " to " + targetPriority, currentPriority, targetPriority);

                conn.c.commit();
                return ServiceResult.ok("Complaint priority changed to " + targetPriority + ".", refNumber, complaintId);
            } catch (Exception ex) {
                conn.c.rollback();
                ex.printStackTrace();
                return ServiceResult.fail("DATABASE_ERROR", "Transaction failed: " + ex.getMessage());
            } finally {
                conn.c.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ServiceResult.fail("DATABASE_ERROR", "Error: " + e.getMessage());
        }
    }

    /**
     * Assigns complaint to an administrator and records history.
     */
    public static ServiceResult assignComplaint(int complaintId, String assignedAdmin, String adminUsername, String remarks) {
        if (complaintId <= 0) {
            return ServiceResult.fail("INVALID_ID", "Invalid complaint ID.");
        }
        if (assignedAdmin == null || assignedAdmin.trim().isEmpty()) {
            return ServiceResult.fail("INVALID_ADMIN", "Administrator username/name cannot be empty.");
        }
        assignedAdmin = assignedAdmin.trim();
        if (adminUsername == null || adminUsername.trim().isEmpty()) {
            adminUsername = "Admin";
        }
        adminUsername = adminUsername.trim();

        try (Conn conn = new Conn()) {
            if (conn.c == null) {
                return ServiceResult.fail("DATABASE_ERROR", "Database connection failed.");
            }

            String currentAssigned = null;
            String refNumber = null;

            String fetchSql = "SELECT reference_number, assigned_admin FROM complaints WHERE complaint_id = ?";
            try (PreparedStatement ps = conn.c.prepareStatement(fetchSql)) {
                ps.setInt(1, complaintId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        refNumber = rs.getString("reference_number");
                        currentAssigned = rs.getString("assigned_admin");
                    } else {
                        return ServiceResult.fail("NOT_FOUND", "Complaint ID " + complaintId + " does not exist.");
                    }
                }
            }

            conn.c.setAutoCommit(false);
            try {
                String updateSql = "UPDATE complaints SET assigned_admin = ?, updated_at = NOW() WHERE complaint_id = ?";
                try (PreparedStatement ps = conn.c.prepareStatement(updateSql)) {
                    ps.setString(1, assignedAdmin);
                    ps.setInt(2, complaintId);
                    ps.executeUpdate();
                }

                String cleanRemarks = (remarks != null && !remarks.trim().isEmpty()) ? remarks.trim() : ("Assigned to " + assignedAdmin);

                String insertHistorySql = 
                    "INSERT INTO complaint_history (complaint_id, action, previous_value, new_value, performed_by, remarks, created_at) " +
                    "VALUES (?, 'Complaint Assigned', ?, ?, ?, ?, NOW())";
                try (PreparedStatement ps = conn.c.prepareStatement(insertHistorySql)) {
                    ps.setInt(1, complaintId);
                    ps.setString(2, currentAssigned != null ? currentAssigned : "Unassigned");
                    ps.setString(3, assignedAdmin);
                    ps.setString(4, adminUsername);
                    ps.setString(5, cleanRemarks);
                    ps.executeUpdate();
                }

                AuditService.logEvent(conn.c, adminUsername, "Admin", AuditService.ACTION_COMPLAINT_ASSIGNED,
                    AuditService.ENTITY_COMPLAINT, refNumber,
                    "Complaint assigned to " + assignedAdmin, currentAssigned != null ? currentAssigned : "Unassigned", assignedAdmin);

                conn.c.commit();
                return ServiceResult.ok("Complaint assigned to " + assignedAdmin + ".", refNumber, complaintId);
            } catch (Exception ex) {
                conn.c.rollback();
                ex.printStackTrace();
                return ServiceResult.fail("DATABASE_ERROR", "Transaction failed: " + ex.getMessage());
            } finally {
                conn.c.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ServiceResult.fail("DATABASE_ERROR", "Error: " + e.getMessage());
        }
    }

    /**
     * Adds or updates resolution remarks and records history.
     */
    public static ServiceResult addRemarks(int complaintId, String newRemarks, String adminUsername) {
        if (complaintId <= 0) {
            return ServiceResult.fail("INVALID_ID", "Invalid complaint ID.");
        }
        if (newRemarks == null || newRemarks.trim().isEmpty()) {
            return ServiceResult.fail("EMPTY_REMARKS", "Remarks cannot be empty.");
        }
        newRemarks = newRemarks.trim();
        if (adminUsername == null || adminUsername.trim().isEmpty()) {
            adminUsername = "Admin";
        }
        adminUsername = adminUsername.trim();

        try (Conn conn = new Conn()) {
            if (conn.c == null) {
                return ServiceResult.fail("DATABASE_ERROR", "Database connection failed.");
            }

            String currentRemarks = null;
            String refNumber = null;

            String fetchSql = "SELECT reference_number, resolution_remarks FROM complaints WHERE complaint_id = ?";
            try (PreparedStatement ps = conn.c.prepareStatement(fetchSql)) {
                ps.setInt(1, complaintId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        refNumber = rs.getString("reference_number");
                        currentRemarks = rs.getString("resolution_remarks");
                    } else {
                        return ServiceResult.fail("NOT_FOUND", "Complaint ID " + complaintId + " does not exist.");
                    }
                }
            }

            conn.c.setAutoCommit(false);
            try {
                String updateSql = "UPDATE complaints SET resolution_remarks = ?, updated_at = NOW() WHERE complaint_id = ?";
                try (PreparedStatement ps = conn.c.prepareStatement(updateSql)) {
                    ps.setString(1, newRemarks);
                    ps.setInt(2, complaintId);
                    ps.executeUpdate();
                }

                String insertHistorySql = 
                    "INSERT INTO complaint_history (complaint_id, action, previous_value, new_value, performed_by, remarks, created_at) " +
                    "VALUES (?, 'Resolution Remarks Added', ?, ?, ?, ?, NOW())";
                try (PreparedStatement ps = conn.c.prepareStatement(insertHistorySql)) {
                    ps.setInt(1, complaintId);
                    ps.setString(2, currentRemarks);
                    ps.setString(3, newRemarks);
                    ps.setString(4, adminUsername);
                    ps.setString(5, newRemarks);
                    ps.executeUpdate();
                }

                AuditService.logEvent(conn.c, adminUsername, "Admin", AuditService.ACTION_COMPLAINT_REMARK_ADDED,
                    AuditService.ENTITY_COMPLAINT, refNumber,
                    "Resolution remarks added: " + newRemarks, currentRemarks, newRemarks);

                conn.c.commit();
                return ServiceResult.ok("Resolution remarks recorded successfully.", refNumber, complaintId);
            } catch (Exception ex) {
                conn.c.rollback();
                ex.printStackTrace();
                return ServiceResult.fail("DATABASE_ERROR", "Transaction failed: " + ex.getMessage());
            } finally {
                conn.c.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ServiceResult.fail("DATABASE_ERROR", "Error: " + e.getMessage());
        }
    }

    /**
     * Rejects an unauthorized status modification attempt by a customer.
     */
    public static ServiceResult customerAttemptModifyStatus(String role) {
        if (!"Admin".equalsIgnoreCase(role)) {
            return ServiceResult.fail("UNAUTHORIZED", "Customers are not permitted to modify complaint status, priority, or resolution remarks.");
        }
        return ServiceResult.ok("Authorized", null, 0);
    }
}
