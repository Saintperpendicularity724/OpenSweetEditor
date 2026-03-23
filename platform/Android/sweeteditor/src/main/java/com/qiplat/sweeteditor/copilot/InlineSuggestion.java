package com.qiplat.sweeteditor.copilot;

import androidx.annotation.NonNull;

/**
 * Immutable value object describing a single inline suggestion (phantom text + position).
 */
public final class InlineSuggestion {
    /** Target line (0-based) */
    public final int line;
    /** Insertion column (0-based, UTF-16 offset) */
    public final int column;
    /** Suggestion text content */
    @NonNull
    public final String text;

    public InlineSuggestion(int line, int column, @NonNull String text) {
        this.line = line;
        this.column = column;
        this.text = text;
    }
}
