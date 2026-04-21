package com.skyblockexp.ezclean.config;

import java.util.Locale;

/**
 * Immutable configuration describing how players can cancel an upcoming cleanup cycle.
 */
public final class CleanupCancelSettings {

    private final boolean enabled;
    private final double cost;
    private final String hoverMessage;
    private final String successMessage;
    private final String broadcastMessage;
    private final String insufficientFundsMessage;
    private final String disabledMessage;
    private final String noEconomyMessage;

    private CleanupCancelSettings(boolean enabled, double cost, String hoverMessage, String successMessage,
            String broadcastMessage, String insufficientFundsMessage, String disabledMessage,
            String noEconomyMessage) {
        this.enabled = enabled;
        this.cost = cost;
        this.hoverMessage = hoverMessage;
        this.successMessage = successMessage;
        this.broadcastMessage = broadcastMessage;
        this.insufficientFundsMessage = insufficientFundsMessage;
        this.disabledMessage = disabledMessage;
        this.noEconomyMessage = noEconomyMessage;
    }

    /**
     * Returns a settings instance with the cancel mechanic disabled.
     */
    public static CleanupCancelSettings disabled() {
        return new CleanupCancelSettings(false, 0.0D, "", "", "", "", "", "");
    }

    public static CleanupCancelSettings create(boolean enabled, double cost, String hoverMessage, String successMessage,
            String broadcastMessage, String insufficientFundsMessage, String disabledMessage,
            String noEconomyMessage) {
        if (!enabled) {
            return disabled();
        }
        return new CleanupCancelSettings(true, Math.max(0.0D, cost), hoverMessage, successMessage, broadcastMessage,
                insufficientFundsMessage, disabledMessage, noEconomyMessage);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double getCost() {
        return cost;
    }

    public String getHoverMessage() {
        return hoverMessage;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public String getBroadcastMessage() {
        return broadcastMessage;
    }

    public String getInsufficientFundsMessage() {
        return insufficientFundsMessage;
    }

    public String getDisabledMessage() {
        return disabledMessage;
    }

    public String getNoEconomyMessage() {
        return noEconomyMessage;
    }

    /**
     * Formats the configured cancel cost into a human-friendly string suitable for placeholders.
     *
     * @return formatted cost string
     */
    public String getFormattedCost() {
        if (Math.abs(cost - Math.rint(cost)) < 0.005D) {
            return String.format(Locale.US, "%,.0f", cost);
        }
        return String.format(Locale.US, "%,.2f", cost);
    }
}

