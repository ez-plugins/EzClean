package com.skyblockexp.ezclean.command;

import com.skyblockexp.ezclean.EzCleanPlugin;
import com.skyblockexp.ezclean.stats.LiveUsageSubscription;
import com.skyblockexp.ezclean.stats.UsageCounts;
import com.skyblockexp.ezclean.stats.UsageSnapshot;
import com.skyblockexp.ezclean.util.FoliaScheduler;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitWorker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the "usage" subcommand for viewing scheduler usage statistics.
 */
public class UsageSubcommand implements Subcommand {

    private static final int ASYNC_GRAPH_WIDTH = 20;
    private static final long LIVE_MONITOR_PERIOD_TICKS = 10L;
    private static final int LIVE_CHAT_INTERVAL_CYCLES = 8;

    private final EzCleanPlugin plugin;
    private final Map<UUID, LiveUsageSubscription> liveUsageSessions = new HashMap<>();

    public UsageSubcommand(EzCleanPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length > 2) {
            sendUsage(sender, label);
            return true;
        }

        boolean stopRequested = false;
        boolean liveRequested = false;
        String requestedPlugin = null;

        if (args.length >= 1) {
            String argument = args[0];
            if ("stop".equalsIgnoreCase(argument)) {
                stopRequested = true;
                if (args.length > 1) {
                    sendUsage(sender, label);
                    return true;
                }
            } else if ("live".equalsIgnoreCase(argument)) {
                liveRequested = true;
                if (args.length == 2) {
                    requestedPlugin = args[1];
                } else if (args.length > 2) {
                    sendUsage(sender, label);
                    return true;
                }
            } else {
                if (args.length > 1) {
                    sendUsage(sender, label);
                    return true;
                }
                requestedPlugin = argument;
            }
        }

        if (stopRequested) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players may stop the live usage monitor.");
                return true;
            }
            boolean cancelled = cancelLiveUsage(player.getUniqueId());
            if (cancelled) {
                sender.sendMessage(ChatColor.GREEN + "Stopped the live async usage monitor.");
            } else {
                sender.sendMessage(ChatColor.RED + "You do not currently have a live usage monitor running.");
            }
            return true;
        }

        if (liveRequested) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players may view the live usage monitor.");
                return true;
            }
            startLiveUsageMonitor(player, label, requestedPlugin);
            return true;
        }

        UsageSnapshot snapshot = createUsageSnapshot(requestedPlugin);
        if (snapshot.isEmpty()) {
            if (requestedPlugin == null) {
                sender.sendMessage(ChatColor.GREEN + "No scheduler activity detected.");
            } else {
                sender.sendMessage(ChatColor.RED + "No scheduler activity found for plugin \"" + requestedPlugin + "\".");
            }
            if (sender instanceof Player) {
                sender.sendMessage(ChatColor.DARK_GRAY + "Tip: Use /" + label + " usage live"
                        + (requestedPlugin != null ? " " + requestedPlugin : "")
                        + " for a live async graph. Use /" + label + " usage stop to cancel.");
            }
            return true;
        }

        sendUsageSnapshot(sender, snapshot, false, requestedPlugin, label, sender instanceof Player);
        return true;
    }

    @Override
    public String getName() {
        return "usage";
    }

    @Override
    public String getPermission() {
        return "ezclean.usage";
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> suggestions = new ArrayList<>();
            for (String option : List.of("live", "stop")) {
                if (option.startsWith(prefix)) {
                    suggestions.add(option);
                }
            }
            for (String pluginName : collectSchedulerUsage().keySet()) {
                if (pluginName.toLowerCase().startsWith(prefix)) {
                    suggestions.add(pluginName);
                }
            }
            return suggestions;
        }
        if (args.length == 2 && "live".equalsIgnoreCase(args[0])) {
            String prefix = args[1].toLowerCase();
            List<String> suggestions = new ArrayList<>();
            for (String pluginName : collectSchedulerUsage().keySet()) {
                if (pluginName.toLowerCase().startsWith(prefix)) {
                    suggestions.add(pluginName);
                }
            }
            return suggestions;
        }
        return Collections.emptyList();
    }

    private UsageSnapshot createUsageSnapshot(@Nullable String filter) {
        Map<String, UsageCounts> usageByPlugin = collectSchedulerUsage();
        return UsageSnapshot.from(usageByPlugin, filter);
    }

    private void sendUsageSnapshot(CommandSender target, UsageSnapshot snapshot, boolean live,
            @Nullable String filter, String label, boolean includeInstructions) {
        for (String line : formatUsageSnapshot(snapshot, live, filter, label, includeInstructions)) {
            target.sendMessage(line);
        }
    }

    private List<String> formatUsageSnapshot(UsageSnapshot snapshot, boolean live, @Nullable String filter,
            String label, boolean includeInstructions) {
        List<String> lines = new ArrayList<>();

        String headerColor = live ? ChatColor.DARK_AQUA.toString() : ChatColor.GOLD.toString();
        String headerTitle = live ? "Live async scheduler usage" : "Scheduler usage overview";
        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append(headerColor).append(ChatColor.BOLD).append(headerTitle).append(ChatColor.RESET)
                .append(ChatColor.DARK_GRAY).append(" • plugins: ").append(ChatColor.AQUA)
                .append(snapshot.pluginCount());
        if (filter != null && !filter.isEmpty()) {
            headerBuilder.append(ChatColor.DARK_GRAY).append(" • filter: ")
                    .append(ChatColor.AQUA).append(filter);
        }
        lines.add(headerBuilder.toString());

        if (snapshot.isEmpty()) {
            if (filter == null || filter.isEmpty()) {
                lines.add(ChatColor.GRAY + "No scheduler activity detected.");
            } else {
                lines.add(ChatColor.GRAY + "No scheduler activity currently matches the filter.");
            }
            if (includeInstructions) {
                lines.add(ChatColor.DARK_GRAY + "Use /" + label + " usage stop to cancel the live view.");
            }
            return lines;
        }

        lines.add(ChatColor.GRAY + "Pending sync: " + ChatColor.AQUA + snapshot.totalPendingSync()
                + ChatColor.GRAY + " | Pending async: " + ChatColor.AQUA + snapshot.totalPendingAsync()
                + ChatColor.GRAY + " | Active async workers: " + ChatColor.AQUA
                + snapshot.totalActiveAsyncWorkers());

        long usedMB = snapshot.usedMemory() / 1024 / 1024;
        long totalMB = snapshot.totalMemory() / 1024 / 1024;
        lines.add(ChatColor.GRAY + "Memory: " + ChatColor.AQUA + usedMB + "/" + totalMB + " MB");

        Map.Entry<String, UsageCounts> peakEntry = snapshot.entries().get(0);
        UsageCounts peakCounts = peakEntry.getValue();
        int peakAsync = peakCounts.getPendingAsync() + peakCounts.getActiveAsyncWorkers();
        lines.add(ChatColor.GRAY + "Top async load: " + ChatColor.AQUA + peakEntry.getKey() + ChatColor.GRAY
                + " with " + ChatColor.AQUA + peakAsync + ChatColor.GRAY + " async tasks ("
                + ChatColor.AQUA + peakCounts.getPendingAsync() + ChatColor.GRAY + " pending, "
                + ChatColor.AQUA + peakCounts.getActiveAsyncWorkers() + ChatColor.GRAY + " workers).");

        int limit = live ? 5 : 10;
        int displayed = 0;
        for (Map.Entry<String, UsageCounts> entry : snapshot.entries()) {
            if (displayed >= limit) {
                break;
            }
            lines.add(formatUsageLine(entry, snapshot.maxAsyncLoad()));
            displayed++;
        }

        if (snapshot.pluginCount() > limit) {
            int remaining = snapshot.pluginCount() - limit;
            lines.add(ChatColor.DARK_GRAY + "… plus " + ChatColor.AQUA + remaining + ChatColor.DARK_GRAY
                    + " more plugin" + (remaining == 1 ? "" : "s") + ".");
        }

        if (includeInstructions) {
            lines.add(ChatColor.DARK_GRAY + "Tip: Use /" + label + " usage live"
                    + (filter != null && !filter.isEmpty() ? " " + filter : "")
                    + " for a live async graph. Use /" + label + " usage stop to cancel.");
        }

        return lines;
    }

    private String formatUsageLine(Map.Entry<String, UsageCounts> entry, int maxAsyncLoad) {
        UsageCounts counts = entry.getValue();
        int asyncLoad = counts.getPendingAsync() + counts.getActiveAsyncWorkers();
        String graph = buildAsyncGraph(asyncLoad, maxAsyncLoad);
        ChatColor severity = usageSeverityColor(counts.getTotal());

        StringBuilder line = new StringBuilder();
        line.append(severity).append(entry.getKey()).append(ChatColor.GRAY).append(" ").append(graph)
            .append(ChatColor.DARK_GRAY).append(" | ")
            .append(ChatColor.AQUA).append(asyncLoad).append(ChatColor.GRAY).append(" async (")
            .append(ChatColor.AQUA).append(counts.getPendingAsync()).append(ChatColor.GRAY).append(" pending, ")
            .append(ChatColor.AQUA).append(counts.getActiveAsyncWorkers()).append(ChatColor.GRAY).append(" workers)")
            .append(ChatColor.DARK_GRAY).append(" | ").append(ChatColor.AQUA).append(counts.getPendingSync())
            .append(ChatColor.GRAY).append(" sync pending");

        // Add thread information if available
        if (counts.getThreadCount() > 0) {
            line.append(ChatColor.DARK_GRAY).append(" | ").append(ChatColor.AQUA).append(counts.getThreadCount())
                .append(ChatColor.GRAY).append(" threads");
        }

        // Add CPU time if available (convert to milliseconds)
        if (counts.getCpuTime() > 0) {
            double cpuMs = counts.getCpuTime() / 1_000_000.0;
            line.append(ChatColor.DARK_GRAY).append(" | ").append(ChatColor.AQUA)
                .append(String.format("%.1f", cpuMs)).append(ChatColor.GRAY).append("ms CPU");
        }

        // Add jar size if available
        if (counts.getJarSize() > 0) {
            double sizeKB = counts.getJarSize() / 1024.0;
            line.append(ChatColor.DARK_GRAY).append(" | ").append(ChatColor.AQUA)
                .append(String.format("%.1f", sizeKB)).append(ChatColor.GRAY).append("KB jar");
        }

        return line.toString();
    }

    private String buildAsyncGraph(int value, int maxValue) {
        int clampedMax = Math.max(1, maxValue);
        int filledSegments = (int) Math.round((double) value / clampedMax * ASYNC_GRAPH_WIDTH);
        if (value > 0 && filledSegments == 0) {
            filledSegments = 1;
        }
        if (filledSegments > ASYNC_GRAPH_WIDTH) {
            filledSegments = ASYNC_GRAPH_WIDTH;
        }
        int emptySegments = ASYNC_GRAPH_WIDTH - filledSegments;

        StringBuilder builder = new StringBuilder();
        builder.append(ChatColor.DARK_GRAY).append('[');
        if (filledSegments > 0) {
            builder.append(ChatColor.AQUA);
            for (int i = 0; i < filledSegments; i++) {
                builder.append('█');
            }
        }
        if (emptySegments > 0) {
            builder.append(ChatColor.DARK_GRAY);
            for (int i = 0; i < emptySegments; i++) {
                builder.append('░');
            }
        }
        builder.append(ChatColor.DARK_GRAY).append(']');
        return builder.toString();
    }

    private void startLiveUsageMonitor(Player player, String label, @Nullable String filter) {
        UUID playerId = player.getUniqueId();
        cancelLiveUsage(playerId);

        StringBuilder message = new StringBuilder();
        message.append(ChatColor.GREEN).append("Started live async usage monitor");
        if (filter != null && !filter.isEmpty()) {
            message.append(" for \"").append(filter).append("\"");
        }
        message.append(". Use /").append(label).append(" usage stop to cancel.");
        player.sendMessage(message.toString());
        player.sendMessage(ChatColor.DARK_GRAY + "Live summaries refresh in your action bar. Chat updates"
                + " appear when the load meaningfully changes.");

        LiveUsageSubscription subscription = new LiveUsageSubscription();
        Runnable[] cancelRef = new Runnable[1];
        Runnable cancelTask = FoliaScheduler.runGlobalTimer(plugin, () -> {
            Player current = Bukkit.getPlayer(playerId);
            if (current == null || !current.isOnline()) {
                liveUsageSessions.remove(playerId, subscription);
                Runnable self = cancelRef[0];
                if (self != null) {
                    self.run();
                }
                return;
            }
            UsageSnapshot snapshot = createUsageSnapshot(filter);
            deliverLiveUsageUpdate(current, snapshot, filter, label, subscription);
        }, 0L, LIVE_MONITOR_PERIOD_TICKS);
        cancelRef[0] = cancelTask;
        subscription.setCancelHook(cancelTask);
        liveUsageSessions.put(playerId, subscription);
    }

    private boolean cancelLiveUsage(UUID playerId) {
        LiveUsageSubscription subscription = liveUsageSessions.remove(playerId);
        if (subscription != null) {
            subscription.cancel();
            return true;
        }
        return false;
    }

    private void deliverLiveUsageUpdate(Player player, UsageSnapshot snapshot, @Nullable String filter,
            String label, LiveUsageSubscription subscription) {
        String actionBarTemplate = buildLiveActionBarMessage(snapshot);
        Component actionBarComponent = subscription.actionBarFor(actionBarTemplate,
                () -> MiniMessage.miniMessage().deserialize(actionBarTemplate));
        // Use legacy Spigot API for action bar compatibility
        String legacyActionBar = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(actionBarComponent);
        player.spigot().sendMessage(
            net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(legacyActionBar)
        );

        List<String> latestLines = formatUsageSnapshot(snapshot, true, filter, label, false);
        if (subscription.chatCooldown() > 0) {
            if (!latestLines.equals(subscription.lastChatLines())) {
                subscription.setPendingChatLines(latestLines);
            }
            subscription.decrementChatCooldown();
            return;
        }

        List<String> linesToSend = subscription.hasPendingChatLines()
                ? subscription.consumePendingChatLines()
                : latestLines;
        if (linesToSend != null && !linesToSend.equals(subscription.lastChatLines())) {
            for (String line : linesToSend) {
                player.sendMessage(line);
            }
            subscription.updateLastChatLines(linesToSend);
        }
        subscription.resetChatCooldown(LIVE_CHAT_INTERVAL_CYCLES);
    }

    private String buildLiveActionBarMessage(UsageSnapshot snapshot) {
        if (snapshot.isEmpty()) {
            return "<gray>Async scheduler idle</gray>";
        }

        Map.Entry<String, UsageCounts> peakEntry = snapshot.entries().get(0);
        UsageCounts counts = peakEntry.getValue();
        int asyncLoad = counts.getPendingAsync() + counts.getActiveAsyncWorkers();
        long usedMB = snapshot.usedMemory() / 1024 / 1024;
        long totalMB = snapshot.totalMemory() / 1024 / 1024;

        StringBuilder message = new StringBuilder();
        message.append("<gray>Sync:</gray> <aqua>").append(snapshot.totalPendingSync())
               .append("</aqua> <gray>| Async:</gray> <aqua>").append(snapshot.totalPendingAsync())
               .append("</aqua> <gray>| Workers:</gray> <aqua>").append(snapshot.totalActiveAsyncWorkers())
               .append("</aqua> <gray>| Top:</gray> <aqua>")
               .append(escapeMiniMessage(peakEntry.getKey())).append("</aqua> <gray>(").append(asyncLoad);

        if (counts.getThreadCount() > 0) {
            message.append(", ").append(counts.getThreadCount()).append(" threads");
        }

        message.append(")</gray> <gray>| Memory:</gray> <aqua>").append(usedMB).append("/").append(totalMB).append("</aqua>");

        return message.toString();
    }

    private String escapeMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("<", "\\<")
                .replace(">", "\\>");
    }

    private Map<String, UsageCounts> collectSchedulerUsage() {
        Map<String, UsageCounts> usageByPlugin = new HashMap<>();
        BukkitScheduler scheduler = Bukkit.getScheduler();

        for (BukkitTask task : scheduler.getPendingTasks()) {
            Plugin owner = task.getOwner();
            if (owner == null) {
                continue;
            }

            UsageCounts counts = usageByPlugin.computeIfAbsent(owner.getName(), key -> new UsageCounts());
            if (task.isSync()) {
                counts.incrementPendingSync();
            } else {
                counts.incrementPendingAsync();
            }
        }

        for (BukkitWorker worker : scheduler.getActiveWorkers()) {
            Plugin owner = worker.getOwner();
            if (owner == null) {
                continue;
            }

            UsageCounts counts = usageByPlugin.computeIfAbsent(owner.getName(), key -> new UsageCounts());
            counts.incrementActiveAsyncWorkers();
        }

        // Collect thread and CPU information
        collectThreadUsage(usageByPlugin);

        // Collect plugin jar sizes
        collectPluginSizes(usageByPlugin);

        return usageByPlugin;
    }

    private void collectPluginSizes(Map<String, UsageCounts> usageByPlugin) {
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            try {
                java.io.File jarFile = getPluginJarFile(plugin);
                if (jarFile != null && jarFile.exists()) {
                    long size = jarFile.length();
                    UsageCounts counts = usageByPlugin.computeIfAbsent(plugin.getName(), key -> new UsageCounts());
                    counts.setJarSize(size);
                }
            } catch (Exception e) {
                // Ignore jar size collection errors
            }
        }
    }

    private java.io.File getPluginJarFile(Plugin plugin) {
        try {
            java.lang.reflect.Method getFileMethod = plugin.getClass().getMethod("getFile");
            getFileMethod.setAccessible(true);
            return (java.io.File) getFileMethod.invoke(plugin);
        } catch (Exception e) {
            // Fallback: try to find jar in plugins folder
            java.io.File pluginsDir = new java.io.File("plugins");
            if (pluginsDir.exists() && pluginsDir.isDirectory()) {
                java.io.File[] files = pluginsDir.listFiles((dir, name) -> name.endsWith(".jar"));
                if (files != null) {
                    for (java.io.File file : files) {
                        if (file.getName().toLowerCase().contains(plugin.getName().toLowerCase())) {
                            return file;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void collectThreadUsage(Map<String, UsageCounts> usageByPlugin) {
        try {
            java.lang.management.ThreadMXBean threadMXBean = java.lang.management.ManagementFactory.getThreadMXBean();
            boolean cpuTimeSupported = threadMXBean.isThreadCpuTimeSupported() && threadMXBean.isThreadCpuTimeEnabled();

            Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
            Map<String, Integer> threadCounts = new HashMap<>();
            Map<String, Long> cpuTimes = new HashMap<>();

            for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
                Thread thread = entry.getKey();
                StackTraceElement[] stackTrace = entry.getValue();

                String pluginName = identifyPluginFromStackTrace(stackTrace);
                if (pluginName != null) {
                    threadCounts.merge(pluginName, 1, Integer::sum);

                    if (cpuTimeSupported) {
                        try {
                            long cpuTime = threadMXBean.getThreadCpuTime(thread.getId());
                            if (cpuTime != -1) {
                                cpuTimes.merge(pluginName, cpuTime, Long::sum);
                            }
                        } catch (Exception e) {
                            // Ignore CPU time collection errors
                        }
                    }
                }
            }

            // Update UsageCounts with thread and CPU information
            for (Map.Entry<String, Integer> entry : threadCounts.entrySet()) {
                String pluginName = entry.getKey();
                int count = entry.getValue();
                UsageCounts counts = usageByPlugin.computeIfAbsent(pluginName, key -> new UsageCounts());
                counts.setThreadCount(count);
            }

            for (Map.Entry<String, Long> entry : cpuTimes.entrySet()) {
                String pluginName = entry.getKey();
                long cpuTime = entry.getValue();
                UsageCounts counts = usageByPlugin.computeIfAbsent(pluginName, key -> new UsageCounts());
                counts.setCpuTime(cpuTime);
            }

        } catch (Exception e) {
            // If thread monitoring fails, continue without it
            plugin.getLogger().warning("Failed to collect thread usage: " + e.getMessage());
        }
    }

    private String identifyPluginFromStackTrace(StackTraceElement[] stackTrace) {
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();

            // Check for known plugin packages
            if (className.startsWith("com.skyblockexp.") ||
                className.startsWith("org.bukkit.") ||
                className.startsWith("net.minecraft.")) {
                continue; // Skip server/framework classes
            }

            // Try to match against loaded plugins
            for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                String pluginPackage = plugin.getClass().getPackage().getName();
                if (className.startsWith(pluginPackage)) {
                    return plugin.getName();
                }
            }

            // Fallback: try to extract plugin name from class name patterns
            // Many plugins use their name in package structure
            if (className.contains(".") && !className.startsWith("java.") &&
                !className.startsWith("javax.") && !className.startsWith("sun.") &&
                !className.startsWith("com.sun.") && !className.startsWith("org.apache.")) {

                String[] parts = className.split("\\.");
                if (parts.length >= 2) {
                    String potentialPlugin = parts[1];
                    // Check if this matches a loaded plugin name (case-insensitive)
                    for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                        if (plugin.getName().equalsIgnoreCase(potentialPlugin)) {
                            return plugin.getName();
                        }
                    }
                }
            }
        }
        return null;
    }

    private ChatColor usageSeverityColor(int total) {
        if (total >= 25) {
            return ChatColor.RED;
        }
        if (total >= 10) {
            return ChatColor.GOLD;
        }
        return ChatColor.GREEN;
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.RED + "Usage: /" + label + " usage [plugin|live|stop] [plugin]");
    }
}
