package electricity.billing.system;

import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable value object holding authenticated session details.
 * Instances of this class are completely thread-safe and immutable.
 */
public final class SessionContext implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String username;
    private final String role;
    private final String meterNo;
    private final long loginTimestamp;

    public SessionContext(String username, String role, String meterNo) {
        this(username, role, meterNo, System.currentTimeMillis());
    }

    public SessionContext(String username, String role, String meterNo, long loginTimestamp) {
        this.username = (username != null) ? username.trim() : "";
        this.role = (role != null) ? role.trim() : "Unknown";
        this.meterNo = (meterNo != null) ? meterNo.trim() : "";
        this.loginTimestamp = loginTimestamp;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getMeterNo() {
        return meterNo;
    }

    public long getLoginTimestamp() {
        return loginTimestamp;
    }

    public boolean isAdmin() {
        return "Admin".equalsIgnoreCase(role);
    }

    public boolean isCustomer() {
        return "Customer".equalsIgnoreCase(role);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionContext that = (SessionContext) o;
        return loginTimestamp == that.loginTimestamp &&
               Objects.equals(username, that.username) &&
               Objects.equals(role, that.role) &&
               Objects.equals(meterNo, that.meterNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, role, meterNo, loginTimestamp);
    }

    @Override
    public String toString() {
        return "SessionContext{" +
               "username='" + username + '\'' +
               ", role='" + role + '\'' +
               ", meterNo='" + meterNo + '\'' +
               ", loginTimestamp=" + loginTimestamp +
               '}';
    }
}
