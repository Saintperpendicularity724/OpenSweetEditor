package com.qiplat.sweeteditor.core.keymap;

/**
 * A key binding entry: one or two chords mapped to a command.
 * Matches the C++ KeyBinding struct.
 */
public class KeyBinding {
    public final KeyChord first;
    public final KeyChord second;
    public final int command;

    public KeyBinding(KeyChord first, int command) {
        this(first, KeyChord.EMPTY, command);
    }

    public KeyBinding(KeyChord first, KeyChord second, int command) {
        this.first = first;
        this.second = second;
        this.command = command;
    }

    public KeyBinding(int modifiers, int keyCode, int command) {
        this(new KeyChord(modifiers, keyCode), KeyChord.EMPTY, command);
    }

    public KeyBinding(int firstModifiers, int firstKeyCode,
                      int secondModifiers, int secondKeyCode, int command) {
        this(new KeyChord(firstModifiers, firstKeyCode),
             new KeyChord(secondModifiers, secondKeyCode), command);
    }
}
