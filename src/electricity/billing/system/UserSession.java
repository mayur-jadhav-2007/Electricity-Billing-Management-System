package electricity.billing.system;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe centralized user session manager holding the current authenticated SessionContext.
 */
public final class UserSession {

    private static final AtomicReference<SessionContext> currentSession = new AtomicReference<>(null);

    private UserSession() {
        // Prevent instantiation
    }

    /**
     * Initializes the authenticated session.
     */
    public static void setSession(SessionContext session) {
        currentSession.set(session);
    }

    /**
     * Retrieves the active SessionContext snapshot, or null if unauthenticated.
     */
    public static SessionContext getSession() {
        return currentSession.get();
    }

    /**
     * Alias for getSession().
     */
    public static SessionContext getCurrentSession() {
        return currentSession.get();
    }

    /**
     * Checks if a user is currently authenticated.
     */
    public static boolean isAuthenticated() {
        return currentSession.get() != null;
    }

    /**
     * Checks if the authenticated user has the Admin role.
     */
    public static boolean isAdmin() {
        SessionContext s = currentSession.get();
        return s != null && s.isAdmin();
    }

    /**
     * Checks if the authenticated user has the Customer role.
     */
    public static boolean isCustomer() {
        SessionContext s = currentSession.get();
        return s != null && s.isCustomer();
    }

    /**
     * Atomically consumes and clears the active session context.
     * Guaranteed to return non-null to exactly one thread/invocation per session lifecycle.
     * Subsequent invocations return null until a new session is established.
     */
    public static SessionContext consumeSession() {
        return currentSession.getAndSet(null);
    }

    /**
     * Invalidates the active session immediately.
     */
    public static void invalidate() {
        currentSession.set(null);
    }

    /**
     * Returns the authenticated username, or "SYSTEM" if unauthenticated.
     */
    public static String getActorId() {
        SessionContext s = currentSession.get();
        return (s != null && !s.getUsername().isEmpty()) ? s.getUsername() : "SYSTEM";
    }

    /**
     * Returns the authenticated role, or "SYSTEM" if unauthenticated.
     */
    public static String getActorRole() {
        SessionContext s = currentSession.get();
        return (s != null && !s.getRole().isEmpty()) ? s.getRole() : "SYSTEM";
    }
}
