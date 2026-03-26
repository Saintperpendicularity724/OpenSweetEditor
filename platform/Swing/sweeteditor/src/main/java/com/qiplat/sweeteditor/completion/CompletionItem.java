package com.qiplat.sweeteditor.completion;

import com.qiplat.sweeteditor.core.foundation.TextRange;

/**
 * Completion item data model.
 * <p>Confirmation priority: textEdit → insertText → label.</p>
 */
public class CompletionItem {

    public static class TextEdit {
        public final TextRange range;
        public final String newText;

        public TextEdit(TextRange range, String newText) {
            this.range = range;
            this.newText = newText;
        }

        @Override
        public String toString() {
            return "TextEdit{range=" + range + ", newText='" + newText + "'}";
        }
    }

    public static final int KIND_KEYWORD = 0;
    public static final int KIND_FUNCTION = 1;
    public static final int KIND_VARIABLE = 2;
    public static final int KIND_CLASS = 3;
    public static final int KIND_INTERFACE = 4;
    public static final int KIND_MODULE = 5;
    public static final int KIND_PROPERTY = 6;
    public static final int KIND_SNIPPET = 7;
    public static final int KIND_TEXT = 8;

    /** Plain text format (default). */
    public static final int INSERT_TEXT_FORMAT_PLAIN_TEXT = 1;
    /** VSCode Snippet format (supports $1, ${1:default}, $0, etc. placeholders). */
    public static final int INSERT_TEXT_FORMAT_SNIPPET = 2;

    public String label = "";
    public String detail;
    public String insertText;
    public int insertTextFormat = INSERT_TEXT_FORMAT_PLAIN_TEXT;
    public TextEdit textEdit;
    public String filterText;
    public String sortKey;
    public int kind;

    /**
     * Returns text used for filtering/matching (prefers filterText, falls back to label).
     */
    public String getMatchText() {
        return filterText != null ? filterText : label;
    }

    @Override
    public String toString() {
        return "CompletionItem{label='" + label + "', kind=" + kind + "}";
    }
}
