package com.skyblockexp.ezclean.integration;

import com.skyblockexp.ezclean.config.CleanupSettings;
import com.skyblockexp.ezclean.config.EzCountdownSettings;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.time.Duration;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.Nullable;

/**
 * Optional integration with EzCountdown that mirrors each EzClean cleanup countdown
 * to EzCountdown's display system (action bar, boss bar, title, scoreboard, etc.).
 *
 * <p>EzCountdown must be installed alongside EzClean for this integration to activate.
 * It is soft-dependent: EzClean loads and operates normally when EzCountdown is absent.</p>
 *
 * <p>Each enabled cleaner profile creates a single DURATION-type countdown in EzCountdown
 * named according to the {@code integrations.ezcountdown.countdown-name} config key.
 * When a cleanup cycle completes the countdown is restarted with the full interval duration
 * so players always see the accurate time remaining until the next cleanup.</p>
 *
 * <p>This class is fully reflection-based and carries no compile-time dependency on
 * EzCountdown. All API access is performed via cached {@link MethodHandle}s resolved
 * once during {@link #create}.</p>
 */
public final class EzCountdownIntegration {

    // -----------------------------------------------------------------------------------------
    // EzCountdown class / method names — must match EzCountdown's published API
    // -----------------------------------------------------------------------------------------

    private static final String API_CLASS          = "com.skyblockexp.ezcountdown.api.EzCountdownApi";
    private static final String COUNTDOWN_CLASS    = "com.skyblockexp.ezcountdown.api.model.Countdown";
    private static final String BUILDER_CLASS      = "com.skyblockexp.ezcountdown.api.model.CountdownBuilder";
    private static final String COUNTDOWN_TYPE_CLS = "com.skyblockexp.ezcountdown.api.model.CountdownType";
    private static final String DISPLAY_TYPE_CLS   = "com.skyblockexp.ezcountdown.display.DisplayType";

    // -----------------------------------------------------------------------------------------
    // Internal state
    // -----------------------------------------------------------------------------------------

    private final Object api;
    private final Logger logger;
    private final Set<String> managedCountdownNames = new HashSet<>();

    // EzCountdownApi method handles
    private final MethodHandle mhGetCountdown;
    private final MethodHandle mhCreateCountdown;
    private final MethodHandle mhStartCountdown;
    private final MethodHandle mhStopCountdown;
    private final MethodHandle mhDeleteCountdown;

    // Countdown instance method handles
    private final MethodHandle mhCountdownIsRunning;
    private final MethodHandle mhCountdownSetDuration;

    // CountdownBuilder method handles
    private final MethodHandle mhBuilderFactory;
    private final MethodHandle mhBuilderType;
    private final MethodHandle mhBuilderDisplayTypes;
    private final MethodHandle mhBuilderUpdateInterval;
    private final MethodHandle mhBuilderFormatMessage;
    private final MethodHandle mhBuilderStartMessage;
    private final MethodHandle mhBuilderEndMessage;
    private final @Nullable MethodHandle mhBuilderVisibility;
    private final MethodHandle mhBuilderZoneId;
    private final MethodHandle mhBuilderDuration;
    private final MethodHandle mhBuilderBuild;

    // Pre-resolved enum constants
    private final Object countdownTypeDuration;
    @SuppressWarnings("rawtypes")
    private final Class<? extends Enum> displayTypeClass;
    @SuppressWarnings("rawtypes")
    private final Map<String, Enum> displayTypeValues;

    // -----------------------------------------------------------------------------------------
    // Constructor (private; use create())
    // -----------------------------------------------------------------------------------------

    @SuppressWarnings("rawtypes")
    private EzCountdownIntegration(
            Object api,
            MethodHandle mhGetCountdown,
            MethodHandle mhCreateCountdown,
            MethodHandle mhStartCountdown,
            MethodHandle mhStopCountdown,
            MethodHandle mhDeleteCountdown,
            MethodHandle mhCountdownIsRunning,
            MethodHandle mhCountdownSetDuration,
            MethodHandle mhBuilderFactory,
            MethodHandle mhBuilderType,
            MethodHandle mhBuilderDisplayTypes,
            MethodHandle mhBuilderUpdateInterval,
            MethodHandle mhBuilderFormatMessage,
            MethodHandle mhBuilderStartMessage,
            MethodHandle mhBuilderEndMessage,
            @Nullable MethodHandle mhBuilderVisibility,
            MethodHandle mhBuilderZoneId,
            MethodHandle mhBuilderDuration,
            MethodHandle mhBuilderBuild,
            Object countdownTypeDuration,
            Class<? extends Enum> displayTypeClass,
            Map<String, Enum> displayTypeValues,
            Logger logger) {
        this.api = api;
        this.mhGetCountdown = mhGetCountdown;
        this.mhCreateCountdown = mhCreateCountdown;
        this.mhStartCountdown = mhStartCountdown;
        this.mhStopCountdown = mhStopCountdown;
        this.mhDeleteCountdown = mhDeleteCountdown;
        this.mhCountdownIsRunning = mhCountdownIsRunning;
        this.mhCountdownSetDuration = mhCountdownSetDuration;
        this.mhBuilderFactory = mhBuilderFactory;
        this.mhBuilderType = mhBuilderType;
        this.mhBuilderDisplayTypes = mhBuilderDisplayTypes;
        this.mhBuilderUpdateInterval = mhBuilderUpdateInterval;
        this.mhBuilderFormatMessage = mhBuilderFormatMessage;
        this.mhBuilderStartMessage = mhBuilderStartMessage;
        this.mhBuilderEndMessage = mhBuilderEndMessage;
        this.mhBuilderVisibility = mhBuilderVisibility;
        this.mhBuilderZoneId = mhBuilderZoneId;
        this.mhBuilderDuration = mhBuilderDuration;
        this.mhBuilderBuild = mhBuilderBuild;
        this.countdownTypeDuration = countdownTypeDuration;
        this.displayTypeClass = displayTypeClass;
        this.displayTypeValues = Map.copyOf(displayTypeValues);
        this.logger = logger;
    }

    // -----------------------------------------------------------------------------------------
    // Factory
    // -----------------------------------------------------------------------------------------

    /**
     * Attempts to initialise the EzCountdown integration by resolving the
     * {@code EzCountdownApi} service and caching all required API method handles.
     *
     * @param logger the plugin logger for integration messages
     * @return a configured integration instance, or {@code null} when EzCountdown is
     *         unavailable or its API is incompatible with the expected interface
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static @Nullable EzCountdownIntegration create(Logger logger) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();

            Class<?> apiClass     = Class.forName(API_CLASS);
            Class<?> cdClass      = Class.forName(COUNTDOWN_CLASS);
            Class<?> bldrClass    = Class.forName(BUILDER_CLASS);
            Class<?> cdTypeClass  = Class.forName(COUNTDOWN_TYPE_CLS);
            Class<?> dispClass    = Class.forName(DISPLAY_TYPE_CLS);

            RegisteredServiceProvider<?> rsp =
                    Bukkit.getServicesManager().getRegistration(apiClass);
            if (rsp == null) {
                return null;
            }
            Object api = rsp.getProvider();
            if (api == null) {
                return null;
            }

            // EzCountdownApi method handles
            MethodHandle mhGet    = lookup.findVirtual(apiClass, "getCountdown",
                    MethodType.methodType(Optional.class, String.class));
            MethodHandle mhCreate = lookup.findVirtual(apiClass, "createCountdown",
                    MethodType.methodType(boolean.class, cdClass));
            MethodHandle mhStart  = lookup.findVirtual(apiClass, "startCountdown",
                    MethodType.methodType(boolean.class, String.class));
            MethodHandle mhStop   = lookup.findVirtual(apiClass, "stopCountdown",
                    MethodType.methodType(boolean.class, String.class));
            MethodHandle mhDelete = lookup.findVirtual(apiClass, "deleteCountdown",
                    MethodType.methodType(boolean.class, String.class));

            // Countdown instance method handles
            MethodHandle mhIsRunning    = lookup.findVirtual(cdClass, "isRunning",
                    MethodType.methodType(boolean.class));
            MethodHandle mhSetDuration  = lookup.findVirtual(cdClass, "setDurationSeconds",
                    MethodType.methodType(void.class, long.class));

            // CountdownBuilder method handles
            MethodHandle mhFactory     = lookup.findStatic(bldrClass, "builder",
                    MethodType.methodType(bldrClass, String.class));
            MethodHandle mhType        = lookup.findVirtual(bldrClass, "type",
                    MethodType.methodType(bldrClass, cdTypeClass));
            MethodHandle mhDispTypes   = lookup.findVirtual(bldrClass, "displayTypes",
                    MethodType.methodType(bldrClass, EnumSet.class));
            MethodHandle mhInterval    = lookup.findVirtual(bldrClass, "updateIntervalSeconds",
                    MethodType.methodType(bldrClass, int.class));
            MethodHandle mhFormat      = lookup.findVirtual(bldrClass, "formatMessage",
                    MethodType.methodType(bldrClass, String.class));
            MethodHandle mhStartMsg    = lookup.findVirtual(bldrClass, "startMessage",
                    MethodType.methodType(bldrClass, String.class));
            MethodHandle mhEndMsg      = lookup.findVirtual(bldrClass, "endMessage",
                    MethodType.methodType(bldrClass, String.class));
            MethodHandle mhZone        = lookup.findVirtual(bldrClass, "zoneId",
                    MethodType.methodType(bldrClass, ZoneId.class));
            MethodHandle mhDur         = lookup.findVirtual(bldrClass, "duration",
                    MethodType.methodType(bldrClass, Duration.class));
            MethodHandle mhBuild       = lookup.findVirtual(bldrClass, "build",
                    MethodType.methodType(cdClass));

            // visibilityPermission is optional — some versions of EzCountdown may omit it
            MethodHandle mhVisibility = null;
            try {
                mhVisibility = lookup.findVirtual(bldrClass, "visibilityPermission",
                        MethodType.methodType(bldrClass, String.class));
            } catch (NoSuchMethodException ignored) {
                // Not present in this version of EzCountdown — skip silently
            }

            // Enum constants
            Object typeDuration = Enum.valueOf((Class<? extends Enum>) cdTypeClass, "DURATION");
            Map<String, Enum> dispValues = new HashMap<>();
            for (Object c : dispClass.getEnumConstants()) {
                dispValues.put(((Enum<?>) c).name(), (Enum<?>) c);
            }

            return new EzCountdownIntegration(api,
                    mhGet, mhCreate, mhStart, mhStop, mhDelete,
                    mhIsRunning, mhSetDuration,
                    mhFactory, mhType, mhDispTypes, mhInterval,
                    mhFormat, mhStartMsg, mhEndMsg, mhVisibility,
                    mhZone, mhDur, mhBuild,
                    typeDuration, (Class<? extends Enum>) dispClass, dispValues, logger);

        } catch (ClassNotFoundException ignored) {
            // EzCountdown is not installed — integration silently unavailable
            return null;
        } catch (Throwable ex) {
            logger.log(Level.WARNING,
                    "Failed to initialise the EzCountdown integration; the API may have changed. "
                            + "Continuing without EzCountdown support.", ex);
            return null;
        }
    }

    // -----------------------------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------------------------

    /**
     * Registers and starts an EzCountdown DURATION countdown that mirrors the EzClean cleanup
     * timer for the given cleaner profile.
     *
     * <p>If a countdown with the resolved name already exists its duration is updated and it
     * is restarted. If no countdown exists yet a new one is created using the display options
     * from {@code ezSettings}.</p>
     *
     * <p>This method is a no-op when {@link EzCountdownSettings#isEnabled()}
     * returns {@code false}.</p>
     *
     * @param cleanerId       identifier of the EzClean cleaner profile
     * @param ezSettings      EzCountdown settings read from the cleaner's configuration section
     * @param durationSeconds full countdown duration in seconds (typically the cleanup interval)
     */
    public void syncCountdown(String cleanerId,
                              EzCountdownSettings ezSettings,
                              long durationSeconds) {
        if (!ezSettings.isEnabled()) {
            return;
        }
        String name = ezSettings.resolveCountdownName(cleanerId);
        try {
            Optional<?> existing = (Optional<?>) mhGetCountdown.invoke(api, name);
            if (existing != null && existing.isPresent()) {
                Object countdown = existing.get();
                boolean running = (boolean) mhCountdownIsRunning.invoke(countdown);
                if (running) {
                    mhStopCountdown.invoke(api, name);
                }
                mhCountdownSetDuration.invoke(countdown, durationSeconds);
            } else {
                Object countdown = buildCountdown(name, ezSettings, durationSeconds);
                mhCreateCountdown.invoke(api, countdown);
                managedCountdownNames.add(name);
            }
            mhStartCountdown.invoke(api, name);
        } catch (Throwable ex) {
            logger.log(Level.WARNING,
                    "Failed to sync EzCountdown countdown '" + name + "' for cleaner '"
                            + cleanerId + "'.", ex);
        }
    }

    /**
     * Stops the EzCountdown countdown for the specified cleaner without removing it.
     *
     * <p>This method is a no-op when {@link EzCountdownSettings#isEnabled()}
     * returns {@code false}.</p>
     *
     * @param cleanerId  identifier of the EzClean cleaner profile
     * @param ezSettings EzCountdown settings read from the cleaner's configuration section
     */
    public void stopCountdown(String cleanerId, EzCountdownSettings ezSettings) {
        if (!ezSettings.isEnabled()) {
            return;
        }
        String name = ezSettings.resolveCountdownName(cleanerId);
        try {
            mhStopCountdown.invoke(api, name);
        } catch (Throwable ex) {
            logger.log(Level.WARNING,
                    "Failed to stop EzCountdown countdown '" + name + "' for cleaner '"
                            + cleanerId + "'.", ex);
        }
    }

    /**
     * Stops and deletes all EzCountdown countdowns that were created by this integration.
     * Called during plugin shutdown to avoid leaving stale entries in EzCountdown's storage.
     */
    public void shutdown() {
        for (String name : managedCountdownNames) {
            try {
                mhStopCountdown.invoke(api, name);
                mhDeleteCountdown.invoke(api, name);
            } catch (Throwable ex) {
                logger.log(Level.WARNING,
                        "Failed to remove EzCountdown countdown '" + name + "' during shutdown.", ex);
            }
        }
        managedCountdownNames.clear();
    }

    // -----------------------------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------------------------

    private Object buildCountdown(String name,
                                  EzCountdownSettings ezSettings,
                                  long durationSeconds) throws Throwable {
        Object builder = mhBuilderFactory.invoke(name);
        builder = mhBuilderType.invoke(builder, countdownTypeDuration);
        builder = mhBuilderDisplayTypes.invoke(builder, buildDisplayEnumSet(ezSettings.getDisplayTypes()));
        builder = mhBuilderUpdateInterval.invoke(builder, ezSettings.getUpdateIntervalSeconds());
        builder = mhBuilderFormatMessage.invoke(builder, ezSettings.getFormatMessage());
        builder = mhBuilderStartMessage.invoke(builder, nvl(ezSettings.getStartMessage()));
        builder = mhBuilderEndMessage.invoke(builder, nvl(ezSettings.getEndMessage()));
        if (mhBuilderVisibility != null) {
            builder = mhBuilderVisibility.invoke(builder, nvl(ezSettings.getVisibilityPermission()));
        }
        builder = mhBuilderZoneId.invoke(builder, ZoneId.systemDefault());
        builder = mhBuilderDuration.invoke(builder, Duration.ofSeconds(durationSeconds));
        return mhBuilderBuild.invoke(builder);
    }

    /**
     * Builds an {@link EnumSet} of EzCountdown {@code DisplayType} values from the
     * configured list of display type strings. Unknown strings are silently skipped;
     * defaults to {@code ACTION_BAR} if no valid entries remain.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private EnumSet<?> buildDisplayEnumSet(List<String> rawTypes) {
        EnumSet result = EnumSet.noneOf(displayTypeClass);
        for (String raw : rawTypes) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            Enum<?> constant = displayTypeValues.get(raw.trim().toUpperCase(Locale.ROOT));
            if (constant != null) {
                result.add(constant);
            }
        }
        if (result.isEmpty()) {
            Enum<?> actionBar = displayTypeValues.get("ACTION_BAR");
            if (actionBar != null) {
                result.add(actionBar);
            }
        }
        return result;
    }

    private static String nvl(@Nullable String value) {
        return value != null ? value : "";
    }
}
