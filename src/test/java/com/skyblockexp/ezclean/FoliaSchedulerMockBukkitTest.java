package com.skyblockexp.ezclean;

import com.skyblockexp.ezclean.util.FoliaScheduler;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies scheduler behaviour using MockBukkit.
 *
 * <p>MockBukkit provides a full in-process Bukkit server implementation so we
 * can exercise scheduling paths without a live server.
 */
class FoliaSchedulerMockBukkitTest {

    private ServerMock server;
    private JavaPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void isFolia_returnsFalseUnderMockBukkit() {
        assertFalse(FoliaScheduler.isFolia(),
                "FoliaScheduler should report false when running under MockBukkit (Paper/Bukkit)");
    }

    @Test
    void runGlobalLater_returnsCancellable() {
        Runnable cancel = FoliaScheduler.runGlobalLater(plugin, () -> {}, 1L);
        assertNotNull(cancel);
        cancel.run(); // clean up — must not throw
    }

    @Test
    void runGlobalTimer_returnsCancellable() {
        Runnable cancel = FoliaScheduler.runGlobalTimer(plugin, () -> {}, 20L, 20L);
        assertNotNull(cancel);
        cancel.run(); // clean up — must not throw
    }

    @Test
    void runAsync_doesNotThrow() {
        // Simply verify that scheduling an async task does not throw under MockBukkit.
        FoliaScheduler.runAsync(plugin, () -> {});
    }
}
