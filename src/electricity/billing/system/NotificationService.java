package electricity.billing.system;

import java.sql.*;
import java.util.*;

/**
 * Enterprise service providing secure, transaction-safe operations for Admin-to-Customer notifications.
 * Implements strict role-based access control, session-derived customer meter isolation,
 * atomic transactions with state tracking, and integration with the centralized AuditService.
 */
public final class NotificationService {

    // Permitted Priority Levels (matching CHECK constraint chk_msg_priority)
    public static final String[] PRIORITIES = {"Normal", "Important", "Urgent"};
    public static final String PRIORITY_NORMAL = "Normal";
    public static final String PRIORITY_IMPORTANT = "Important";
    public static final String PRIORITY_URGENT = "Urgent";

    // Target Types (matching CHECK constraint chk_msg_target_type)
    public static final String TARGET_INDIVIDUAL = "INDIVIDUAL";
    public static final String TARGET_BROADCAST = "BROADCAST";

    private NotificationService() {
        // Prevent instantiation of static utility/service class
    }

    // =========================================================================
    // TRANSACTION STATE ENUM
    // =========================================================================

    public enum TxState {
        NOT_STARTED,
        ACTIVE,
        COMMITTED,
        COMMIT_FAILED,
        ROLLED_BACK,
        ROLLBACK_FAILED
    }

    // =========================================================================
    // DTO & RESULT CLASSES
    // =========================================================================

    /**
     * Standard operation outcome wrapper for mutation operations (send, broadcast, markAsRead).
     */
    public static class ServiceResult {
        public final boolean success;
        public final String errorCode;
        public final String message;
        public final int messageId;
        public final int affectedRows;

        public ServiceResult(boolean success, String errorCode, String message, int messageId, int affectedRows) {
            this.success = success;
            this.errorCode = (errorCode != null) ? errorCode : (success ? "SUCCESS" : "ERROR");
            this.message = (message != null) ? message : "";
            this.messageId = messageId;
            this.affectedRows = affectedRows;
        }

        public static ServiceResult ok(String message) {
            return new ServiceResult(true, "SUCCESS", message, -1, 0);
        }

        public static ServiceResult ok(String message, int messageId) {
            return new ServiceResult(true, "SUCCESS", message, messageId, 1);
        }

        public static ServiceResult ok(String message, int messageId, int affectedRows) {
            return new ServiceResult(true, "SUCCESS", message, messageId, affectedRows);
        }

        public static ServiceResult fail(String errorCode, String message) {
            return new ServiceResult(false, errorCode, message, -1, 0);
        }

        public boolean isSuccess() { return success; }
        public String getErrorCode() { return errorCode; }
        public String getMessage() { return message; }
        public int getMessageId() { return messageId; }
        public int getAffectedRows() { return affectedRows; }

        @Override
        public String toString() {
            return "ServiceResult{" +
                   "success=" + success +
                   ", errorCode='" + errorCode + '\'' +
                   ", message='" + message + '\'' +
                   ", messageId=" + messageId +
                   ", affectedRows=" + affectedRows +
                   '}';
        }
    }

    /**
     * Generic typed query wrapper for query operations, guaranteeing type-safe error handling.
     */
    public static class QueryResult<T> {
        public final boolean success;
        public final String errorCode;
        public final String message;
        public final T data;

        public QueryResult(boolean success, String errorCode, String message, T data) {
            this.success = success;
            this.errorCode = (errorCode != null) ? errorCode : (success ? "SUCCESS" : "ERROR");
            this.message = (message != null) ? message : "";
            this.data = data;
        }

        public static <T> QueryResult<T> ok(T data) {
            return new QueryResult<>(true, "SUCCESS", "Query executed successfully.", data);
        }

        public static <T> QueryResult<T> fail(String errorCode, String message) {
            return new QueryResult<>(false, errorCode, message, null);
        }

        public boolean isSuccess() { return success; }
        public String getErrorCode() { return errorCode; }
        public String getMessage() { return message; }
        public T getData() { return data; }

        @Override
        public String toString() {
            return "QueryResult{" +
                   "success=" + success +
                   ", errorCode='" + errorCode + '\'' +
                   ", message='" + message + '\'' +
                   ", hasData=" + (data != null) +
                   '}';
        }
    }

    /**
     * Immutable summary DTO for admin sent message history with recipient and read count aggregation.
     */
    public static class SentMessageSummary {
        public final int messageId;
        public final String senderAdmin;
        public final String targetType;
        public final String title;
        public final String message;
        public final String priority;
        public final Timestamp createdAt;
        public final int totalRecipients;
        public final int readCount;
        public final String recipientMeter;
        public final String recipientName;

        public SentMessageSummary(int messageId, String senderAdmin, String targetType,
                                  String title, String message, String priority,
                                  Timestamp createdAt, int totalRecipients, int readCount,
                                  String recipientMeter, String recipientName) {
            this.messageId = messageId;
            this.senderAdmin = senderAdmin;
            this.targetType = targetType;
            this.title = title;
            this.message = message;
            this.priority = priority;
            this.createdAt = createdAt;
            this.totalRecipients = totalRecipients;
            this.readCount = readCount;
            this.recipientMeter = recipientMeter;
            this.recipientName = recipientName;
        }

        public int getMessageId() { return messageId; }
        public String getSenderAdmin() { return senderAdmin; }
        public String getTargetType() { return targetType; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getBody() { return message; }
        public String getPriority() { return priority; }
        public Timestamp getCreatedAt() { return createdAt; }
        public int getTotalRecipients() { return totalRecipients; }
        public int getReadCount() { return readCount; }
        public int getUnreadCount() { return Math.max(0, totalRecipients - readCount); }
        public String getRecipientMeter() { return recipientMeter; }
        public String getRecipientName() { return recipientName; }
    }

    /**
     * Immutable notification DTO for customer inbox display.
     */
    public static class CustomerNotification {
        public final int recipientId;
        public final int messageId;
        public final String meterNo;
        public final boolean isRead;
        public final Timestamp readAt;
        public final Timestamp receivedAt;
        public final String senderAdmin;
        public final String targetType;
        public final String title;
        public final String message;
        public final String priority;

        public CustomerNotification(int recipientId, int messageId, String meterNo,
                                    boolean isRead, Timestamp readAt, Timestamp receivedAt,
                                    String senderAdmin, String targetType, String title,
                                    String message, String priority) {
            this.recipientId = recipientId;
            this.messageId = messageId;
            this.meterNo = meterNo;
            this.isRead = isRead;
            this.readAt = readAt;
            this.receivedAt = receivedAt;
            this.senderAdmin = senderAdmin;
            this.targetType = targetType;
            this.title = title;
            this.message = message;
            this.priority = priority;
        }

        public int getRecipientId() { return recipientId; }
        public int getMessageId() { return messageId; }
        public String getMeterNo() { return meterNo; }
        public boolean isRead() { return isRead; }
        public Timestamp getReadAt() { return readAt; }
        public Timestamp getReceivedAt() { return receivedAt; }
        public String getSenderAdmin() { return senderAdmin; }
        public String getTargetType() { return targetType; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getBody() { return message; }
        public String getPriority() { return priority; }
    }

    // =========================================================================
    // VALIDATION HELPERS
    // =========================================================================

    public static boolean isValidPriority(String priority) {
        if (priority == null) return false;
        for (String p : PRIORITIES) {
            if (p.equalsIgnoreCase(priority.trim())) return true;
        }
        return false;
    }

    public static String normalizePriority(String priority) {
        if (priority == null) return null;
        for (String p : PRIORITIES) {
            if (p.equalsIgnoreCase(priority.trim())) return p;
        }
        return null;
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        s = s.trim();
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }

    private static String sanitizeErrorMessage(Throwable t) {
        if (t == null) return "An unexpected error occurred.";
        String msg = t.getMessage();
        if (msg == null || msg.trim().isEmpty()) {
            return "An internal database error occurred.";
        }
        msg = msg.trim();
        if (msg.contains("password") || msg.contains("Access denied for user") || msg.contains("jdbc:")) {
            return "Database access error. Please contact your system administrator.";
        }
        if (msg.length() > 250) {
            msg = msg.substring(0, 250) + "...";
        }
        return msg;
    }

    // =========================================================================
    // SYSTEM NOTIFICATION DISPATCH (PACKAGE-PRIVATE)
    // =========================================================================

    /**
     * Sends an automatic system in-app notification to a specific customer meter for internal application events
     * (e.g., bill generated, payment completed, complaint status changed).
     * <p>
     * Package-private visibility guarantees invocation only by internal application services, never external callers.
     * Uses SYSTEM as sender identity without requiring or elevating administrator session privileges.
     * </p>
     */
    static ServiceResult sendSystemNotification(String meterNo, String title, String body, String priority) {
        return executeIndividualDispatch(meterNo, title, body, priority, "SYSTEM", "SYSTEM");
    }

    // =========================================================================
    // ADMIN OPERATIONS
    // =========================================================================

    /**
     * Sends an individual notification message to a specific customer meter inside an atomic transaction.
     * Verifies that the recipient customer exists and has an 'Active' account status.
     * Strictly requires active administrator privileges.
     */
    public static ServiceResult sendIndividualMessage(String meterNo, String title, String body, String priority) {
        // 1. Authorization check
        if (!UserSession.isAdmin()) {
            return ServiceResult.fail("UNAUTHORIZED", "Access denied. Administrator privileges required.");
        }

        String senderAdmin = UserSession.getActorId();
        if (senderAdmin == null || senderAdmin.trim().isEmpty() || "SYSTEM".equalsIgnoreCase(senderAdmin.trim())) {
            senderAdmin = "Admin";
        }
        senderAdmin = truncate(senderAdmin, 50);

        return executeIndividualDispatch(meterNo, title, body, priority, senderAdmin, "Admin");
    }

    /**
     * Internal atomic execution engine for individual and system message dispatch.
     */
    private static ServiceResult executeIndividualDispatch(String meterNo, String title, String body, String priority,
                                                          String senderActor, String senderRole) {
        // 1. Input validation
        if (meterNo == null || meterNo.trim().isEmpty()) {
            return ServiceResult.fail("INVALID_INPUT", "Recipient meter number is required.");
        }
        String cleanMeter = meterNo.trim();
        if (cleanMeter.length() > 20) {
            return ServiceResult.fail("INVALID_INPUT", "Meter number cannot exceed 20 characters.");
        }

        if (title == null || title.trim().isEmpty()) {
            return ServiceResult.fail("INVALID_INPUT", "Message title cannot be empty.");
        }
        String cleanTitle = title.trim();
        if (cleanTitle.length() < 3 || cleanTitle.length() > 150) {
            return ServiceResult.fail("INVALID_INPUT", "Message title must be between 3 and 150 characters.");
        }

        if (body == null || body.trim().isEmpty()) {
            return ServiceResult.fail("INVALID_INPUT", "Message body cannot be empty.");
        }
        String cleanBody = body.trim();
        if (cleanBody.length() < 5 || cleanBody.length() > 5000) {
            return ServiceResult.fail("INVALID_INPUT", "Message body must be between 5 and 5000 characters.");
        }

        if (priority == null || !isValidPriority(priority)) {
            return ServiceResult.fail("INVALID_INPUT", "Invalid priority. Permitted values: Normal, Important, Urgent.");
        }
        String cleanPriority = normalizePriority(priority);

        String safeSender = (senderActor != null && !senderActor.trim().isEmpty()) ? truncate(senderActor.trim(), 50) : "SYSTEM";
        String safeRole = (senderRole != null && !senderRole.trim().isEmpty()) ? truncate(senderRole.trim(), 20) : "SYSTEM";

        // 2. Database operation with state-aware transaction
        Conn conn = new Conn();
        if (conn.c == null) {
            return ServiceResult.fail("DATABASE_ERROR", "Database connection unavailable.");
        }

        TxState txState = TxState.NOT_STARTED;
        boolean originalAutoCommit = true;
        Throwable rootFailure = null;
        int messageId = -1;

        try {
            originalAutoCommit = conn.c.getAutoCommit();
            conn.c.setAutoCommit(false);
            txState = TxState.ACTIVE;

            // Verify customer exists and is active
            String checkCustSql = "SELECT status FROM customer WHERE meter_no = ? FOR SHARE";
            try (PreparedStatement psCust = conn.c.prepareStatement(checkCustSql)) {
                psCust.setString(1, cleanMeter);
                try (ResultSet rsCust = psCust.executeQuery()) {
                    if (!rsCust.next()) {
                        conn.c.rollback();
                        txState = TxState.ROLLED_BACK;
                        return ServiceResult.fail("CUSTOMER_NOT_FOUND", "Customer with meter number '" + cleanMeter + "' does not exist.");
                    }
                    String status = rsCust.getString("status");
                    if (!"Active".equalsIgnoreCase(status != null ? status.trim() : "")) {
                        conn.c.rollback();
                        txState = TxState.ROLLED_BACK;
                        return ServiceResult.fail("CUSTOMER_INACTIVE", "Customer account for meter '" + cleanMeter + "' is not active (current status: " + status + ").");
                    }
                }
            }

            // Insert into messages
            String insertMsgSql = "INSERT INTO messages (sender_admin, target_type, title, message, priority, created_at) " +
                                  "VALUES (?, 'INDIVIDUAL', ?, ?, ?, CURRENT_TIMESTAMP)";
            try (PreparedStatement psMsg = conn.c.prepareStatement(insertMsgSql, Statement.RETURN_GENERATED_KEYS)) {
                psMsg.setString(1, safeSender);
                psMsg.setString(2, cleanTitle);
                psMsg.setString(3, cleanBody);
                psMsg.setString(4, cleanPriority);
                int msgRows = psMsg.executeUpdate();
                if (msgRows != 1) {
                    throw new SQLException("Failed to insert message record, unexpected affected rows: " + msgRows);
                }
                try (ResultSet keys = psMsg.getGeneratedKeys()) {
                    if (keys.next()) {
                        messageId = keys.getInt(1);
                    } else {
                        throw new SQLException("Failed to retrieve generated message_id.");
                    }
                }
            }

            // Insert into message_recipients
            String insertRecipSql = "INSERT INTO message_recipients (message_id, meter_no, is_read, read_at, created_at) " +
                                    "VALUES (?, ?, 0, NULL, CURRENT_TIMESTAMP)";
            try (PreparedStatement psRecip = conn.c.prepareStatement(insertRecipSql)) {
                psRecip.setInt(1, messageId);
                psRecip.setString(2, cleanMeter);
                int recipRows = psRecip.executeUpdate();
                if (recipRows != 1) {
                    throw new SQLException("Failed to insert message recipient record, unexpected affected rows: " + recipRows);
                }
            }

            // Audit log event inside the same transaction
            String auditDesc = ("SYSTEM".equalsIgnoreCase(safeSender) ? "System sent notification #" : "Sent individual message #")
                    + messageId + " ('" + truncate(cleanTitle, 50) + "') to meter " + cleanMeter;
            AuditService.logEvent(
                conn.c,
                safeSender,
                safeRole,
                AuditService.ACTION_MESSAGE_SENT,
                AuditService.ENTITY_NOTIFICATION,
                String.valueOf(messageId),
                auditDesc,
                null,
                cleanPriority
            );

            // Attempt commit
            try {
                conn.c.commit();
                txState = TxState.COMMITTED;
            } catch (SQLException commitEx) {
                txState = TxState.COMMIT_FAILED;
                rootFailure = commitEx;
                try {
                    conn.c.rollback();
                } catch (SQLException rbEx) {
                    commitEx.addSuppressed(rbEx);
                }
                throw commitEx;
            }

            return ServiceResult.ok("Message sent successfully to meter " + cleanMeter + ".", messageId, 1);

        } catch (Throwable ex) {
            if (rootFailure == null) {
                rootFailure = ex;
            }

            if (txState == TxState.ACTIVE) {
                try {
                    conn.c.rollback();
                    txState = TxState.ROLLED_BACK;
                } catch (SQLException rbEx) {
                    txState = TxState.ROLLBACK_FAILED;
                    rootFailure.addSuppressed(rbEx);
                }
            }

            if (txState == TxState.COMMIT_FAILED) {
                return ServiceResult.fail(
                    "COMMIT_OUTCOME_UNKNOWN",
                    "Transaction commit failed; outcome is uncertain. Please check sent message history before retrying to prevent duplicate messages."
                );
            } else if (txState == TxState.ROLLBACK_FAILED) {
                return ServiceResult.fail(
                    "ROLLBACK_FAILED",
                    "Database transaction failed and rollback could not be completed. Manual DBA inspection may be required."
                );
            } else {
                return ServiceResult.fail(
                    "DATABASE_ERROR",
                    "Transaction rolled back due to error: " + sanitizeErrorMessage(rootFailure)
                );
            }
        } finally {
            if (txState == TxState.COMMITTED || txState == TxState.ROLLED_BACK) {
                try {
                    if (conn.c != null && !conn.c.isClosed()) {
                        conn.c.setAutoCommit(originalAutoCommit);
                    }
                } catch (SQLException acEx) {
                    if (rootFailure != null) {
                        rootFailure.addSuppressed(acEx);
                    }
                }
            }
            try {
                conn.close();
            } catch (Throwable closeEx) {
                if (rootFailure != null) {
                    rootFailure.addSuppressed(closeEx);
                }
            }
        }
    }

    /**
     * Sends a broadcast notification message to all currently Active customers inside an atomic transaction.
     * Aborts without writing message or audit records if zero active customers exist.
     */
    public static ServiceResult sendBroadcastMessage(String title, String body, String priority) {
        // 1. Authorization check
        if (!UserSession.isAdmin()) {
            return ServiceResult.fail("UNAUTHORIZED", "Access denied. Administrator privileges required.");
        }

        // 2. Input validation
        if (title == null || title.trim().isEmpty()) {
            return ServiceResult.fail("INVALID_INPUT", "Message title cannot be empty.");
        }
        String cleanTitle = title.trim();
        if (cleanTitle.length() < 3 || cleanTitle.length() > 150) {
            return ServiceResult.fail("INVALID_INPUT", "Message title must be between 3 and 150 characters.");
        }

        if (body == null || body.trim().isEmpty()) {
            return ServiceResult.fail("INVALID_INPUT", "Message body cannot be empty.");
        }
        String cleanBody = body.trim();
        if (cleanBody.length() < 5 || cleanBody.length() > 5000) {
            return ServiceResult.fail("INVALID_INPUT", "Message body must be between 5 and 5000 characters.");
        }

        if (priority == null || !isValidPriority(priority)) {
            return ServiceResult.fail("INVALID_INPUT", "Invalid priority. Permitted values: Normal, Important, Urgent.");
        }
        String cleanPriority = normalizePriority(priority);

        String senderAdmin = UserSession.getActorId();
        if (senderAdmin == null || senderAdmin.trim().isEmpty() || "SYSTEM".equalsIgnoreCase(senderAdmin.trim())) {
            senderAdmin = "Admin";
        }
        senderAdmin = truncate(senderAdmin, 50);

        // 3. Database operation with state-aware transaction
        Conn conn = new Conn();
        if (conn.c == null) {
            return ServiceResult.fail("DATABASE_ERROR", "Database connection unavailable.");
        }

        TxState txState = TxState.NOT_STARTED;
        boolean originalAutoCommit = true;
        Throwable rootFailure = null;
        int messageId = -1;

        try {
            originalAutoCommit = conn.c.getAutoCommit();
            conn.c.setAutoCommit(false);
            txState = TxState.ACTIVE;

            // Select active customer meter numbers using FOR SHARE lock
            List<String> activeMeters = new ArrayList<>();
            String selectActiveSql = "SELECT meter_no FROM customer WHERE status = 'Active' FOR SHARE";
            try (PreparedStatement psCust = conn.c.prepareStatement(selectActiveSql);
                 ResultSet rsCust = psCust.executeQuery()) {
                while (rsCust.next()) {
                    String m = rsCust.getString("meter_no");
                    if (m != null && !m.trim().isEmpty()) {
                        activeMeters.add(m.trim());
                    }
                }
            }

            // Gating: If zero active customers, abort without creating message or audit record
            if (activeMeters.isEmpty()) {
                conn.c.rollback();
                txState = TxState.ROLLED_BACK;
                return ServiceResult.fail("NO_ACTIVE_RECIPIENTS", "No active customers found. Broadcast message was not sent.");
            }

            // Insert BROADCAST message record
            String insertMsgSql = "INSERT INTO messages (sender_admin, target_type, title, message, priority, created_at) " +
                                  "VALUES (?, 'BROADCAST', ?, ?, ?, CURRENT_TIMESTAMP)";
            try (PreparedStatement psMsg = conn.c.prepareStatement(insertMsgSql, Statement.RETURN_GENERATED_KEYS)) {
                psMsg.setString(1, senderAdmin);
                psMsg.setString(2, cleanTitle);
                psMsg.setString(3, cleanBody);
                psMsg.setString(4, cleanPriority);
                int msgRows = psMsg.executeUpdate();
                if (msgRows != 1) {
                    throw new SQLException("Failed to insert broadcast message record, unexpected affected rows: " + msgRows);
                }
                try (ResultSet keys = psMsg.getGeneratedKeys()) {
                    if (keys.next()) {
                        messageId = keys.getInt(1);
                    } else {
                        throw new SQLException("Failed to retrieve generated message_id for broadcast.");
                    }
                }
            }

            // Batch insert recipient records
            String insertRecipSql = "INSERT INTO message_recipients (message_id, meter_no, is_read, read_at, created_at) " +
                                    "VALUES (?, ?, 0, NULL, CURRENT_TIMESTAMP)";
            try (PreparedStatement psRecip = conn.c.prepareStatement(insertRecipSql)) {
                for (String meter : activeMeters) {
                    psRecip.setInt(1, messageId);
                    psRecip.setString(2, meter);
                    psRecip.addBatch();
                }
                int[] batchResults = psRecip.executeBatch();
                int successfulInserts = 0;
                for (int res : batchResults) {
                    if (res == Statement.EXECUTE_FAILED) {
                        throw new SQLException("Batch insert failed for recipient in broadcast message #" + messageId);
                    } else if (res >= 0) {
                        successfulInserts += res;
                    } else if (res == Statement.SUCCESS_NO_INFO) {
                        successfulInserts += 1;
                    }
                }
                if (successfulInserts != activeMeters.size()) {
                    throw new SQLException("Inserted recipient count (" + successfulInserts + ") does not match selected active customer count (" + activeMeters.size() + ").");
                }
            }

            // Audit log event inside the same transaction
            AuditService.logEvent(
                conn.c,
                senderAdmin,
                "Admin",
                AuditService.ACTION_MESSAGE_BROADCAST,
                AuditService.ENTITY_NOTIFICATION,
                String.valueOf(messageId),
                "Broadcast message #" + messageId + " ('" + truncate(cleanTitle, 50) + "') sent to " + activeMeters.size() + " active customers",
                null,
                cleanPriority
            );

            // Attempt commit
            try {
                conn.c.commit();
                txState = TxState.COMMITTED;
            } catch (SQLException commitEx) {
                txState = TxState.COMMIT_FAILED;
                rootFailure = commitEx;
                try {
                    conn.c.rollback();
                } catch (SQLException rbEx) {
                    commitEx.addSuppressed(rbEx);
                }
                throw commitEx;
            }

            return ServiceResult.ok("Broadcast message sent successfully to " + activeMeters.size() + " active customers.", messageId, activeMeters.size());

        } catch (Throwable ex) {
            if (rootFailure == null) {
                rootFailure = ex;
            }

            if (txState == TxState.ACTIVE) {
                try {
                    conn.c.rollback();
                    txState = TxState.ROLLED_BACK;
                } catch (SQLException rbEx) {
                    txState = TxState.ROLLBACK_FAILED;
                    rootFailure.addSuppressed(rbEx);
                }
            }

            if (txState == TxState.COMMIT_FAILED) {
                return ServiceResult.fail(
                    "COMMIT_OUTCOME_UNKNOWN",
                    "Transaction commit failed; outcome is uncertain. Please check sent message history before retrying to prevent duplicate messages."
                );
            } else if (txState == TxState.ROLLBACK_FAILED) {
                return ServiceResult.fail(
                    "ROLLBACK_FAILED",
                    "Database transaction failed and rollback could not be completed. Manual DBA inspection may be required."
                );
            } else {
                return ServiceResult.fail(
                    "DATABASE_ERROR",
                    "Transaction rolled back due to error: " + sanitizeErrorMessage(rootFailure)
                );
            }
        } finally {
            if (txState == TxState.COMMITTED || txState == TxState.ROLLED_BACK) {
                try {
                    if (conn.c != null && !conn.c.isClosed()) {
                        conn.c.setAutoCommit(originalAutoCommit);
                    }
                } catch (SQLException acEx) {
                    if (rootFailure != null) {
                        rootFailure.addSuppressed(acEx);
                    }
                }
            }
            try {
                conn.close();
            } catch (Throwable closeEx) {
                if (rootFailure != null) {
                    rootFailure.addSuppressed(closeEx);
                }
            }
        }
    }

    /**
     * Retrieves sent message history with recipient and read count aggregation for administrators.
     */
    public static QueryResult<List<SentMessageSummary>> getSentMessageHistory() {
        if (!UserSession.isAdmin()) {
            return QueryResult.fail("UNAUTHORIZED", "Access denied. Administrator privileges required.");
        }

        try (Conn conn = new Conn()) {
            if (conn.c == null) {
                return QueryResult.fail("DATABASE_ERROR", "Database connection unavailable.");
            }

            String sql =
                "SELECT m.message_id, m.sender_admin, m.target_type, m.title, m.message, m.priority, m.created_at, " +
                "       COUNT(r.recipient_id) AS total_recipients, " +
                "       COALESCE(SUM(CASE WHEN r.is_read = 1 THEN 1 ELSE 0 END), 0) AS read_count, " +
                "       MAX(CASE WHEN m.target_type = 'INDIVIDUAL' THEN r.meter_no ELSE NULL END) AS recipient_meter, " +
                "       MAX(CASE WHEN m.target_type = 'INDIVIDUAL' THEN c.name ELSE NULL END) AS recipient_name " +
                "FROM messages m " +
                "LEFT JOIN message_recipients r ON m.message_id = r.message_id " +
                "LEFT JOIN customer c ON r.meter_no = c.meter_no " +
                "GROUP BY m.message_id, m.sender_admin, m.target_type, m.title, m.message, m.priority, m.created_at " +
                "ORDER BY m.created_at DESC";

            try (PreparedStatement ps = conn.c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                List<SentMessageSummary> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new SentMessageSummary(
                        rs.getInt("message_id"),
                        rs.getString("sender_admin"),
                        rs.getString("target_type"),
                        rs.getString("title"),
                        rs.getString("message"),
                        rs.getString("priority"),
                        rs.getTimestamp("created_at"),
                        rs.getInt("total_recipients"),
                        rs.getInt("read_count"),
                        rs.getString("recipient_meter"),
                        rs.getString("recipient_name")
                    ));
                }
                return QueryResult.ok(Collections.unmodifiableList(list));
            }
        } catch (SQLException ex) {
            return QueryResult.fail("DATABASE_ERROR", "Failed to retrieve sent message history: " + sanitizeErrorMessage(ex));
        }
    }

    // =========================================================================
    // CUSTOMER OPERATIONS
    // =========================================================================

    /**
     * Retrieves all notifications for the authenticated customer.
     */
    public static QueryResult<List<CustomerNotification>> getCustomerNotifications() {
        return getCustomerNotifications(false);
    }

    /**
     * Retrieves notifications for the authenticated customer, optionally filtering for unread messages only.
     * The meter number is strictly extracted from the active customer session.
     */
    public static QueryResult<List<CustomerNotification>> getCustomerNotifications(boolean unreadOnly) {
        if (!UserSession.isCustomer()) {
            return QueryResult.fail("UNAUTHORIZED", "Access denied. Customer privileges required.");
        }

        SessionContext session = UserSession.getSession();
        if (session == null || session.getMeterNo() == null || session.getMeterNo().trim().isEmpty()) {
            return QueryResult.fail("INVALID_SESSION", "No active customer session found.");
        }
        String meterNo = session.getMeterNo().trim();

        try (Conn conn = new Conn()) {
            if (conn.c == null) {
                return QueryResult.fail("DATABASE_ERROR", "Database connection unavailable.");
            }

            String sql =
                "SELECT r.recipient_id, r.message_id, r.meter_no, r.is_read, r.read_at, r.created_at AS received_at, " +
                "       m.sender_admin, m.target_type, m.title, m.message, m.priority " +
                "FROM message_recipients r " +
                "JOIN messages m ON r.message_id = m.message_id " +
                "WHERE r.meter_no = ? " +
                (unreadOnly ? "AND r.is_read = 0 " : "") +
                "ORDER BY r.is_read ASC, r.created_at DESC";

            try (PreparedStatement ps = conn.c.prepareStatement(sql)) {
                ps.setString(1, meterNo);
                try (ResultSet rs = ps.executeQuery()) {
                    List<CustomerNotification> list = new ArrayList<>();
                    while (rs.next()) {
                        list.add(new CustomerNotification(
                            rs.getInt("recipient_id"),
                            rs.getInt("message_id"),
                            rs.getString("meter_no"),
                            rs.getInt("is_read") == 1,
                            rs.getTimestamp("read_at"),
                            rs.getTimestamp("received_at"),
                            rs.getString("sender_admin"),
                            rs.getString("target_type"),
                            rs.getString("title"),
                            rs.getString("message"),
                            rs.getString("priority")
                        ));
                    }
                    return QueryResult.ok(Collections.unmodifiableList(list));
                }
            }
        } catch (SQLException ex) {
            return QueryResult.fail("DATABASE_ERROR", "Failed to retrieve notifications: " + sanitizeErrorMessage(ex));
        }
    }

    /**
     * Retrieves the count of unread notifications for the authenticated customer.
     */
    public static QueryResult<Integer> getUnreadNotificationCount() {
        if (!UserSession.isCustomer()) {
            return QueryResult.fail("UNAUTHORIZED", "Access denied. Customer privileges required.");
        }

        SessionContext session = UserSession.getSession();
        if (session == null || session.getMeterNo() == null || session.getMeterNo().trim().isEmpty()) {
            return QueryResult.fail("INVALID_SESSION", "No active customer session found.");
        }
        String meterNo = session.getMeterNo().trim();

        try (Conn conn = new Conn()) {
            if (conn.c == null) {
                return QueryResult.fail("DATABASE_ERROR", "Database connection unavailable.");
            }

            String sql = "SELECT COUNT(*) FROM message_recipients WHERE meter_no = ? AND is_read = 0";
            try (PreparedStatement ps = conn.c.prepareStatement(sql)) {
                ps.setString(1, meterNo);
                try (ResultSet rs = ps.executeQuery()) {
                    int count = 0;
                    if (rs.next()) {
                        count = rs.getInt(1);
                    }
                    return QueryResult.ok(count);
                }
            }
        } catch (SQLException ex) {
            return QueryResult.fail("DATABASE_ERROR", "Failed to retrieve unread notification count: " + sanitizeErrorMessage(ex));
        }
    }

    /**
     * Marks a notification recipient record as read inside an atomic transaction.
     * Enforces customer isolation by requiring both recipient_id and authenticated customer meter_no.
     */
    public static ServiceResult markAsRead(int recipientId) {
        if (!UserSession.isCustomer()) {
            return ServiceResult.fail("UNAUTHORIZED", "Access denied. Customer privileges required.");
        }

        SessionContext session = UserSession.getSession();
        if (session == null || session.getMeterNo() == null || session.getMeterNo().trim().isEmpty()) {
            return ServiceResult.fail("INVALID_SESSION", "No active customer session found.");
        }
        String meterNo = session.getMeterNo().trim();

        if (recipientId <= 0) {
            return ServiceResult.fail("INVALID_INPUT", "Invalid notification recipient ID.");
        }

        Conn conn = new Conn();
        if (conn.c == null) {
            return ServiceResult.fail("DATABASE_ERROR", "Database connection unavailable.");
        }

        TxState txState = TxState.NOT_STARTED;
        boolean originalAutoCommit = true;
        Throwable rootFailure = null;

        try {
            originalAutoCommit = conn.c.getAutoCommit();
            conn.c.setAutoCommit(false);
            txState = TxState.ACTIVE;

            String updateSql = "UPDATE message_recipients " +
                               "SET is_read = 1, read_at = CURRENT_TIMESTAMP " +
                               "WHERE recipient_id = ? AND meter_no = ? AND is_read = 0";
            int affected = 0;
            try (PreparedStatement psUpdate = conn.c.prepareStatement(updateSql)) {
                psUpdate.setInt(1, recipientId);
                psUpdate.setString(2, meterNo);
                affected = psUpdate.executeUpdate();
            }

            if (affected == 1) {
                // Log audit event inside transaction
                AuditService.logEvent(
                    conn.c,
                    meterNo,
                    "Customer",
                    AuditService.ACTION_MESSAGE_READ,
                    AuditService.ENTITY_NOTIFICATION,
                    String.valueOf(recipientId),
                    "Marked notification #" + recipientId + " as read",
                    "0",
                    "1"
                );

                try {
                    conn.c.commit();
                    txState = TxState.COMMITTED;
                } catch (SQLException commitEx) {
                    txState = TxState.COMMIT_FAILED;
                    rootFailure = commitEx;
                    try {
                        conn.c.rollback();
                    } catch (SQLException rbEx) {
                        commitEx.addSuppressed(rbEx);
                    }
                    throw commitEx;
                }

                return ServiceResult.ok("Notification marked as read.", recipientId, 1);
            } else {
                // Identify reason for 0 affected rows: already read or does not exist / belongs to another customer
                String checkSql = "SELECT is_read FROM message_recipients WHERE recipient_id = ? AND meter_no = ?";
                boolean alreadyRead = false;
                boolean exists = false;
                try (PreparedStatement psCheck = conn.c.prepareStatement(checkSql)) {
                    psCheck.setInt(1, recipientId);
                    psCheck.setString(2, meterNo);
                    try (ResultSet rsCheck = psCheck.executeQuery()) {
                        if (rsCheck.next()) {
                            exists = true;
                            alreadyRead = (rsCheck.getInt("is_read") == 1);
                        }
                    }
                }

                conn.c.rollback();
                txState = TxState.ROLLED_BACK;

                if (alreadyRead) {
                    return ServiceResult.fail("ALREADY_READ", "Notification is already marked as read.");
                } else if (!exists) {
                    return ServiceResult.fail("NOT_FOUND", "Notification record not found or access denied.");
                } else {
                    return ServiceResult.fail("DATABASE_ERROR", "Failed to update notification read status.");
                }
            }

        } catch (Throwable ex) {
            if (rootFailure == null) {
                rootFailure = ex;
            }

            if (txState == TxState.ACTIVE) {
                try {
                    conn.c.rollback();
                    txState = TxState.ROLLED_BACK;
                } catch (SQLException rbEx) {
                    txState = TxState.ROLLBACK_FAILED;
                    rootFailure.addSuppressed(rbEx);
                }
            }

            if (txState == TxState.COMMIT_FAILED) {
                return ServiceResult.fail(
                    "COMMIT_OUTCOME_UNKNOWN",
                    "Transaction commit failed; outcome is uncertain. Please check your notification status before retrying."
                );
            } else if (txState == TxState.ROLLBACK_FAILED) {
                return ServiceResult.fail(
                    "ROLLBACK_FAILED",
                    "Database transaction failed and rollback could not be completed. Manual DBA inspection may be required."
                );
            } else {
                return ServiceResult.fail(
                    "DATABASE_ERROR",
                    "Transaction rolled back due to error: " + sanitizeErrorMessage(rootFailure)
                );
            }
        } finally {
            if (txState == TxState.COMMITTED || txState == TxState.ROLLED_BACK) {
                try {
                    if (conn.c != null && !conn.c.isClosed()) {
                        conn.c.setAutoCommit(originalAutoCommit);
                    }
                } catch (SQLException acEx) {
                    if (rootFailure != null) {
                        rootFailure.addSuppressed(acEx);
                    }
                }
            }
            try {
                conn.close();
            } catch (Throwable closeEx) {
                if (rootFailure != null) {
                    rootFailure.addSuppressed(closeEx);
                }
            }
        }
    }
}
