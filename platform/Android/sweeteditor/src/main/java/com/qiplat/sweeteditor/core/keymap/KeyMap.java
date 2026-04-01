package com.qiplat.sweeteditor.core.keymap;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Pure data container for keyboard shortcut bindings.
 * Maps {@link KeyBinding} to command id (int). Platform-specific command handlers
 * are managed by subclasses (e.g. EditorKeyMap in the widget layer).
 */
public class KeyMap {

    private final Map<KeyBinding, Integer> mBindings = new HashMap<>();

    public void addBinding(@NonNull KeyBinding binding, int commandId) {
        mBindings.put(binding, commandId);
    }

    public void removeBinding(@NonNull KeyBinding binding) {
        mBindings.remove(binding);
    }

    @NonNull
    public Map<KeyBinding, Integer> getBindings() {
        return mBindings;
    }
}
