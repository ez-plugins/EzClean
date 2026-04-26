package com.skyblockexp.ezclean.gui;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-player session state for the EzClean setup GUI.
 * Tracks which screen the player is on, which cleaner is being edited,
 * a YAML snapshot of the disk state, and any unsaved pending changes.
 */
final class SetupSession {

    enum Screen {
        LIST,
        EDITOR
    }

    private Screen screen = Screen.LIST;
    private String editingCleaner;
    private YamlConfiguration snapshot;
    private final Map<String, Object> pendingChanges = new HashMap<>();

    // ── Screen ───────────────────────────────────────────────────────────

    Screen getScreen() {
        return screen;
    }

    void setScreen(Screen screen) {
        this.screen = screen;
    }

    // ── Editing cleaner ID ───────────────────────────────────────────────

    String getEditingCleaner() {
        return editingCleaner;
    }

    void setEditingCleaner(String id) {
        this.editingCleaner = id;
    }

    // ── Snapshot + pending changes ───────────────────────────────────────

    /**
     * Loads a fresh YAML snapshot from disk and clears any pending changes.
     * Call this when opening the editor for a cleaner.
     */
    void loadSnapshot(YamlConfiguration cfg) {
        this.snapshot = cfg;
        this.pendingChanges.clear();
    }

    /**
     * Returns a boolean value: pending change takes precedence over snapshot.
     */
    boolean getBoolean(String path, boolean def) {
        if (pendingChanges.containsKey(path)) {
            Object v = pendingChanges.get(path);
            return v instanceof Boolean ? (Boolean) v : def;
        }
        return snapshot != null ? snapshot.getBoolean(path, def) : def;
    }

    /**
     * Returns an integer value: pending change takes precedence over snapshot.
     */
    int getInt(String path, int def) {
        if (pendingChanges.containsKey(path)) {
            Object v = pendingChanges.get(path);
            if (v instanceof Integer) return (Integer) v;
            if (v instanceof Number) return ((Number) v).intValue();
        }
        return snapshot != null ? snapshot.getInt(path, def) : def;
    }

    void set(String path, Object value) {
        pendingChanges.put(path, value);
    }

    Map<String, Object> getPendingChanges() {
        return pendingChanges;
    }

    boolean hasPendingChanges() {
        return !pendingChanges.isEmpty();
    }

    /** Resets all session state. */
    void clear() {
        screen = Screen.LIST;
        editingCleaner = null;
        snapshot = null;
        pendingChanges.clear();
    }
}
