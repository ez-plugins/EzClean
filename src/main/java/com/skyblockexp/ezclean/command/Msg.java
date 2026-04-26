package com.skyblockexp.ezclean.command;

import com.skyblockexp.ezclean.Registry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Centralised message factory for EzClean command feedback.
 * All methods return Adventure {@link Component} objects prefixed with
 * {@code [EzClean]} so every line is consistently branded.
 */
public final class Msg {

    /** Dark-aqua bold "[EzClean] " prefix component. */
    static final Component PREFIX = Component.text()
            .append(Component.text("[", NamedTextColor.DARK_AQUA))
            .append(Component.text("EzClean", NamedTextColor.AQUA, TextDecoration.BOLD))
            .append(Component.text("] ", NamedTextColor.DARK_AQUA))
            .build();

    private Msg() {}

    // ── Translation helper ───────────────────────────────────────────────────

    /**
     * Returns the translated string for the given key from the active language.
     * Falls back to English, then to {@code ""} when not found.
     */
    public static String t(String key) {
        return Registry.getLang().get(key);
    }

    /**
     * Returns the translated string with {@code {placeholder}} substitutions applied.
     *
     * @param key     the language key
     * @param kvPairs alternating placeholder names and values, e.g. {@code "id", cleanerId}
     */
    public static String t(String key, String... kvPairs) {
        return Registry.getLang().get(key, kvPairs);
    }

    // ── Factory helpers ──────────────────────────────────────────────────────

    /** Informational message (gray text). */
    public static Component info(String text) {
        return PREFIX.append(Component.text(text, NamedTextColor.GRAY));
    }

    /** Success message (green text). */
    public static Component success(String text) {
        return PREFIX.append(Component.text(text, NamedTextColor.GREEN));
    }

    /** Error message (red text). */
    public static Component error(String text) {
        return PREFIX.append(Component.text(text, NamedTextColor.RED));
    }

    /** Warning message (yellow text). */
    public static Component warn(String text) {
        return PREFIX.append(Component.text(text, NamedTextColor.YELLOW));
    }

    // ── Duration formatting ──────────────────────────────────────────────────

    /**
     * Formats a {@link Duration} into a human-readable string.
     * Examples: "2 hours and 5 minutes", "30 seconds", "less than a minute".
     */
    public static String formatDuration(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return t("format.duration.instant");
        }

        long totalSeconds = duration.getSeconds();
        if (totalSeconds <= 0L) {
            return t("format.duration.instant");
        }

        long hours   = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        String joiner = t("format.duration.joiner");

        if (hours > 0L) {
            String hUnit = hours == 1L ? t("format.duration.hour-s") : t("format.duration.hour-p");
            if (minutes > 0L) {
                String mUnit = minutes == 1L ? t("format.duration.minute-s") : t("format.duration.minute-p");
                return hours + " " + hUnit + joiner + minutes + " " + mUnit;
            }
            if (seconds > 0L) {
                String sUnit = seconds == 1L ? t("format.duration.second-s") : t("format.duration.second-p");
                return hours + " " + hUnit + joiner + seconds + " " + sUnit;
            }
            return hours + " " + hUnit;
        }
        if (minutes > 0L) {
            String mUnit = minutes == 1L ? t("format.duration.minute-s") : t("format.duration.minute-p");
            if (seconds > 0L) {
                String sUnit = seconds == 1L ? t("format.duration.second-s") : t("format.duration.second-p");
                return minutes + " " + mUnit + joiner + seconds + " " + sUnit;
            }
            return minutes + " " + mUnit;
        }
        String sUnit = seconds == 1L ? t("format.duration.second-s") : t("format.duration.second-p");
        return seconds + " " + sUnit;
    }

    /**
     * Formats a millisecond value into a short duration string.
     * Examples: "123ms", "1.50s".
     */
    public static String formatMs(@Nullable Long durationMillis) {
        if (durationMillis == null) {
            return t("format.not-available");
        }
        if (durationMillis < 1000L) {
            return durationMillis + "ms";
        }
        return String.format(Locale.ROOT, "%.2fs", durationMillis / 1000.0);
    }

    /** Formats a nullable TPS-impact double. */
    public static String formatTpsImpact(@Nullable Double tpsImpact) {
        if (tpsImpact == null) {
            return t("format.not-available");
        }
        return String.format(Locale.ROOT, "%.2f", tpsImpact);
    }

    // ── Entry list formatting ────────────────────────────────────────────────

    /**
     * Formats a map of string→number into a sorted, comma-joined string.
     * Returns "None" when the map is empty.
     */
    public static String formatTopEntries(Map<String, ? extends Number> entries, int limit) {
        if (entries == null || entries.isEmpty()) {
            return t("format.entries.none");
        }
        List<Map.Entry<String, ? extends Number>> sorted = new ArrayList<>(entries.entrySet());
        sorted.sort((a, b) -> {
            int cmp = Double.compare(b.getValue().doubleValue(), a.getValue().doubleValue());
            return cmp != 0 ? cmp : a.getKey().compareToIgnoreCase(b.getKey());
        });
        StringBuilder sb = new StringBuilder();
        int max = Math.min(limit, sorted.size());
        for (int i = 0; i < max; i++) {
            if (i > 0) sb.append(", ");
            Map.Entry<String, ? extends Number> e = sorted.get(i);
            sb.append(e.getKey()).append(" (").append(e.getValue()).append(")");
        }
        if (sorted.size() > limit) {
            sb.append(" ").append(t("format.entries.more", "n", String.valueOf(sorted.size() - limit)));
        }
        return sb.toString();
    }

    /**
     * Builds a list of {@link Component} entries — one per map entry — each with
     * a hover tooltip showing the numeric count and a click-to-suggest command
     * (if {@code suggestCmd} is non-null).
     */
    public static List<Component> formatTopEntriesAsComponents(
            Map<String, ? extends Number> entries, int limit, @Nullable String suggestCmd) {
        List<Component> components = new ArrayList<>();
        if (entries == null || entries.isEmpty()) {
            return components;
        }
        List<Map.Entry<String, ? extends Number>> sorted = new ArrayList<>(entries.entrySet());
        sorted.sort((a, b) -> {
            int cmp = Double.compare(b.getValue().doubleValue(), a.getValue().doubleValue());
            return cmp != 0 ? cmp : a.getKey().compareToIgnoreCase(b.getKey());
        });
        int max = Math.min(limit, sorted.size());
        for (int i = 0; i < max; i++) {
            Map.Entry<String, ? extends Number> e = sorted.get(i);
            Component hover = Component.text(e.getValue() + " " + t("format.entries.removed"), NamedTextColor.GRAY);
            Component entry = Component.text("  • ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(e.getKey(), NamedTextColor.AQUA))
                    .append(Component.text(" ×" + e.getValue(), NamedTextColor.DARK_GRAY))
                    .hoverEvent(HoverEvent.showText(hover));
            if (suggestCmd != null) {
                entry = entry.clickEvent(ClickEvent.suggestCommand(suggestCmd));
            }
            components.add(entry);
        }
        return components;
    }

    // ── Shared separator ─────────────────────────────────────────────────────

    /** A full-width separator bar in dark gray. */
    public static Component separator() {
        return Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY);
    }

    // ── Internal helpers ─────────────────────────────────────────────────────


}
