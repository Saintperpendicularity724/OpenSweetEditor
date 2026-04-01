package com.qiplat.sweeteditor.core.keymap;

/**
 * Generic command interface for editor key bindings.
 * Built-in command id constants are kept as static fields for compatibility with C++ enum values.
 *
 * @param <T> the widget type that receives the shortcut callback
 */
@FunctionalInterface
public interface EditorCommand<T> {

    void onShortCut(KeyBinding binding, T widget);

    int NONE = 0;
    int CURSOR_LEFT = 1;
    int CURSOR_RIGHT = 2;
    int CURSOR_UP = 3;
    int CURSOR_DOWN = 4;
    int CURSOR_LINE_START = 5;
    int CURSOR_LINE_END = 6;
    int CURSOR_PAGE_UP = 7;
    int CURSOR_PAGE_DOWN = 8;
    int SELECT_LEFT = 9;
    int SELECT_RIGHT = 10;
    int SELECT_UP = 11;
    int SELECT_DOWN = 12;
    int SELECT_LINE_START = 13;
    int SELECT_LINE_END = 14;
    int SELECT_PAGE_UP = 15;
    int SELECT_PAGE_DOWN = 16;
    int SELECT_ALL = 17;
    int BACKSPACE = 18;
    int DELETE_FORWARD = 19;
    int INSERT_TAB = 20;
    int INSERT_NEWLINE = 21;
    int INSERT_LINE_ABOVE = 22;
    int INSERT_LINE_BELOW = 23;
    int UNDO = 24;
    int REDO = 25;
    int MOVE_LINE_UP = 26;
    int MOVE_LINE_DOWN = 27;
    int COPY_LINE_UP = 28;
    int COPY_LINE_DOWN = 29;
    int DELETE_LINE = 30;
    int COPY = 31;
    int PASTE = 32;
    int CUT = 33;
    int TRIGGER_COMPLETION = 34;

    int BUILT_IN_MAX = TRIGGER_COMPLETION;
}
