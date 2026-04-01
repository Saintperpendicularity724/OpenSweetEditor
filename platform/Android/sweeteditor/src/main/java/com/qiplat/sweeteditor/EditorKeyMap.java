package com.qiplat.sweeteditor;

import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qiplat.sweeteditor.core.keymap.EditorCommand;
import com.qiplat.sweeteditor.core.keymap.KeyBinding;
import com.qiplat.sweeteditor.core.keymap.KeyCode;
import com.qiplat.sweeteditor.core.keymap.KeyMap;
import com.qiplat.sweeteditor.core.keymap.KeyModifier;

/**
 * Widget-layer extension of {@link KeyMap} that additionally holds
 * command handlers bound to {@link SweetEditor}.
 */
public class EditorKeyMap extends KeyMap {

    private final SparseArray<EditorCommand<SweetEditor>> mCommands = new SparseArray<>();
    private int mNextCustomId = EditorCommand.BUILT_IN_MAX + 1;

    /**
     * Register a command with an explicit command id (typically for built-in platform commands).
     */
    public void registerCommand(int commandId, @NonNull KeyBinding binding,
                                @NonNull EditorCommand<SweetEditor> handler) {
        mCommands.put(commandId, handler);
        addBinding(binding, commandId);
    }

    /**
     * Register a custom command with an auto-assigned id.
     *
     * @return the assigned command id
     */
    public int registerCommand(@NonNull KeyBinding binding,
                               @NonNull EditorCommand<SweetEditor> handler) {
        int id = mNextCustomId++;
        mCommands.put(id, handler);
        addBinding(binding, id);
        return id;
    }

    @Nullable
    public EditorCommand<SweetEditor> getCommand(int commandId) {
        return mCommands.get(commandId);
    }

    private static void bind(EditorKeyMap km, int modifiers, int keyCode, int command) {
        km.addBinding(new KeyBinding(modifiers, keyCode, command), command);
    }

    private static void addCommonBindings(EditorKeyMap km) {
        bind(km, KeyModifier.NONE, KeyCode.LEFT,      EditorCommand.CURSOR_LEFT);
        bind(km, KeyModifier.NONE, KeyCode.RIGHT,     EditorCommand.CURSOR_RIGHT);
        bind(km, KeyModifier.NONE, KeyCode.UP,        EditorCommand.CURSOR_UP);
        bind(km, KeyModifier.NONE, KeyCode.DOWN,      EditorCommand.CURSOR_DOWN);
        bind(km, KeyModifier.NONE, KeyCode.HOME,      EditorCommand.CURSOR_LINE_START);
        bind(km, KeyModifier.NONE, KeyCode.END,       EditorCommand.CURSOR_LINE_END);
        bind(km, KeyModifier.NONE, KeyCode.PAGE_UP,   EditorCommand.CURSOR_PAGE_UP);
        bind(km, KeyModifier.NONE, KeyCode.PAGE_DOWN, EditorCommand.CURSOR_PAGE_DOWN);

        bind(km, KeyModifier.SHIFT, KeyCode.LEFT,      EditorCommand.SELECT_LEFT);
        bind(km, KeyModifier.SHIFT, KeyCode.RIGHT,     EditorCommand.SELECT_RIGHT);
        bind(km, KeyModifier.SHIFT, KeyCode.UP,        EditorCommand.SELECT_UP);
        bind(km, KeyModifier.SHIFT, KeyCode.DOWN,      EditorCommand.SELECT_DOWN);
        bind(km, KeyModifier.SHIFT, KeyCode.HOME,      EditorCommand.SELECT_LINE_START);
        bind(km, KeyModifier.SHIFT, KeyCode.END,       EditorCommand.SELECT_LINE_END);
        bind(km, KeyModifier.SHIFT, KeyCode.PAGE_UP,   EditorCommand.SELECT_PAGE_UP);
        bind(km, KeyModifier.SHIFT, KeyCode.PAGE_DOWN, EditorCommand.SELECT_PAGE_DOWN);

        bind(km, KeyModifier.NONE, KeyCode.BACKSPACE,  EditorCommand.BACKSPACE);
        bind(km, KeyModifier.NONE, KeyCode.DELETE_KEY,  EditorCommand.DELETE_FORWARD);
        bind(km, KeyModifier.NONE, KeyCode.TAB,        EditorCommand.INSERT_TAB);
        bind(km, KeyModifier.NONE, KeyCode.ENTER,      EditorCommand.INSERT_NEWLINE);

        bind(km, KeyModifier.CTRL, KeyCode.A, EditorCommand.SELECT_ALL);
        bind(km, KeyModifier.META, KeyCode.A, EditorCommand.SELECT_ALL);

        bind(km, KeyModifier.CTRL, KeyCode.Z, EditorCommand.UNDO);
        bind(km, KeyModifier.META, KeyCode.Z, EditorCommand.UNDO);

        km.registerCommand(EditorCommand.COPY,
                new KeyBinding(KeyModifier.CTRL, KeyCode.C, EditorCommand.COPY),
                (binding, editor) -> editor.copyToClipboard());
        km.registerCommand(EditorCommand.COPY,
                new KeyBinding(KeyModifier.META, KeyCode.C, EditorCommand.COPY),
                (binding, editor) -> editor.copyToClipboard());
        km.registerCommand(EditorCommand.PASTE,
                new KeyBinding(KeyModifier.CTRL, KeyCode.V, EditorCommand.PASTE),
                (binding, editor) -> editor.pasteFromClipboard());
        km.registerCommand(EditorCommand.PASTE,
                new KeyBinding(KeyModifier.META, KeyCode.V, EditorCommand.PASTE),
                (binding, editor) -> editor.pasteFromClipboard());
        km.registerCommand(EditorCommand.CUT,
                new KeyBinding(KeyModifier.CTRL, KeyCode.X, EditorCommand.CUT),
                (binding, editor) -> editor.cutToClipboard());
        km.registerCommand(EditorCommand.CUT,
                new KeyBinding(KeyModifier.META, KeyCode.X, EditorCommand.CUT),
                (binding, editor) -> editor.cutToClipboard());

        km.registerCommand(EditorCommand.TRIGGER_COMPLETION,
                new KeyBinding(KeyModifier.CTRL, KeyCode.SPACE, EditorCommand.TRIGGER_COMPLETION),
                (binding, editor) -> editor.triggerCompletion());
        km.registerCommand(EditorCommand.TRIGGER_COMPLETION,
                new KeyBinding(KeyModifier.META, KeyCode.SPACE, EditorCommand.TRIGGER_COMPLETION),
                (binding, editor) -> editor.triggerCompletion());
    }

    /**
     * Create the default key map (VS Code style).
     */
    public static EditorKeyMap defaultKeyMap() {
        return vscode();
    }

    /**
     * VS Code key bindings.
     */
    public static EditorKeyMap vscode() {
        EditorKeyMap km = new EditorKeyMap();
        addCommonBindings(km);

        bind(km, KeyModifier.CTRL | KeyModifier.SHIFT, KeyCode.Z, EditorCommand.REDO);
        bind(km, KeyModifier.META | KeyModifier.SHIFT, KeyCode.Z, EditorCommand.REDO);
        bind(km, KeyModifier.CTRL, KeyCode.Y, EditorCommand.REDO);
        bind(km, KeyModifier.META, KeyCode.Y, EditorCommand.REDO);

        bind(km, KeyModifier.CTRL, KeyCode.ENTER, EditorCommand.INSERT_LINE_BELOW);
        bind(km, KeyModifier.META, KeyCode.ENTER, EditorCommand.INSERT_LINE_BELOW);
        bind(km, KeyModifier.CTRL | KeyModifier.SHIFT, KeyCode.ENTER, EditorCommand.INSERT_LINE_ABOVE);
        bind(km, KeyModifier.META | KeyModifier.SHIFT, KeyCode.ENTER, EditorCommand.INSERT_LINE_ABOVE);

        bind(km, KeyModifier.ALT, KeyCode.UP,   EditorCommand.MOVE_LINE_UP);
        bind(km, KeyModifier.ALT, KeyCode.DOWN, EditorCommand.MOVE_LINE_DOWN);
        bind(km, KeyModifier.ALT | KeyModifier.SHIFT, KeyCode.UP,   EditorCommand.COPY_LINE_UP);
        bind(km, KeyModifier.ALT | KeyModifier.SHIFT, KeyCode.DOWN, EditorCommand.COPY_LINE_DOWN);

        bind(km, KeyModifier.CTRL | KeyModifier.SHIFT, KeyCode.K, EditorCommand.DELETE_LINE);
        bind(km, KeyModifier.META | KeyModifier.SHIFT, KeyCode.K, EditorCommand.DELETE_LINE);

        return km;
    }

    /**
     * JetBrains (IntelliJ IDEA) key bindings.
     */
    public static EditorKeyMap jetbrains() {
        EditorKeyMap km = new EditorKeyMap();
        addCommonBindings(km);

        bind(km, KeyModifier.CTRL | KeyModifier.SHIFT, KeyCode.Z, EditorCommand.REDO);
        bind(km, KeyModifier.META | KeyModifier.SHIFT, KeyCode.Z, EditorCommand.REDO);

        bind(km, KeyModifier.CTRL, KeyCode.Y, EditorCommand.DELETE_LINE);
        bind(km, KeyModifier.META, KeyCode.Y, EditorCommand.DELETE_LINE);

        bind(km, KeyModifier.CTRL, KeyCode.D, EditorCommand.COPY_LINE_DOWN);
        bind(km, KeyModifier.META, KeyCode.D, EditorCommand.COPY_LINE_DOWN);

        bind(km, KeyModifier.SHIFT, KeyCode.ENTER, EditorCommand.INSERT_LINE_BELOW);
        bind(km, KeyModifier.CTRL | KeyModifier.ALT, KeyCode.ENTER, EditorCommand.INSERT_LINE_ABOVE);
        bind(km, KeyModifier.META | KeyModifier.ALT, KeyCode.ENTER, EditorCommand.INSERT_LINE_ABOVE);

        bind(km, KeyModifier.ALT | KeyModifier.SHIFT, KeyCode.UP,   EditorCommand.MOVE_LINE_UP);
        bind(km, KeyModifier.ALT | KeyModifier.SHIFT, KeyCode.DOWN, EditorCommand.MOVE_LINE_DOWN);

        return km;
    }

    /**
     * Sublime Text key bindings.
     */
    public static EditorKeyMap sublime() {
        EditorKeyMap km = new EditorKeyMap();
        addCommonBindings(km);

        bind(km, KeyModifier.CTRL | KeyModifier.SHIFT, KeyCode.Z, EditorCommand.REDO);
        bind(km, KeyModifier.META | KeyModifier.SHIFT, KeyCode.Z, EditorCommand.REDO);
        bind(km, KeyModifier.CTRL, KeyCode.Y, EditorCommand.REDO);
        bind(km, KeyModifier.META, KeyCode.Y, EditorCommand.REDO);

        bind(km, KeyModifier.CTRL, KeyCode.ENTER, EditorCommand.INSERT_LINE_BELOW);
        bind(km, KeyModifier.META, KeyCode.ENTER, EditorCommand.INSERT_LINE_BELOW);
        bind(km, KeyModifier.CTRL | KeyModifier.SHIFT, KeyCode.ENTER, EditorCommand.INSERT_LINE_ABOVE);
        bind(km, KeyModifier.META | KeyModifier.SHIFT, KeyCode.ENTER, EditorCommand.INSERT_LINE_ABOVE);

        bind(km, KeyModifier.CTRL | KeyModifier.SHIFT, KeyCode.UP,   EditorCommand.MOVE_LINE_UP);
        bind(km, KeyModifier.CTRL | KeyModifier.SHIFT, KeyCode.DOWN, EditorCommand.MOVE_LINE_DOWN);

        bind(km, KeyModifier.CTRL | KeyModifier.SHIFT, KeyCode.K, EditorCommand.DELETE_LINE);
        bind(km, KeyModifier.META | KeyModifier.SHIFT, KeyCode.K, EditorCommand.DELETE_LINE);

        return km;
    }
}
