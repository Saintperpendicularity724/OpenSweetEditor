package com.qiplat.sweeteditor.core.keymap;

/**
 * Modifier key flag constants matching the C++ KeyModifier enum.
 */
public final class KeyModifier {
    private KeyModifier() {}

    public static final int NONE  = 0;
    public static final int SHIFT = 1;
    public static final int CTRL  = 2;
    public static final int ALT   = 4;
    public static final int META  = 8;
}
