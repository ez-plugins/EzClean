package com.skyblockexp.ezclean.stats;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UsageSnapshot {
    private final List<Map.Entry<String, UsageCounts>> entries;
    private final int totalPendingSync;
    private final int totalPendingAsync;
    private final int totalActiveAsyncWorkers;
    private final int maxAsyncLoad;
    private final long totalMemory;
    private final long usedMemory;

    private UsageSnapshot(List<Map.Entry<String, UsageCounts>> entries, int totalPendingSync,
            int totalPendingAsync, int totalActiveAsyncWorkers, int maxAsyncLoad, long totalMemory, long usedMemory) {
        this.entries = Collections.unmodifiableList(entries);
        this.totalPendingSync = totalPendingSync;
        this.totalPendingAsync = totalPendingAsync;
        this.totalActiveAsyncWorkers = totalActiveAsyncWorkers;
        this.maxAsyncLoad = maxAsyncLoad;
        this.totalMemory = totalMemory;
        this.usedMemory = usedMemory;
    }

    public static UsageSnapshot from(Map<String, UsageCounts> usageByPlugin, String filter) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        if (usageByPlugin.isEmpty()) {
            return new UsageSnapshot(Collections.emptyList(), 0, 0, 0, 0, totalMemory, usedMemory);
        }

        String normalizedFilter = filter != null ? filter.toLowerCase(Locale.ROOT) : null;
        List<Map.Entry<String, UsageCounts>> filtered = new ArrayList<>();
        int totalPendingSync = 0;
        int totalPendingAsync = 0;
        int totalActiveAsyncWorkers = 0;
        int maxAsyncLoad = 0;

        for (Map.Entry<String, UsageCounts> entry : usageByPlugin.entrySet()) {
            String pluginName = entry.getKey();
            if (normalizedFilter != null
                    && !pluginName.toLowerCase(Locale.ROOT).contains(normalizedFilter)) {
                continue;
            }

            UsageCounts counts = entry.getValue();
            filtered.add(new AbstractMap.SimpleEntry<>(pluginName, counts));
            totalPendingSync += counts.getPendingSync();
            totalPendingAsync += counts.getPendingAsync();
            totalActiveAsyncWorkers += counts.getActiveAsyncWorkers();
            int asyncLoad = counts.getPendingAsync() + counts.getActiveAsyncWorkers();
            if (asyncLoad > maxAsyncLoad) {
                maxAsyncLoad = asyncLoad;
            }
        }

        filtered.sort(Comparator
                .comparingInt((Map.Entry<String, UsageCounts> entry) -> calculateResourceScore(entry.getValue()))
                .reversed()
                .thenComparing(Map.Entry::getKey));

        if (filtered.isEmpty()) {
            return new UsageSnapshot(Collections.emptyList(), 0, 0, 0, 0, totalMemory, usedMemory);
        }

        return new UsageSnapshot(filtered, totalPendingSync, totalPendingAsync, totalActiveAsyncWorkers,
                maxAsyncLoad, totalMemory, usedMemory);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public List<Map.Entry<String, UsageCounts>> entries() {
        return entries;
    }

    public int totalPendingSync() {
        return totalPendingSync;
    }

    public int totalPendingAsync() {
        return totalPendingAsync;
    }

    public int totalActiveAsyncWorkers() {
        return totalActiveAsyncWorkers;
    }

    public int maxAsyncLoad() {
        return maxAsyncLoad;
    }

    public int pluginCount() {
        return entries.size();
    }

    public long totalMemory() {
        return totalMemory;
    }

    public long usedMemory() {
        return usedMemory;
    }

    private static int calculateResourceScore(UsageCounts counts) {
        int score = counts.getTotal(); // Base score from scheduler activity

        // Add thread count (threads are resource intensive)
        score += counts.getThreadCount() * 2;

        // Add CPU time contribution (convert nanoseconds to a reasonable score)
        // 1 second of CPU time = 1000 points
        long cpuSeconds = counts.getCpuTime() / 1_000_000_000L;
        score += (int) (cpuSeconds * 1000);

        // Add jar size contribution (larger plugins might be more complex)
        // 1MB jar size = 100 points
        long jarMB = counts.getJarSize() / (1024 * 1024);
        score += (int) (jarMB * 100);

        return score;
    }
}
