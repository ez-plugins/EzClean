package com.skyblockexp.ezclean.stats;

import net.kyori.adventure.text.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class LiveUsageSubscription {
    private Runnable cancelHook;
    private List<String> lastChatLines = Collections.emptyList();
    private int chatCooldown;
    private String lastActionBarRaw;
    private Component lastActionBarComponent;
    private List<String> pendingChatLines;

    public void setCancelHook(Runnable cancelHook) {
        this.cancelHook = cancelHook;
    }

    public void cancel() {
        if (cancelHook != null) {
            cancelHook.run();
        }
    }

    public List<String> lastChatLines() {
        return lastChatLines;
    }

    public void updateLastChatLines(List<String> lines) {
        this.lastChatLines = Collections.unmodifiableList(new ArrayList<>(lines));
    }

    public int chatCooldown() {
        return chatCooldown;
    }

    public void resetChatCooldown(int cycles) {
        chatCooldown = Math.max(0, cycles);
    }

    public void decrementChatCooldown() {
        if (chatCooldown > 0) {
            chatCooldown--;
        }
    }

    public void setPendingChatLines(List<String> lines) {
        pendingChatLines = Collections.unmodifiableList(new ArrayList<>(lines));
    }

    public boolean hasPendingChatLines() {
        return pendingChatLines != null;
    }

    public List<String> consumePendingChatLines() {
        List<String> lines = pendingChatLines;
        pendingChatLines = null;
        return lines;
    }

    public Component actionBarFor(String rawMessage, Supplier<Component> factory) {
        if (rawMessage.equals(lastActionBarRaw) && lastActionBarComponent != null) {
            return lastActionBarComponent;
        }
        Component component = factory.get();
        lastActionBarRaw = rawMessage;
        lastActionBarComponent = component;
        return component;
    }
}
