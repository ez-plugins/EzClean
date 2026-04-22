package com.skyblockexp.ezclean.config;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable spawn-reason filter for a single cleanup profile or per-world override.
 *
 * <p>Two filtering modes are supported:
 * <ul>
 *   <li><b>restrict</b> — entities with these spawn reasons are shielded from all
 *       category-based removal rules (hostile-mobs, dropped-items, etc.).</li>
 *   <li><b>force-remove</b> — entities with these spawn reasons bypass standard protect
 *       rules (tamed, name-tagged, armor-stands, display-entities) and are always
 *       removed when encountered during a cleanup pass.</li>
 * </ul>
 *
 * <p>WorldGuard region bypasses and explicit {@code entity-types.keep} entries still
 * take precedence over both filter modes.
 */
public final class SpawnReasonFilter {

    private static final SpawnReasonFilter EMPTY =
            new SpawnReasonFilter(Collections.emptySet(), Collections.emptySet());

    private final Set<SpawnReason> restrictedReasons;
    private final Set<SpawnReason> forceRemoveReasons;

    private SpawnReasonFilter(Set<SpawnReason> restrictedReasons, Set<SpawnReason> forceRemoveReasons) {
        this.restrictedReasons = restrictedReasons;
        this.forceRemoveReasons = forceRemoveReasons;
    }

    /** Returns a filter that applies no spawn-reason rules. */
    public static SpawnReasonFilter empty() {
        return EMPTY;
    }

    static SpawnReasonFilter of(Set<SpawnReason> restricted, Set<SpawnReason> forceRemove) {
        if (restricted.isEmpty() && forceRemove.isEmpty()) {
            return EMPTY;
        }
        return new SpawnReasonFilter(
                restricted.isEmpty() ? Collections.emptySet()
                        : Collections.unmodifiableSet(EnumSet.copyOf(restricted)),
                forceRemove.isEmpty() ? Collections.emptySet()
                        : Collections.unmodifiableSet(EnumSet.copyOf(forceRemove)));
    }

    /**
     * Returns {@code true} if entities with this spawn reason should never be removed by
     * category-based rules.
     */
    public boolean isRestricted(SpawnReason reason) {
        return restrictedReasons.contains(reason);
    }

    /**
     * Returns {@code true} if entities with this spawn reason bypass standard protect
     * rules (tamed, name-tagged, armor-stands, display-entities) and must always be
     * removed when encountered.
     */
    public boolean isForceRemove(SpawnReason reason) {
        return forceRemoveReasons.contains(reason);
    }

    /** Returns {@code true} if this filter has no active rules. */
    public boolean isEmpty() {
        return restrictedReasons.isEmpty() && forceRemoveReasons.isEmpty();
    }

    public Set<SpawnReason> getRestrictedReasons() {
        return restrictedReasons;
    }

    public Set<SpawnReason> getForceRemoveReasons() {
        return forceRemoveReasons;
    }

    /**
     * Parses a {@code spawn-reasons} configuration section.
     *
     * @param section the {@code spawn-reasons} config section, or {@code null}
     * @param logger  plugin logger used to warn about unknown reason names
     * @param path    config path prefix used in warning messages
     * @return parsed filter, or {@link #empty()} when the section is absent or blank
     */
    static SpawnReasonFilter parse(@Nullable ConfigurationSection section, @Nullable Logger logger, String path) {
        if (section == null) {
            return EMPTY;
        }
        Set<SpawnReason> restricted = parseReasons(
                section.getStringList("restrict"), logger, path + ".restrict");
        Set<SpawnReason> forceRemove = parseReasons(
                section.getStringList("force-remove"), logger, path + ".force-remove");
        return of(restricted, forceRemove);
    }

    private static Set<SpawnReason> parseReasons(List<String> entries, @Nullable Logger logger, String path) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptySet();
        }
        Set<SpawnReason> result = new HashSet<>();
        for (String raw : entries) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                result.add(SpawnReason.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                if (logger != null) {
                    logger.warning("Unknown spawn reason at '" + path + "': " + raw);
                }
            }
        }
        return result;
    }
}
