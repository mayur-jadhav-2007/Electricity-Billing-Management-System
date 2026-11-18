package electricity.billing.system;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Configuration and constants for Late Payment Surcharge (LPSC) calculation
 * in accordance with the UPERC FY 2025-26 Tariff Order Rate Schedule General Provisions.
 *
 * Applicable to retrospective postpaid electricity connections.
 * Convention:
 *  - Tier 1: 1.25% per month for the first 90 days of delay.
 *  - Tier 2: 2.00% per month thereafter (> 90 days of delay).
 *  - Proportional day-to-day calculation using a 30-day monthly divisor.
 *  - Terminal rounding: HALF_UP to two decimal places after summing both unrounded tiers.
 *  - Default grace period: 15 calendar days from bill generation.
 */
public final class LpscConfig {

    private LpscConfig() {
        // Prevent instantiation
    }

    /** Primary regulatory authority source */
    public static final String REGULATORY_ORDER_SOURCE = "Official UPERC FY 2025–26 Tariff Order, Page 489";

    /** Verbatim UPERC Tariff Order Page 489 regulatory provision */
    public static final String REGULATORY_QUOTATION =
        "“In case of consumers having postpaid connections, if a consumer fails to pay his electricity bill by the due date specified therein, a late payment surcharge shall be levied at 1.25% on the dues (excluding late payment surcharge) per month; up-to first three months of delay and subsequently at 2.00% on the dues (excluding late payment surcharge) per month of delay. Late payment surcharge shall be calculated proportionately for the number of days for which the payment is delayed beyond the due date specified in the bill and levied on the unpaid amount of the bill excluding delayed payment surcharge.”";

    /** Monthly rate for Tier 1 (up to 90 days): 1.25% = 0.0125 */
    public static final BigDecimal TIER1_MONTHLY_RATE = new BigDecimal("0.0125");
    public static final BigDecimal RATE_TIER1_MONTHLY = TIER1_MONTHLY_RATE;

    /** Monthly rate for Tier 2 (beyond 90 days): 2.00% = 0.0200 */
    public static final BigDecimal TIER2_MONTHLY_RATE = new BigDecimal("0.0200");
    public static final BigDecimal RATE_TIER2_MONTHLY = TIER2_MONTHLY_RATE;

    /** Day boundary between Tier 1 and Tier 2: 90 days */
    public static final long TIER1_MAX_DAYS = 90L;
    public static final long DAYS_TIER1_LIMIT = TIER1_MAX_DAYS;

    /** Standard agreed application divisor for monthly rate conversion: 30 days */
    public static final BigDecimal DAYS_PER_MONTH = new BigDecimal("30");
    public static final BigDecimal DAYS_PER_MONTH_DIVISOR = DAYS_PER_MONTH;

    /** Standard regulatory grace period: 15 calendar days */
    public static final int STANDARD_GRACE_DAYS = 15;

    /** Demonstration mode grace period: 1 calendar day */
    public static final int DEMO_GRACE_DAYS = 1;

    /** Standard bill payment grace period: 15 calendar days (backwards compatibility alias) */
    public static final int DEFAULT_GRACE_DAYS = STANDARD_GRACE_DAYS;

    /** Active grace period setting (defaults to 15 days) */
    private static volatile int activeGraceDays = STANDARD_GRACE_DAYS;

    /**
     * Gets the currently active grace period for new bill generation.
     * Default is 15 calendar days.
     */
    public static int getActiveGraceDays() {
        return activeGraceDays;
    }

    /**
     * Allows the administrator to configure the active grace period (1 day for demo, 15 days for standard).
     * Applies only to newly generated bills. Existing bills remain untouched.
     */
    public static synchronized void setActiveGraceDays(int days) {
        if (days == DEMO_GRACE_DAYS || days == STANDARD_GRACE_DAYS) {
            activeGraceDays = days;
        } else {
            throw new IllegalArgumentException("Only 1-day (Demo) or 15-day (Standard) grace period is supported. Got: " + days);
        }
    }

    /**
     * Restores the default regulatory grace period (15 calendar days) after a demonstration.
     */
    public static synchronized void restoreDefaultGraceDays() {
        activeGraceDays = STANDARD_GRACE_DAYS;
    }

    /** Scale for terminal rounding: 2 decimal places */
    public static final int CURRENCY_SCALE = 2;

    /** Rounding mode for final surcharge and totals: HALF_UP */
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /** Working scale for unrounded intermediate tier calculations */
    public static final int INTERMEDIATE_SCALE = 16;
}
