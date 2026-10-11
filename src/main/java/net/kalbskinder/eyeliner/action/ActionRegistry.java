package net.kalbskinder.eyeliner.action;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps action keys (the value of {@code onClick}) to Java handlers.
 */
public final class ActionRegistry {

    private final Map<String, ClickHandler> handlers = new ConcurrentHashMap<>();

    /**
     * Registers a handler, replacing any existing one with the same key.
     */
    public void register(String key, ClickHandler handler) {
        handlers.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(handler, "handler"));
    }

    public void unregister(String key) {
        handlers.remove(key);
    }

    public @Nullable ClickHandler get(String key) {
        return handlers.get(key);
    }

    public boolean contains(String key) {
        return handlers.containsKey(key);
    }
}
