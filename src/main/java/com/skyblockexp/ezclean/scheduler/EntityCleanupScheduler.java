package com.skyblockexp.ezclean.scheduler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.skyblockexp.ezclean.util.EntityPileDetector;
import com.skyblockexp.ezclean.util.FoliaScheduler;
import com.skyblockexp.ezclean.stats.CleanupStatsTracker;
import com.skyblockexp.ezclean.integration.WorldGuardCleanupBypass;
import com.skyblockexp.ezclean.config.CleanupSettings;
import com.skyblockexp.ezclean.config.CleanupCancelSettings;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import org.jetbrains.annotations.Nullable;
import com.skyblockexp.ezclean.service.BroadcastService;

/**
 * Schedules an hourly entity cleanup cycle that is announced to all players.
 */
public final class EntityCleanupScheduler {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final long TICKS_PER_MINUTE = 20L * 60L;
    private static final long TICKS_PER_SECOND = 20L;
    private static final long MILLIS_PER_MINUTE = 60_000L;
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final String CANCEL_PERMISSION = "ezclean.cancel";

    private static final int PRE_CLEAN_SUMMARY_LIMIT = 3;
    private static final int STATS_SUMMARY_LIMIT = 4;
    private static final String EMPTY_SUMMARY_PLACEHOLDER = "None";

    private final JavaPlugin plugin;
    private List<CleanupSettings> cleanupSettings;
    private final @Nullable WorldGuardCleanupBypass worldGuardBypass;
    private final CleanupStatsTracker statsTracker;
    private final RemovalEvaluator removalEvaluator;
    private final BroadcastService broadcastService = new BroadcastService();

    private final List<ScheduledCleanup> scheduledCleanups = new ArrayList<>();
    private final Map<String, Long> statsSummaryRuns = new HashMap<>();
    private final Map<String, PreCleanSummary> cachedPreCleanSummaries = new HashMap<>();

    public EntityCleanupScheduler(JavaPlugin plugin, List<CleanupSettings> settings,
            @Nullable WorldGuardCleanupBypass worldGuardBypass, CleanupStatsTracker statsTracker) {
        this.plugin = plugin;
        this.cleanupSettings = new ArrayList<>(settings);
        this.worldGuardBypass = worldGuardBypass;
        this.statsTracker = statsTracker;
        this.removalEvaluator = new RemovalEvaluator(worldGuardBypass);
    }

    /**
     * Starts the repeating cleanup tasks.
     */
    public void enable() {
        for (CleanupSettings settings : cleanupSettings) {
            scheduleCleanupTasks(settings);
        }
    }

    /**
     * Cancels all scheduled cleanup tasks.
     */
    public void disable() {
        for (ScheduledCleanup scheduled : scheduledCleanups) {
            scheduled.cancel();
        }
        scheduledCleanups.clear();
    }

    /**
     * Reloads the scheduler by restarting the repeating tasks with updated settings.
     */
    public void reload(List<CleanupSettings> newSettings) {
        this.cleanupSettings = new ArrayList<>(newSettings);
        disable();
        enable();
    }

    /**
     * Returns a snapshot of the configured cleaner identifiers.
     *
     * @return immutable list of cleaner identifiers
     */
    public List<String> getCleanerIds() {
        List<String> cleanerIds = new ArrayList<>();
        for (CleanupSettings settings : cleanupSettings) {
            cleanerIds.add(settings.getCleanerId());
        }
        return Collections.unmodifiableList(cleanerIds);
    }

    /**
     * Immediately executes the cleanup routine for the requested cleaner.
     *
     * @param cleanerId the identifier of the cleaner to trigger
     * @return {@code true} if the cleaner was found and executed
     */
    public boolean triggerCleanup(String cleanerId) {
        if (cleanerId == null) {
            return false;
        }
        String normalized = cleanerId.toLowerCase(Locale.ROOT);
        for (ScheduledCleanup scheduled : scheduledCleanups) {
            if (scheduled.matchesCleaner(normalized)) {
                scheduled.triggerNow();
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the remaining time until the next scheduled cleanup for the requested profile.
     *
     * @param cleanerId the identifier of the cleaner to inspect
     * @return the duration remaining, or {@code null} if the cleaner is not scheduled
     */
    public Duration getTimeUntilCleanup(String cleanerId) {
        ScheduledCleanup scheduled = findScheduled(cleanerId);
        return scheduled != null ? scheduled.getTimeUntilCleanup() : null;
    }

    /**
     * Returns the configuration for the specified cleaner, if available.
     *
     * @param cleanerId the identifier of the cleaner to inspect
     * @return the configuration associated with the cleaner
     */
    public @Nullable CleanupSettings getSettings(String cleanerId) {
        ScheduledCleanup scheduled = findScheduled(cleanerId);
        if (scheduled != null) {
            return scheduled.getSettings();
        }
        if (cleanerId == null) {
            return null;
        }
        String normalized = cleanerId.toLowerCase(Locale.ROOT);
        for (CleanupSettings settings : cleanupSettings) {
            if (settings.getCleanerId().toLowerCase(Locale.ROOT).equals(normalized)) {
                return settings;
            }
        }
        return null;
    }

    /**
     * Cancels the pending cleanup for the specified profile and restarts its countdown.
     *
     * @param cleanerId the identifier of the cleaner to cancel
     * @return the duration until the rescheduled cleanup, or {@code null} if the cleaner is not scheduled
     */
    public @Nullable Duration cancelNextCleanup(String cleanerId) {
        ScheduledCleanup scheduled = findScheduled(cleanerId);
        return scheduled != null ? scheduled.cancelNextRun() : null;
    }

    private void scheduleCleanupTasks(CleanupSettings settings) {
        ScheduledCleanup scheduled = new ScheduledCleanup(settings);
        scheduled.start();
        scheduledCleanups.add(scheduled);
    }

    private void performCleanup(CleanupSettings settings) {
        long startNanos = System.nanoTime();
        Double tpsBefore = captureTpsSample();
        if (settings.isStartBroadcastEnabled()) {
            Component message = MINI_MESSAGE.deserialize(settings.getStartMessageTemplate(),
                    Placeholder.parsed("cleaner", settings.getCleanerId()));
            String legacyMessage = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(message);
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(legacyMessage);
            }
            Bukkit.getServer().getConsoleSender().sendMessage(legacyMessage);
        }

        EntityPileDetector pileDetector = EntityPileDetector.create(settings.getPileDetectionSettings());

        // Only build summary tracker when the pre-clean template is configured — avoids allocating
        // tracking state (and the recordEntity() call overhead) when the feature is unused.
        boolean needsSummary = settings.getPreCleanMessageTemplate() != null
                && !settings.getPreCleanMessageTemplate().isBlank();
        EntityPileDetector summaryTracker = needsSummary
                ? EntityPileDetector.createSummaryTracker(settings.getPileDetectionSettings())
                : null;

        if (!FoliaScheduler.isFolia() && settings.isAsyncRemoval()) {
            // Paper/Spigot with async-removal: true — run the entity scan off the main thread,
            // then dispatch removal batches and finishCleanup back to the main thread.
            // Folia is excluded: world.getEntities() must be called from the owning region thread.
            FoliaScheduler.runAsync(plugin, () -> {
                List<Entity> toRemove = new ArrayList<>();
                Map<String, Integer> groupCounts = new HashMap<>();
                Map<String, Integer> worldCounts = new HashMap<>();
                for (World world : Bukkit.getWorlds()) {
                    if (!settings.isWorldEnabled(world.getName())) {
                        continue;
                    }
                    for (Entity entity : world.getEntities()) {
                        String removalGroup = removalEvaluator.evaluateRemovalGroup(entity, settings, pileDetector);
                        if (removalGroup != null) {
                            toRemove.add(entity);
                            groupCounts.merge(removalGroup, 1, Integer::sum);
                            worldCounts.merge(world.getName(), 1, Integer::sum);
                            if (summaryTracker != null) {
                                summaryTracker.recordEntity(entity);
                            }
                        }
                    }
                }
                // Compute the pre-clean summary while still on the async thread; it is written to
                // cachedPreCleanSummaries on the main thread to avoid publishing across threads.
                final PreCleanSummary preSummary = summaryTracker != null
                        ? new PreCleanSummary(
                                formatTopWorlds(summaryTracker.getTopWorlds(PRE_CLEAN_SUMMARY_LIMIT)),
                                formatTopChunks(summaryTracker.getTopChunks(PRE_CLEAN_SUMMARY_LIMIT)))
                        : null;

                // Hand removal and post-cleanup back to the main thread so that
                // entity.remove() and finishCleanup always run on the correct thread.
                FoliaScheduler.runGlobalLater(plugin, () -> {
                    if (preSummary != null) {
                        cachedPreCleanSummaries.put(settings.getCleanerId(), preSummary);
                    }
                    if (toRemove.size() > settings.getAsyncRemovalBatchSize()) {
                        scheduleEntityRemovalBatches(toRemove, settings, startNanos, tpsBefore, groupCounts, worldCounts);
                    } else {
                        for (Entity entity : toRemove) {
                            entity.remove();
                        }
                        finishCleanup(settings, toRemove.size(), startNanos, tpsBefore, groupCounts, worldCounts);
                    }
                }, 1L);
            });
            return;
        }

        // Folia (any async-removal setting) or Paper/Spigot with async-removal: false —
        // keep the existing synchronous scan + removal path unchanged.
        List<Entity> toRemove = new ArrayList<>();
        Map<String, Integer> groupCounts = new HashMap<>();
        Map<String, Integer> worldCounts = new HashMap<>();
        for (World world : Bukkit.getWorlds()) {
            if (!settings.isWorldEnabled(world.getName())) {
                continue;
            }
            for (Entity entity : world.getEntities()) {
                String removalGroup = removalEvaluator.evaluateRemovalGroup(entity, settings, pileDetector);
                if (removalGroup != null) {
                    toRemove.add(entity);
                    groupCounts.merge(removalGroup, 1, Integer::sum);
                    worldCounts.merge(world.getName(), 1, Integer::sum);
                    if (summaryTracker != null) {
                        summaryTracker.recordEntity(entity);
                    }
                }
            }
        }

        // Cache summary data built during this run so the next warning broadcast
        // can use it without triggering an additional full entity scan.
        if (summaryTracker != null) {
            String topWorlds = formatTopWorlds(summaryTracker.getTopWorlds(PRE_CLEAN_SUMMARY_LIMIT));
            String topChunks = formatTopChunks(summaryTracker.getTopChunks(PRE_CLEAN_SUMMARY_LIMIT));
            cachedPreCleanSummaries.put(settings.getCleanerId(), new PreCleanSummary(topWorlds, topChunks));
        }

        if (FoliaScheduler.isFolia() && settings.isAsyncRemoval()) {
            // Folia with async-removal: true — dispatch all removals to entity region threads in a
            // single pass. FoliaScheduler.removeEntity calls entity.getScheduler().run(), which just
            // registers the task (cheap). The actual entity.remove() executes on each entity's owning
            // region thread so all removals run concurrently without blocking the global region thread.
            int total = toRemove.size();
            for (Entity entity : toRemove) {
                if (entity.isValid()) {
                    FoliaScheduler.removeEntity(entity, plugin);
                }
            }
            FoliaScheduler.runGlobalLater(plugin,
                    () -> finishCleanup(settings, total, startNanos, tpsBefore, groupCounts, worldCounts), 1L);
            return;
        }

        // Synchronous removal on main/global thread (all platforms with async-removal: false).
        for (Entity entity : toRemove) {
            entity.remove();
        }
        finishCleanup(settings, toRemove.size(), startNanos, tpsBefore, groupCounts, worldCounts);
    }

    private void scheduleEntityRemovalBatches(List<Entity> toRemove, CleanupSettings settings,
            long startNanos, Double tpsBefore, Map<String, Integer> groupCounts, Map<String, Integer> worldCounts) {
        int total = toRemove.size();
        int batchSize = settings.getAsyncRemovalBatchSize();
        int numBatches = (total + batchSize - 1) / batchSize;
        for (int i = 0; i < numBatches; i++) {
            int batchStart = i * batchSize;
            int batchEnd = Math.min(batchStart + batchSize, total);
            boolean isLastBatch = (i == numBatches - 1);
            long delayTicks = (long) i + 1L; // 1-indexed; one batch per tick
            FoliaScheduler.runGlobalLater(plugin, () -> {
                for (int j = batchStart; j < batchEnd; j++) {
                    Entity entity = toRemove.get(j);
                    if (entity.isValid()) {
                        FoliaScheduler.removeEntity(entity, plugin);
                    }
                }
                if (isLastBatch) {
                    finishCleanup(settings, total, startNanos, tpsBefore, groupCounts, worldCounts);
                }
            }, delayTicks);
        }
    }

    private void finishCleanup(CleanupSettings settings, int removed, long startNanos,
            Double tpsBefore, Map<String, Integer> groupCounts, Map<String, Integer> worldCounts) {
        long durationMillis = Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
        Double tpsAfter = captureTpsSample();
        Double tpsImpact = null;
        if (tpsBefore != null && tpsAfter != null) {
            tpsImpact = tpsAfter - tpsBefore;
        }
        statsTracker.recordRun(settings.getCleanerId(),
                CleanupStatsTracker.CleanupRunStats.create(removed, durationMillis, tpsBefore, tpsAfter, tpsImpact,
                        groupCounts, worldCounts));
        maybeBroadcastStatsSummary(settings);

        if (settings.isSummaryBroadcastEnabled()) {
            Component message = MINI_MESSAGE.deserialize(settings.getSummaryMessageTemplate(),
                    Placeholder.parsed("count", Integer.toString(removed)),
                    Placeholder.parsed("minutes", Long.toString(settings.getCleanupIntervalMinutes())),
                    Placeholder.parsed("cleaner", settings.getCleanerId()));
            String legacyMessage = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(message);
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(legacyMessage);
            }
            Bukkit.getServer().getConsoleSender().sendMessage(legacyMessage);
        }
    }

    private void maybeBroadcastStatsSummary(CleanupSettings settings) {
        if (!settings.isStatsSummaryBroadcastEnabled()) {
            return;
        }
        long runCount = statsSummaryRuns.merge(settings.getCleanerId(), 1L, Long::sum);
        if (runCount % settings.getStatsSummaryEveryRuns() != 0L) {
            return;
        }

        CleanupStatsTracker.CleanupStatsSnapshot snapshot = statsTracker.getSnapshot(settings.getCleanerId());
        if (snapshot == null) {
            return;
        }

        String avgDuration = formatDurationMillis(snapshot.totals().averageDurationMillis());
        String avgTpsImpact = formatTpsImpact(snapshot.totals().averageTpsImpact());
        String topGroups = formatStatEntries(snapshot.totals().groupTotals(), STATS_SUMMARY_LIMIT);
        String topWorlds = formatStatEntries(snapshot.totals().worldTotals(), STATS_SUMMARY_LIMIT);

        CleanupStatsTracker.CleanupStatsSnapshot.LastRun lastRun = snapshot.lastRun();
        String lastRemoved = lastRun == null ? "0" : Long.toString(lastRun.removed());
        String lastDuration = lastRun == null ? "N/A" : formatDurationMillis(lastRun.durationMillis());
        String lastTpsImpact = lastRun == null ? "N/A" : formatTpsImpact(lastRun.tpsImpact());

        Component message = MINI_MESSAGE.deserialize(settings.getStatsSummaryMessageTemplate(),
                Placeholder.parsed("cleaner", settings.getCleanerId()),
                Placeholder.parsed("runs", Long.toString(snapshot.totals().runs())),
                Placeholder.parsed("total_removed", Long.toString(snapshot.totals().removed())),
                Placeholder.parsed("avg_duration", avgDuration),
                Placeholder.parsed("avg_tps_impact", avgTpsImpact),
                Placeholder.parsed("top_groups", topGroups),
                Placeholder.parsed("top_worlds", topWorlds),
                Placeholder.parsed("last_removed", lastRemoved),
                Placeholder.parsed("last_duration", lastDuration),
                Placeholder.parsed("last_tps_impact", lastTpsImpact));

        String legacyMessage = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                .serialize(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(legacyMessage);
        }
        Bukkit.getServer().getConsoleSender().sendMessage(legacyMessage);
    }

    // Backwards-compatibility helper used by existing unit tests which reflectively
    // invoke this method. Delegates to RemovalEvaluator so logic remains testable.
    private @Nullable String evaluateRemovalGroup(Entity entity, CleanupSettings settings,
            @Nullable EntityPileDetector pileDetector) {
        return removalEvaluator.evaluateRemovalGroup(entity, settings, pileDetector);
    }

    private void sendWarningBroadcast(CleanupSettings settings, long minutesRemaining) {
        Component message = MINI_MESSAGE.deserialize(settings.getWarningMessageTemplate(),
                createBroadcastPlaceholders(settings, minutesRemaining, minutesRemaining * 60L));
        broadcastCountdownMessage(settings, message);
    }

    private void sendPreCleanBroadcast(CleanupSettings settings, long minutesRemaining) {
        String template = settings.getPreCleanMessageTemplate();
        if (template == null || template.isBlank()) {
            return;
        }
        PreCleanSummary summary = collectPreCleanSummary(settings);
        List<TagResolver> placeholders = new ArrayList<>();
        Collections.addAll(placeholders, createBroadcastPlaceholders(settings, minutesRemaining, minutesRemaining * 60L));
        placeholders.add(Placeholder.parsed("top_worlds", summary.topWorlds()));
        placeholders.add(Placeholder.parsed("top_chunks", summary.topChunks()));
        Component message = MINI_MESSAGE.deserialize(template, placeholders.toArray(TagResolver[]::new));
        broadcastCountdownMessage(settings, message);
    }

    private void sendIntervalBroadcast(CleanupSettings settings, long minutesRemaining) {
        Component message = MINI_MESSAGE.deserialize(settings.getIntervalBroadcastMessageTemplate(),
                createBroadcastPlaceholders(settings, minutesRemaining, minutesRemaining * 60L));
        broadcastCountdownMessage(settings, message);
    }

    private void sendDynamicBroadcast(CleanupSettings settings, long totalSecondsRemaining) {
        long minutesRemaining = totalSecondsRemaining / 60L;
        Component message = MINI_MESSAGE.deserialize(settings.getDynamicBroadcastMessageTemplate(),
                createBroadcastPlaceholders(settings, minutesRemaining, totalSecondsRemaining));
        broadcastCountdownMessage(settings, message);
    }

    private void handleCountdownBroadcasts(CleanupSettings settings, long totalSecondsRemaining) {
        long minutesRemaining = totalSecondsRemaining / 60L;

        // Warning (minute-based) - only trigger on whole minute boundaries
        if (settings.isWarningEnabled() && (totalSecondsRemaining % 60L == 0L)
                && minutesRemaining == settings.getWarningMinutesBefore()) {
            sendWarningBroadcast(settings, minutesRemaining);
            sendPreCleanBroadcast(settings, minutesRemaining);
        }

        if (settings.isDynamicBroadcastEnabled()) {
            Set<Long> dynamicMinutes = settings.getDynamicBroadcastMinutes();
            Set<Long> dynamicSeconds = settings.getDynamicBroadcastSeconds();
            // minute-based dynamic reminders (on whole minute)
            if ((totalSecondsRemaining % 60L == 0L) && dynamicMinutes.contains(minutesRemaining)) {
                sendDynamicBroadcast(settings, totalSecondsRemaining);
            }
            // seconds-based dynamic reminders (exact second matches)
            if (dynamicSeconds.contains(totalSecondsRemaining)) {
                sendDynamicBroadcast(settings, totalSecondsRemaining);
            }
        }

        if (totalSecondsRemaining <= 0L) {
            return;
        }

        if (settings.isIntervalBroadcastEnabled()) {
            long intervalMinutes = settings.getIntervalBroadcastMinutes();
            if (intervalMinutes > 0L && minutesRemaining % intervalMinutes == 0L && (totalSecondsRemaining % 60L == 0L)) {
                sendIntervalBroadcast(settings, minutesRemaining);
            }
        }
    }

    private TagResolver[] createBroadcastPlaceholders(CleanupSettings settings, long minutesRemaining, long totalSecondsRemaining) {
        List<TagResolver> placeholders = new ArrayList<>();
        placeholders.add(Placeholder.parsed("minutes", Long.toString(minutesRemaining)));
        placeholders.add(Placeholder.parsed("seconds", Long.toString(totalSecondsRemaining)));
        placeholders.add(Placeholder.parsed("cleaner", settings.getCleanerId()));
        CleanupCancelSettings cancelSettings = settings.getCancelSettings();
        if (cancelSettings.isEnabled()) {
            placeholders.add(Placeholder.parsed("cost", cancelSettings.getFormattedCost()));
        }
        return placeholders.toArray(TagResolver[]::new);
    }

    private void broadcastCountdownMessage(CleanupSettings settings, Component baseMessage) {
        CleanupCancelSettings cancelSettings = settings.getCancelSettings();
        if (!cancelSettings.isEnabled()) {
            broadcastService.broadcast(baseMessage);
            return;
        }

        Component interactiveMessage = applyCancelInteraction(settings, baseMessage);
        broadcastService.broadcastWithCancel(baseMessage, interactiveMessage, CANCEL_PERMISSION);
    }

    private Component applyCancelInteraction(CleanupSettings settings, Component message) {
        CleanupCancelSettings cancelSettings = settings.getCancelSettings();
        if (!cancelSettings.isEnabled()) {
            return message;
        }
        String command = "/ezclean cancel " + settings.getCleanerId();
        message = message.clickEvent(ClickEvent.runCommand(command));
        String hoverTemplate = cancelSettings.getHoverMessage();
        if (hoverTemplate != null && !hoverTemplate.isEmpty()) {
            Component hover = MINI_MESSAGE.deserialize(hoverTemplate,
                    Placeholder.parsed("cleaner", settings.getCleanerId()),
                    Placeholder.parsed("cost", cancelSettings.getFormattedCost()));
            message = message.hoverEvent(HoverEvent.showText(hover));
        }
        return message;
    }

    private PreCleanSummary collectPreCleanSummary(CleanupSettings settings) {
        // Prefer data cached from the most recent cleanup run — avoids a second full entity scan.
        PreCleanSummary cached = cachedPreCleanSummaries.get(settings.getCleanerId());
        if (cached != null) {
            return cached;
        }
        // Fallback: first-run scan (no prior cleanup has been recorded yet for this profile).
        EntityPileDetector pileDetector = EntityPileDetector.createSummaryTracker(settings.getPileDetectionSettings());
        for (World world : Bukkit.getWorlds()) {
            if (!settings.isWorldEnabled(world.getName())) {
                continue;
            }
            for (Entity entity : world.getEntities()) {
                if (removalEvaluator.evaluateRemovalGroup(entity, settings, pileDetector) != null) {
                    pileDetector.recordEntity(entity);
                }
            }
        }
        String topWorlds = formatTopWorlds(pileDetector.getTopWorlds(PRE_CLEAN_SUMMARY_LIMIT));
        String topChunks = formatTopChunks(pileDetector.getTopChunks(PRE_CLEAN_SUMMARY_LIMIT));
        return new PreCleanSummary(topWorlds, topChunks);
    }

    private static String formatTopWorlds(List<EntityPileDetector.WorldCount> worlds) {
        if (worlds.isEmpty()) {
            return EMPTY_SUMMARY_PLACEHOLDER;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < worlds.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            EntityPileDetector.WorldCount entry = worlds.get(i);
            builder.append(entry.worldName()).append(" (").append(entry.count()).append(")");
        }
        return builder.toString();
    }

    private static String formatTopChunks(List<EntityPileDetector.ChunkCount> chunks) {
        if (chunks.isEmpty()) {
            return EMPTY_SUMMARY_PLACEHOLDER;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
                EntityPileDetector.ChunkCount entry = chunks.get(i);
                builder.append(entry.worldName())
                    .append(" [")
                    .append(entry.chunkX())
                    .append(", ")
                    .append(entry.chunkZ())
                    .append("] (")
                    .append(entry.count())
                    .append(")");
        }
        return builder.toString();
    }

    private static String formatDurationMillis(@Nullable Long durationMillis) {
        if (durationMillis == null) {
            return "N/A";
        }
        if (durationMillis < 1000L) {
            return durationMillis + "ms";
        }
        double seconds = durationMillis / 1000.0;
        return String.format(Locale.ROOT, "%.2fs", seconds);
    }

    private static String formatTpsImpact(@Nullable Double tpsImpact) {
        if (tpsImpact == null) {
            return "N/A";
        }
        return String.format(Locale.ROOT, "%.2f", tpsImpact);
    }

    private static String formatStatEntries(Map<String, Long> entries, int limit) {
        if (entries.isEmpty() || limit <= 0) {
            return EMPTY_SUMMARY_PLACEHOLDER;
        }
        List<Map.Entry<String, Long>> sorted = new ArrayList<>(entries.entrySet());
        sorted.sort((left, right) -> {
            int countCompare = Long.compare(right.getValue(), left.getValue());
            if (countCompare != 0) {
                return countCompare;
            }
            return left.getKey().compareToIgnoreCase(right.getKey());
        });
        StringBuilder builder = new StringBuilder();
        int max = Math.min(limit, sorted.size());
        for (int i = 0; i < max; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            Map.Entry<String, Long> entry = sorted.get(i);
            builder.append(entry.getKey()).append(" (").append(entry.getValue()).append(")");
        }
        return builder.toString();
    }

    private static @Nullable Double captureTpsSample() {
        try {
            Object result = Bukkit.getServer().getClass().getMethod("getTPS").invoke(Bukkit.getServer());
            if (result instanceof double[] values && values.length > 0) {
                return values[0];
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
        return null;
    }

    private @Nullable ScheduledCleanup findScheduled(String cleanerId) {
        if (cleanerId == null) {
            return null;
        }
        String normalized = cleanerId.toLowerCase(Locale.ROOT);
        for (ScheduledCleanup scheduled : scheduledCleanups) {
            if (scheduled.matchesCleaner(normalized)) {
                return scheduled;
            }
        }
        return null;
    }

    private final class ScheduledCleanup {

        private final CleanupSettings settings;
        private final String normalizedCleanerId;
        private long secondsUntilCleanup;
        private Runnable cancelCountdownTask;
        private long lastTickMillis;

        private ScheduledCleanup(CleanupSettings settings) {
            this.settings = settings;
            this.normalizedCleanerId = settings.getCleanerId().toLowerCase(Locale.ROOT);
        }

        private void start() {
            secondsUntilCleanup = Math.max(1L, settings.getCleanupIntervalMinutes()) * 60L;
            lastTickMillis = System.currentTimeMillis();
            cancelCountdownTask = FoliaScheduler.runGlobalTimer(
                    plugin, this::tick, TICKS_PER_SECOND, TICKS_PER_SECOND);
        }

        private void tick() {
            long now = System.currentTimeMillis();
            secondsUntilCleanup--;
            if (secondsUntilCleanup < 0L) {
                secondsUntilCleanup = 0L;
            }
            handleCountdownBroadcasts(settings, secondsUntilCleanup);
            if (secondsUntilCleanup <= 0L) {
                performCleanup(settings);
                secondsUntilCleanup = Math.max(1L, settings.getCleanupIntervalMinutes()) * 60L;
            }
            lastTickMillis = now;
        }

        private void cancel() {
            if (cancelCountdownTask != null) {
                cancelCountdownTask.run();
                cancelCountdownTask = null;
            }
        }

        private boolean matchesCleaner(String normalizedCleanerId) {
            return this.normalizedCleanerId.equals(normalizedCleanerId);
        }

        private void triggerNow() {
            performCleanup(settings);
            secondsUntilCleanup = Math.max(1L, settings.getCleanupIntervalMinutes()) * 60L;
            lastTickMillis = System.currentTimeMillis();
        }

        private Duration cancelNextRun() {
            secondsUntilCleanup = Math.max(1L, settings.getCleanupIntervalMinutes()) * 60L;
            lastTickMillis = System.currentTimeMillis();
            return Duration.ofSeconds(secondsUntilCleanup);
        }

        private Duration getTimeUntilCleanup() {
            long now = System.currentTimeMillis();
            long elapsedSinceLastTick = Math.max(0L, now - lastTickMillis);
            long remainingMillis = (secondsUntilCleanup * MILLIS_PER_SECOND) - elapsedSinceLastTick;
            if (remainingMillis <= 0L) {
                return Duration.ZERO;
            }
            return Duration.ofMillis(remainingMillis);
        }

        private CleanupSettings getSettings() {
            return settings;
        }
    }

    private record PreCleanSummary(String topWorlds, String topChunks) {
    }

}
