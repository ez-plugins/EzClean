package com.skyblockexp.ezclean.config;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

/**
 * Immutable broadcast and warning settings for a cleanup profile.
 *
 * <p>Groups all player-facing notification options — the pre-cleanup warning, start/summary
 * broadcasts, periodic interval reminders, dynamic per-minute/per-second reminders, and the
 * post-run stats summary digest.</p>
 *
 * <p>Instances are constructed exclusively by {@link CleanupSettings} during configuration
 * loading. All message templates are pre-resolved MiniMessage strings.</p>
 */
public final class BroadcastSettings {

    // Warning
    private final boolean warningEnabled;
    private final long warningOffsetTicks;
    private final long warningMinutesBefore;
    private final String warningMessageTemplate;

    // Start & summary
    private final boolean startBroadcastEnabled;
    private final String startMessageTemplate;
    private final boolean summaryBroadcastEnabled;
    private final String summaryMessageTemplate;
    private final @Nullable String preCleanMessageTemplate;

    // Interval
    private final boolean intervalBroadcastEnabled;
    private final long intervalBroadcastMinutes;
    private final String intervalBroadcastMessageTemplate;

    // Dynamic
    private final boolean dynamicBroadcastEnabled;
    private final Set<Long> dynamicBroadcastMinutes;
    private final Set<Long> dynamicBroadcastSeconds;
    private final String dynamicBroadcastMessageTemplate;

    // Stats summary
    private final boolean statsSummaryBroadcastEnabled;
    private final long statsSummaryEveryRuns;
    private final String statsSummaryMessageTemplate;

    BroadcastSettings(
            boolean warningEnabled, long warningOffsetTicks, long warningMinutesBefore,
            String warningMessageTemplate,
            boolean startBroadcastEnabled, String startMessageTemplate,
            boolean summaryBroadcastEnabled, String summaryMessageTemplate,
            @Nullable String preCleanMessageTemplate,
            boolean intervalBroadcastEnabled, long intervalBroadcastMinutes,
            String intervalBroadcastMessageTemplate,
            boolean dynamicBroadcastEnabled, Set<Long> dynamicBroadcastMinutes,
            Set<Long> dynamicBroadcastSeconds, String dynamicBroadcastMessageTemplate,
            boolean statsSummaryBroadcastEnabled, long statsSummaryEveryRuns,
            String statsSummaryMessageTemplate) {
        this.warningEnabled = warningEnabled;
        this.warningOffsetTicks = warningOffsetTicks;
        this.warningMinutesBefore = warningMinutesBefore;
        this.warningMessageTemplate = warningMessageTemplate;
        this.startBroadcastEnabled = startBroadcastEnabled;
        this.startMessageTemplate = startMessageTemplate;
        this.summaryBroadcastEnabled = summaryBroadcastEnabled;
        this.summaryMessageTemplate = summaryMessageTemplate;
        this.preCleanMessageTemplate = preCleanMessageTemplate;
        this.intervalBroadcastEnabled = intervalBroadcastEnabled;
        this.intervalBroadcastMinutes = intervalBroadcastMinutes;
        this.intervalBroadcastMessageTemplate = intervalBroadcastMessageTemplate;
        this.dynamicBroadcastEnabled = dynamicBroadcastEnabled;
        this.dynamicBroadcastMinutes = Collections.unmodifiableSet(new LinkedHashSet<>(dynamicBroadcastMinutes));
        this.dynamicBroadcastSeconds = Collections.unmodifiableSet(new LinkedHashSet<>(dynamicBroadcastSeconds));
        this.dynamicBroadcastMessageTemplate = dynamicBroadcastMessageTemplate;
        this.statsSummaryBroadcastEnabled = statsSummaryBroadcastEnabled;
        this.statsSummaryEveryRuns = statsSummaryEveryRuns;
        this.statsSummaryMessageTemplate = statsSummaryMessageTemplate;
    }

    // -----------------------------------------------------------------------------------------
    // Warning
    // -----------------------------------------------------------------------------------------

    /** Whether the pre-cleanup warning broadcast is enabled. */
    public boolean isWarningEnabled() {
        return warningEnabled;
    }

    /** Warning lead time expressed in server ticks ({@code warningMinutesBefore * 1200}). */
    public long getWarningOffsetTicks() {
        return warningOffsetTicks;
    }

    /** How many minutes before the cleanup the warning message is sent. */
    public long getWarningMinutesBefore() {
        return warningMinutesBefore;
    }

    /** MiniMessage template for the pre-cleanup warning broadcast. */
    public String getWarningMessageTemplate() {
        return warningMessageTemplate;
    }

    // -----------------------------------------------------------------------------------------
    // Start & summary
    // -----------------------------------------------------------------------------------------

    /** Whether the cleanup-start broadcast is sent to all players. */
    public boolean isStartBroadcastEnabled() {
        return startBroadcastEnabled;
    }

    /** MiniMessage template for the cleanup-start broadcast. */
    public String getStartMessageTemplate() {
        return startMessageTemplate;
    }

    /** Whether the post-cleanup summary broadcast is sent to all players. */
    public boolean isSummaryBroadcastEnabled() {
        return summaryBroadcastEnabled;
    }

    /** MiniMessage template for the post-cleanup summary broadcast. */
    public String getSummaryMessageTemplate() {
        return summaryMessageTemplate;
    }

    /** Optional MiniMessage template shown during the warning phase as a pre-run summary. */
    public @Nullable String getPreCleanMessageTemplate() {
        return preCleanMessageTemplate;
    }

    // -----------------------------------------------------------------------------------------
    // Interval
    // -----------------------------------------------------------------------------------------

    /** Whether periodic interval reminders are sent. */
    public boolean isIntervalBroadcastEnabled() {
        return intervalBroadcastEnabled;
    }

    /** How often (minutes) the interval reminder is broadcast. */
    public long getIntervalBroadcastMinutes() {
        return intervalBroadcastMinutes;
    }

    /** MiniMessage template for the interval reminder broadcast. */
    public String getIntervalBroadcastMessageTemplate() {
        return intervalBroadcastMessageTemplate;
    }

    // -----------------------------------------------------------------------------------------
    // Dynamic
    // -----------------------------------------------------------------------------------------

    /** Whether dynamic per-minute/per-second reminders are enabled. */
    public boolean isDynamicBroadcastEnabled() {
        return dynamicBroadcastEnabled;
    }

    /** Minute marks at which a dynamic reminder is sent (e.g. 30, 15, 10, 5). */
    public Set<Long> getDynamicBroadcastMinutes() {
        return dynamicBroadcastMinutes;
    }

    /** Second marks at which a dynamic reminder is sent (e.g. 5, 4, 3, 2, 1). */
    public Set<Long> getDynamicBroadcastSeconds() {
        return dynamicBroadcastSeconds;
    }

    /** MiniMessage template for dynamic reminder broadcasts. */
    public String getDynamicBroadcastMessageTemplate() {
        return dynamicBroadcastMessageTemplate;
    }

    // -----------------------------------------------------------------------------------------
    // Stats summary
    // -----------------------------------------------------------------------------------------

    /** Whether the post-run stats digest is periodically broadcast. */
    public boolean isStatsSummaryBroadcastEnabled() {
        return statsSummaryBroadcastEnabled;
    }

    /** Number of runs between each stats-summary broadcast. */
    public long getStatsSummaryEveryRuns() {
        return statsSummaryEveryRuns;
    }

    /** MiniMessage template for the stats-summary digest broadcast. */
    public String getStatsSummaryMessageTemplate() {
        return statsSummaryMessageTemplate;
    }
}
