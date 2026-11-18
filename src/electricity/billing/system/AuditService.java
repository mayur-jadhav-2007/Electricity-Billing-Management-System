package electricity.billing.system;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.*;

/**
 * Centralized audit logging service providing transactional and independent audit persistence,
 * secure CSV export with formula injection defense (CWE-1236), and filtered pagination querying.
 */
public final class AuditService {

    // Action Type Constants
    public static final String ACTION_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String ACTION_LOGIN_FAILED = "LOGIN_FAILED";
    public static final String ACTION_LOGOUT = "LOGOUT";

    public static final String ACTION_CUSTOMER_CREATED = "CUSTOMER_CREATED";
    public static final String ACTION_CUSTOMER_UPDATED = "CUSTOMER_UPDATED";
    public static final String ACTION_CUSTOMER_DEACTIVATED = "CUSTOMER_DEACTIVATED";
    public static final String ACTION_CUSTOMER_REACTIVATED = "CUSTOMER_REACTIVATED";

    public static final String ACTION_BILL_GENERATED = "BILL_GENERATED";
    public static final String ACTION_BILL_RECALCULATED = "BILL_RECALCULATED";
    public static final String ACTION_BILL_DUE_DATE_CREATED = "BILL_DUE_DATE_CREATED";

    public static final String ACTION_PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    public static final String ACTION_PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String ACTION_PAYMENT_DUPLICATE_ATTEMPT = "PAYMENT_DUPLICATE_ATTEMPT";

    public static final String ACTION_COMPLAINT_SUBMITTED = "COMPLAINT_SUBMITTED";
    public static final String ACTION_COMPLAINT_ASSIGNED = "COMPLAINT_ASSIGNED";
    public static final String ACTION_COMPLAINT_STATUS_CHANGED = "COMPLAINT_STATUS_CHANGED";
    public static final String ACTION_COMPLAINT_PRIORITY_CHANGED = "COMPLAINT_PRIORITY_CHANGED";
    public static final String ACTION_COMPLAINT_REMARK_ADDED = "COMPLAINT_REMARK_ADDED";
    public static final String ACTION_COMPLAINT_REOPENED = "COMPLAINT_REOPENED";

    // Notification Action Types
    public static final String ACTION_MESSAGE_SENT = "MESSAGE_SENT";
    public static final String ACTION_MESSAGE_BROADCAST = "MESSAGE_BROADCAST";
    public static final String ACTION_MESSAGE_READ = "MESSAGE_READ";

    // Entity Types
    public static final String ENTITY_USER = "USER";
    public static final String ENTITY_CUSTOMER = "CUSTOMER";
    public static final String ENTITY_BILL = "BILL";
    public static final String ENTITY_PAYMENT = "PAYMENT";
    public static final String ENTITY_COMPLAINT = "COMPLAINT";
    public static final String ENTITY_NOTIFICATION = "NOTIFICATION";

    private static final ExecutorService asyncExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Audit-Async-Worker");
        t.setDaemon(true);
        return t;
    });

    private AuditService() {
        // Prevent instantiation
    }

    /**
     * Inserts an audit log entry participating in an existing database transaction on the given Connection.
     * Throws SQLException if insertion fails so the calling business transaction can be safely rolled back.
     */
    public static void logEvent(Connection conn, String actorId, String actorRole, String actionType,
                                String entityType, String entityId, String description,
                                String oldValue, String newValue) throws SQLException {
        if (conn == null) {
            throw new SQLException("Cannot log audit event: JDBC Connection is null");
        }

        String safeActorId = truncate(actorId != null ? actorId : "SYSTEM", 50);
        String safeActorRole = truncate(actorRole != null ? actorRole : "SYSTEM", 20);
        String safeAction = truncate(actionType != null ? actionType : "UNKNOWN", 50);
        String safeEntityType = truncate(entityType != null ? entityType : "GENERAL", 30);
        String safeEntityId = truncate(entityId != null ? entityId : "N/A", 50);
        String safeDesc = truncate(description != null ? description : "-", 255);

        String sql = "INSERT INTO audit_logs (actor_id, actor_role, action_type, entity_type, entity_id, " +
                     "description, old_value, new_value, ip_address, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, CURRENT_TIMESTAMP)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, safeActorId);
            ps.setString(2, safeActorRole);
            ps.setString(3, safeAction);
            ps.setString(4, safeEntityType);
            ps.setString(5, safeEntityId);
            ps.setString(6, safeDesc);
            ps.setString(7, oldValue);
            ps.setString(8, newValue);
            ps.executeUpdate();
        }
    }

    /**
     * Type-safe audit logger for bill-specific operations using bill_id as entity identifier.
     */
    public static void logBillEvent(Connection conn, String actorId, String actorRole, String actionType,
                                    long billId, String description,
                                    String oldValue, String newValue) throws SQLException {
        logEvent(conn, actorId, actorRole, actionType, ENTITY_BILL, String.valueOf(billId), description, oldValue, newValue);
    }

    /**
     * Transactional audit logger for bill recalculation recording administrator reason and amount changes.
     */
    public static void logBillRecalculation(Connection conn, String actorId, String actorRole,
                                            long billId, String meterNo, String month,
                                            String oldAmount, String newAmount, String reason) throws SQLException {
        String desc = String.format("Recalculated bill #%d (Meter: %s, Month: %s). Reason: %s",
                billId, meterNo, month, (reason != null && !reason.trim().isEmpty()) ? reason.trim() : "Not specified");
        logEvent(conn, actorId, actorRole, ACTION_BILL_RECALCULATED, ENTITY_BILL, String.valueOf(billId), desc, oldAmount, newAmount);
    }

    /**
     * Inserts an audit log entry using an independent database connection.
     * Used for standalone events like login success, login failure, or failed non-transactional operations.
     */
    public static boolean logIndependent(String actorId, String actorRole, String actionType,
                                         String entityType, String entityId, String description,
                                         String oldValue, String newValue) {
        try (Conn c = new Conn()) {
            if (c.c == null) {
                System.err.println("[AUDIT ERROR] Database connection unavailable for independent audit log. [Ref: ERR-AUDIT-CONN]");
                return false;
            }
            logEvent(c.c, actorId, actorRole, actionType, entityType, entityId, description, oldValue, newValue);
            return true;
        } catch (SQLException e) {
            System.err.println("[AUDIT ERROR] Failed to record audit log: " + actionType + " [Ref: ERR-AUDIT-INSERT]");
            return false;
        }
    }

    /**
     * Asynchronously records an audit event on a background thread.
     * Guarantees non-blocking execution so UI threads (e.g., during window closing or logout) never freeze.
     */
    public static void logAsync(String actorId, String actorRole, String actionType,
                                String entityType, String entityId, String description,
                                String oldValue, String newValue) {
        try {
            asyncExecutor.submit(() -> {
                logIndependent(actorId, actorRole, actionType, entityType, entityId, description, oldValue, newValue);
            });
        } catch (RejectedExecutionException ree) {
            System.err.println("[AUDIT ERROR] Async audit worker rejected task. [Ref: ERR-AUDIT-REJECTED]");
        }
    }

    /**
     * Sanitizes a single cell value for CSV export to prevent Formula Injection (CWE-1236).
     * Scans leading prefix (ASCII whitespace, control characters, Unicode whitespace, and BOMs).
     * If the first significant character is a formula trigger (=, +, -, @), prepends a single quote (').
     * Wraps cell in RFC 4180 quotes, escaping embedded quotes as "".
     */
    public static String sanitizeCsvCell(String value) {
        if (value == null) {
            return "\"\"";
        }

        int i = 0;
        int len = value.length();

        // Scan past leading prefix characters without modifying the original data
        while (i < len) {
            char c = value.charAt(i);
            if (c == '\uFEFF' || c == '\uFFFE') {
                i++;
            } else if (c <= 0x0020) { // ASCII whitespace (' ', '\t', '\r', '\n') and control characters
                i++;
            } else if (isUnicodeWhitespace(c)) {
                i++;
            } else {
                break;
            }
        }

        // Check first effective non-prefix character
        boolean isFormula = false;
        if (i < len) {
            char first = value.charAt(i);
            if (first == '=' || first == '+' || first == '-' || first == '@') {
                isFormula = true;
            }
        }

        String safeValue = isFormula ? ("'" + value) : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    private static boolean isUnicodeWhitespace(char c) {
        return c == '\u00A0' || (c >= '\u2000' && c <= '\u200B') ||
               c == '\u202F' || c == '\u205F' || c == '\u3000';
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        s = s.trim();
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }

    // =========================================================================
    // DTOs & QUERY APIS
    // =========================================================================

    public static class AuditLogEntry {
        public final String displayId;
        public final long auditId;
        public final String actorId;
        public final String actorRole;
        public final String actionType;
        public final String entityType;
        public final String entityId;
        public final String description;
        public final String oldValue;
        public final String newValue;
        public final String ipAddress;
        public final Timestamp createdAt;
        public final String sourceTable;

        public AuditLogEntry(String displayId, long auditId, String actorId, String actorRole,
                             String actionType, String entityType, String entityId,
                             String description, String oldValue, String newValue,
                             String ipAddress, Timestamp createdAt, String sourceTable) {
            this.displayId = displayId;
            this.auditId = auditId;
            this.actorId = actorId;
            this.actorRole = actorRole;
            this.actionType = actionType;
            this.entityType = entityType;
            this.entityId = entityId;
            this.description = description;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.ipAddress = ipAddress;
            this.createdAt = createdAt;
            this.sourceTable = sourceTable;
        }
    }

    public static class QueryResult {
        public final List<AuditLogEntry> entries;
        public final int totalRecords;
        public final int page;
        public final int pageSize;
        public final int totalPages;

        public QueryResult(List<AuditLogEntry> entries, int totalRecords, int page, int pageSize) {
            this.entries = Collections.unmodifiableList(entries != null ? entries : new ArrayList<>());
            this.totalRecords = totalRecords;
            this.page = page;
            this.pageSize = pageSize;
            this.totalPages = (int) Math.ceil((double) totalRecords / Math.max(1, pageSize));
        }
    }

    /**
     * Queries audit logs with pagination and multi-criteria filtering.
     * Optionally combines with complaint_history via UNION ALL without ID collision.
     */
    public static QueryResult queryLogs(String actionFilter, String roleFilter, String searchQuery,
                                        Date startDate, Date endDate, int page, int pageSize,
                                        boolean includeComplaintHistory) throws SQLException {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 25;
        int offset = (page - 1) * pageSize;

        List<AuditLogEntry> list = new ArrayList<>();
        int totalCount = 0;

        try (Conn c = new Conn()) {
            if (c.c == null) {
                throw new SQLException("Database connection unavailable [Ref: ERR-AUDIT-QUERY]");
            }

            StringBuilder whereClause = new StringBuilder(" WHERE 1=1 ");
            List<Object> params = new ArrayList<>();

            if (actionFilter != null && !actionFilter.trim().isEmpty() && !actionFilter.equalsIgnoreCase("All Actions")) {
                whereClause.append(" AND action_type = ? ");
                params.add(actionFilter.trim());
            }

            if (roleFilter != null && !roleFilter.trim().isEmpty() && !roleFilter.equalsIgnoreCase("All Roles")) {
                whereClause.append(" AND actor_role = ? ");
                params.add(roleFilter.trim());
            }

            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                String term = "%" + searchQuery.trim().toLowerCase() + "%";
                whereClause.append(" AND (LOWER(actor_id) LIKE ? OR LOWER(entity_id) LIKE ? OR LOWER(description) LIKE ?) ");
                params.add(term);
                params.add(term);
                params.add(term);
            }

            if (startDate != null) {
                whereClause.append(" AND created_at >= ? ");
                params.add(new Timestamp(startDate.getTime()));
            }

            if (endDate != null) {
                whereClause.append(" AND created_at <= ? ");
                params.add(new Timestamp(endDate.getTime()));
            }

            // Construct count and data queries
            String baseQuery;
            if (includeComplaintHistory) {
                baseQuery =
                    "(SELECT " +
                    "  CONCAT('AUD-', audit_id) AS display_id, " +
                    "  audit_id, " +
                    "  actor_id, " +
                    "  actor_role, " +
                    "  action_type, " +
                    "  entity_type, " +
                    "  entity_id, " +
                    "  description, " +
                    "  old_value, " +
                    "  new_value, " +
                    "  ip_address, " +
                    "  created_at, " +
                    "  'audit_logs' AS source_table " +
                    "FROM audit_logs " +
                    "UNION ALL " +
                    "SELECT " +
                    "  CONCAT('CMP-HIST-', history_id) AS display_id, " +
                    "  history_id AS audit_id, " +
                    "  CASE WHEN performed_by = 'Admin' THEN 'Admin' ELSE performed_by END AS actor_id, " +
                    "  CASE WHEN performed_by = 'Admin' THEN 'Legacy/Unknown' WHEN performed_by LIKE 'Customer%' THEN 'Customer' ELSE 'Admin' END AS actor_role, " +
                    "  action AS action_type, " +
                    "  'COMPLAINT' AS entity_type, " +
                    "  CAST(complaint_id AS CHAR) AS entity_id, " +
                    "  COALESCE(remarks, action) AS description, " +
                    "  previous_value AS old_value, " +
                    "  new_value AS new_value, " +
                    "  NULL AS ip_address, " +
                    "  created_at, " +
                    "  'complaint_history' AS source_table " +
                    "FROM complaint_history) AS combined_logs";
            } else {
                baseQuery =
                    "(SELECT " +
                    "  CONCAT('AUD-', audit_id) AS display_id, " +
                    "  audit_id, " +
                    "  actor_id, " +
                    "  actor_role, " +
                    "  action_type, " +
                    "  entity_type, " +
                    "  entity_id, " +
                    "  description, " +
                    "  old_value, " +
                    "  new_value, " +
                    "  ip_address, " +
                    "  created_at, " +
                    "  'audit_logs' AS source_table " +
                    "FROM audit_logs) AS combined_logs";
            }

            // 1. Total record count
            String countSql = "SELECT COUNT(*) FROM " + baseQuery + whereClause.toString();
            try (PreparedStatement psCount = c.c.prepareStatement(countSql)) {
                for (int i = 0; i < params.size(); i++) {
                    psCount.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = psCount.executeQuery()) {
                    if (rs.next()) {
                        totalCount = rs.getInt(1);
                    }
                }
            }

            // 2. Paginated data query
            String dataSql = "SELECT * FROM " + baseQuery + whereClause.toString() +
                             " ORDER BY created_at DESC, audit_id DESC LIMIT ? OFFSET ?";
            try (PreparedStatement psData = c.c.prepareStatement(dataSql)) {
                int paramIdx = 1;
                for (Object param : params) {
                    psData.setObject(paramIdx++, param);
                }
                psData.setInt(paramIdx++, pageSize);
                psData.setInt(paramIdx, offset);

                try (ResultSet rs = psData.executeQuery()) {
                    while (rs.next()) {
                        list.add(new AuditLogEntry(
                            rs.getString("display_id"),
                            rs.getLong("audit_id"),
                            rs.getString("actor_id"),
                            rs.getString("actor_role"),
                            rs.getString("action_type"),
                            rs.getString("entity_type"),
                            rs.getString("entity_id"),
                            rs.getString("description"),
                            rs.getString("old_value"),
                            rs.getString("new_value"),
                            rs.getString("ip_address"),
                            rs.getTimestamp("created_at"),
                            rs.getString("source_table")
                        ));
                    }
                }
            }
        }

        return new QueryResult(list, totalCount, page, pageSize);
    }

    /**
     * Exports a list of audit records to CSV using strict RFC 4180 formatting and formula injection defense.
     */
    public static void exportToCsv(List<AuditLogEntry> entries, File destinationFile) throws IOException {
        if (destinationFile == null) {
            throw new IllegalArgumentException("Destination file cannot be null");
        }

        File parent = destinationFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(destinationFile), StandardCharsets.UTF_8))) {
            
            // Header Row - all sanitized
            String[] headers = {
                "Log ID", "Timestamp", "Actor ID", "Actor Role", "Action Type",
                "Entity Type", "Entity ID", "Description", "Old Value", "New Value"
            };

            for (int i = 0; i < headers.length; i++) {
                bw.write(sanitizeCsvCell(headers[i]));
                if (i < headers.length - 1) bw.write(",");
            }
            bw.newLine();

            // Data Rows
            if (entries != null) {
                for (AuditLogEntry e : entries) {
                    bw.write(sanitizeCsvCell(e.displayId));
                    bw.write(",");
                    bw.write(sanitizeCsvCell(e.createdAt != null ? e.createdAt.toString() : ""));
                    bw.write(",");
                    bw.write(sanitizeCsvCell(e.actorId));
                    bw.write(",");
                    bw.write(sanitizeCsvCell(e.actorRole));
                    bw.write(",");
                    bw.write(sanitizeCsvCell(e.actionType));
                    bw.write(",");
                    bw.write(sanitizeCsvCell(e.entityType));
                    bw.write(",");
                    bw.write(sanitizeCsvCell(e.entityId));
                    bw.write(",");
                    bw.write(sanitizeCsvCell(e.description));
                    bw.write(",");
                    bw.write(sanitizeCsvCell(e.oldValue != null ? e.oldValue : ""));
                    bw.write(",");
                    bw.write(sanitizeCsvCell(e.newValue != null ? e.newValue : ""));
                    bw.newLine();
                }
            }
        }
    }
}
