package com.qiplat.sweeteditor.copilot;

import androidx.annotation.NonNull;

/**
 * Callback for inline suggestion accept/dismiss events.
 */
public interface InlineSuggestionListener {
    /** Called when user accepted the suggestion (Tab key or Accept button). */
    void onSuggestionAccepted(@NonNull InlineSuggestion suggestion);

    /** Called when user dismissed the suggestion (Esc key or Dismiss button). */
    void onSuggestionDismissed(@NonNull InlineSuggestion suggestion);
}
