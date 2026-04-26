package com.skyblockexp.ezclean.update;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.bukkit.plugin.java.JavaPlugin;
import com.skyblockexp.ezclean.util.FoliaScheduler;
import org.jetbrains.annotations.Nullable;

public final class SpigotUpdateChecker {
    private static final String UPDATE_ENDPOINT = "https://api.spigotmc.org/legacy/update.php?resource=";
    private final JavaPlugin plugin;
    private final int resourceId;

    /** null = not yet checked; empty string = up to date; non-empty = available version. */
    private volatile @Nullable String availableVersion = null;

    public SpigotUpdateChecker(JavaPlugin plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    /**
     * Returns the latest version string if an update is available, or {@code null}
     * if no update is available or the check has not completed yet.
     */
    public @Nullable String getAvailableVersion() {
        return availableVersion;
    }

    /** Returns the SpigotMC resource URL for building clickable update notices. */
    public String getResourceUrl() {
        return "https://www.spigotmc.org/resources/" + resourceId + "/";
    }

    public void checkForUpdates() {
        FoliaScheduler.runAsync(plugin, () -> {
            try {
                String latestVersion = fetchLatestVersion();
                if (latestVersion == null || latestVersion.isBlank()) {
                    plugin.getLogger().warning("SpigotMC update check returned an empty response.");
                    return;
                }
                String currentVersion = plugin.getDescription().getVersion();
                if (!latestVersion.equalsIgnoreCase(currentVersion)) {
                    availableVersion = latestVersion;
                    plugin.getLogger().info("A new EzClean version is available: " + latestVersion
                            + " (current: " + currentVersion + "). Download: " + getResourceUrl());
                } else {
                    availableVersion = "";
                    plugin.getLogger().info("EzClean is up to date.");
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to check for EzClean updates: " + ex.getMessage());
            }
        });
    }

    private String fetchLatestVersion() throws Exception {
        URL url = new URL(UPDATE_ENDPOINT + resourceId);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent", "EzClean Update Checker");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.readLine();
        } finally {
            connection.disconnect();
        }
    }
}

