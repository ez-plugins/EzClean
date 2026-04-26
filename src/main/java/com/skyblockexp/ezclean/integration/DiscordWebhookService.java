package com.skyblockexp.ezclean.integration;

import com.skyblockexp.ezclean.config.DiscordWebhookSettings;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sends Discord webhook payloads asynchronously using the JDK {@code java.net.http.HttpClient}.
 *
 * <p>No external HTTP library dependency is needed — the JDK built-in client is used
 * throughout. All POST requests are fire-and-forget: they are dispatched on a dedicated
 * single-thread executor so they never block the main server thread.</p>
 *
 * <p>Instantiate once on plugin enable, call {@link #shutdown()} on disable to drain
 * in-flight requests.</p>
 */
public final class DiscordWebhookService {

    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final DiscordWebhookSettings settings;
    private final Logger logger;
    private final HttpClient httpClient;
    private final ExecutorService executor;

    public DiscordWebhookService(DiscordWebhookSettings settings, Logger logger) {
        this.settings = settings;
        this.logger = logger;
        // Virtual-thread executor eliminates blocking; falls back to platform threads on JDK < 21.
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ezclean-discord-webhook");
            t.setDaemon(true);
            return t;
        });
        this.httpClient = HttpClient.newBuilder()
                .executor(executor)
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
    }

    /**
     * Asynchronously sends a cleanup-summary embed to the configured Discord webhook.
     *
     * @param cleanerId   identifier of the cleanup profile that just ran
     * @param removed     total entities removed (after merging)
     * @param groupCounts per-group breakdown (e.g. "hostile-mobs" → 42)
     * @param durationMs  how long the cleanup cycle took in milliseconds
     * @param tpsBefore   server TPS immediately before the cleanup (may be {@code null})
     * @param tpsAfter    server TPS immediately after the cleanup (may be {@code null})
     */
    public void sendCleanupSummary(
            String cleanerId,
            int removed,
            Map<String, Integer> groupCounts,
            long durationMs,
            Double tpsBefore,
            Double tpsAfter) {
        if (!settings.isSendCleanupSummary()) {
            return;
        }

        String json = buildCleanupSummaryJson(cleanerId, removed, groupCounts, durationMs, tpsBefore, tpsAfter);
        postAsync(json);
    }

    /** Shuts down the executor, waiting up to 5 seconds for in-flight requests to finish. */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // -----------------------------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------------------------

    private void postAsync(String json) {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(settings.getWebhookUrl()))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("User-Agent", "EzClean-Discord-Webhook/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
        } catch (IllegalArgumentException e) {
            logger.warning("[EzClean] Discord webhook URL is invalid; skipping notification. " + e.getMessage());
            return;
        }

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        logger.log(Level.WARNING, "[EzClean] Discord webhook delivery failed.", throwable);
                    } else if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        logger.warning(() -> "[EzClean] Discord webhook returned HTTP " + response.statusCode() + ".");
                    }
                });
    }

    private String buildCleanupSummaryJson(
            String cleanerId,
            int removed,
            Map<String, Integer> groupCounts,
            long durationMs,
            Double tpsBefore,
            Double tpsAfter) {

        // Build the per-group breakdown as embed fields.
        StringBuilder fields = new StringBuilder();
        for (Map.Entry<String, Integer> entry : groupCounts.entrySet()) {
            if (fields.length() > 0) {
                fields.append(",");
            }
            fields.append("{\"name\":").append(jsonString(formatGroupLabel(entry.getKey())))
                  .append(",\"value\":").append(jsonString(Integer.toString(entry.getValue())))
                  .append(",\"inline\":true}");
        }

        // Duration field.
        if (fields.length() > 0) {
            fields.append(",");
        }
        fields.append("{\"name\":\"Duration\",\"value\":").append(jsonString(formatDuration(durationMs)))
              .append(",\"inline\":true}");

        // TPS fields (only if available).
        if (tpsBefore != null || tpsAfter != null) {
            fields.append(",{\"name\":\"TPS Before\",\"value\":").append(
                    jsonString(tpsBefore != null ? String.format(Locale.ROOT, "%.1f", tpsBefore) : "N/A"))
                  .append(",\"inline\":true}");
            fields.append(",{\"name\":\"TPS After\",\"value\":").append(
                    jsonString(tpsAfter != null ? String.format(Locale.ROOT, "%.1f", tpsAfter) : "N/A"))
                  .append(",\"inline\":true}");
        }

        // Assemble the full Discord webhook payload.
        return "{\"embeds\":[{"
                + "\"title\":" + jsonString("EzClean \u2014 Cleanup Complete")
                + ",\"description\":" + jsonString("Cleaner **" + cleanerId + "** removed **" + removed + "** entities.")
                + ",\"color\":" + settings.getEmbedColor()
                + (fields.length() > 0 ? ",\"fields\":[" + fields + "]" : "")
                + "}]}";
    }

    /** Wraps a string in a JSON string literal with minimal escaping. */
    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }

    private static String formatGroupLabel(String groupKey) {
        // Convert hyphen-separated keys like "hostile-mobs" to "Hostile Mobs".
        String[] parts = groupKey.split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    private static String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + "ms";
        }
        return String.format(Locale.ROOT, "%.2fs", millis / 1000.0);
    }
}
