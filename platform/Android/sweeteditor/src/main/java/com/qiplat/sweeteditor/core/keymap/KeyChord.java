package com.qiplat.sweeteditor.core.keymap;

import java.util.Objects;

/**
 * A single key chord: one key press with optional modifiers.
 * Matches the C++ KeyChord struct.
 */
public class KeyChord {
    public static final KeyChord EMPTY = new KeyChord(KeyModifier.NONE, KeyCode.NONE);

    public final int modifiers;
    public final int keyCode;

    public KeyChord(int modifiers, int keyCode) {
        this.modifiers = modifiers;
        this.keyCode = keyCode;
    }

    public boolean empty() {
        return keyCode == KeyCode.NONE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KeyChord)) return false;
        KeyChord other = (KeyChord) o;
        return keyCode == other.keyCode && modifiers == other.modifiers;
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyCode, modifiers);
    }
}
