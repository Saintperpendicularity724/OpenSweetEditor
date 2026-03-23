package com.qiplat.sweeteditor.copilot;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Lightweight floating action bar for inline suggestion Accept / Dismiss interaction.
 * Pure UI component — no business logic, no editor dependency.
 */
public class InlineSuggestionActionBar {

    public interface ActionCallback {
        void onAccept();
        void onDismiss();
    }

    private static final int BAR_HEIGHT_DP = 28;
    private static final int CORNER_RADIUS_DP = 6;
    private static final int HORIZONTAL_PADDING_DP = 4;
    private static final int BUTTON_HORIZONTAL_PADDING_DP = 10;
    private static final int DIVIDER_WIDTH_DP = 1;
    private static final int DIVIDER_VERTICAL_MARGIN_DP = 6;
    private static final int FADE_DURATION_MS = 150;
    private static final int GAP_DP = 2;
    private static final int ELEVATION_DP = 4;

    private final Context context;
    private final PopupWindow popupWindow;
    private final View contentView;
    @Nullable private ActionCallback callback;

    private int bgColor;
    private int acceptTextColor;
    private int dismissTextColor;
    private int dividerColor;
    private int rippleColor;

    public InlineSuggestionActionBar(@NonNull Context context,
                                     int bgColor, int acceptTextColor, int dismissTextColor) {
        this.context = context;
        this.bgColor = bgColor;
        this.acceptTextColor = acceptTextColor;
        this.dismissTextColor = dismissTextColor;
        this.dividerColor = deriveOverlayColor(bgColor);
        this.rippleColor = deriveOverlayColor(bgColor);
        this.contentView = buildContentView();
        this.popupWindow = createPopupWindow(contentView);
    }

    private static int deriveOverlayColor(int bgColor) {
        int r = (bgColor >> 16) & 0xFF;
        int g = (bgColor >> 8) & 0xFF;
        int b = bgColor & 0xFF;
        float luminance = 0.299f * r + 0.587f * g + 0.114f * b;
        return luminance > 128 ? 0x22000000 : 0x22FFFFFF;
    }

    public void setCallback(@Nullable ActionCallback callback) {
        this.callback = callback;
    }

    /**
     * Update theme colors and rebuild internal views.
     * If currently showing, the popup is dismissed first.
     */
    public void applyTheme(int bgColor, int acceptTextColor, int dismissTextColor) {
        boolean wasShowing = isShowing();
        if (wasShowing) dismissImmediately();

        this.bgColor = bgColor;
        this.acceptTextColor = acceptTextColor;
        this.dismissTextColor = dismissTextColor;
        this.dividerColor = deriveOverlayColor(bgColor);
        this.rippleColor = deriveOverlayColor(bgColor);

        View newContent = buildContentView();
        popupWindow.setContentView(newContent);
    }

    public boolean isShowing() {
        return popupWindow.isShowing();
    }

    public void showAt(@NonNull View anchor, float cursorX, float cursorY, float cursorHeight) {
        int barHeight = dpToPx(BAR_HEIGHT_DP);
        popupWindow.setHeight(barHeight);
        if (!popupWindow.isShowing()) {
            popupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, 0, 0);
            fadeIn();
        }
        updatePosition(anchor, cursorX, cursorY, cursorHeight);
    }

    public void updatePosition(@NonNull View anchor, float cursorX, float cursorY, float cursorHeight) {
        if (!popupWindow.isShowing()) return;

        int gap = dpToPx(GAP_DP);
        int barHeight = dpToPx(BAR_HEIGHT_DP);

        int[] loc = new int[2];
        anchor.getLocationOnScreen(loc);

        int screenX = loc[0] + (int) cursorX;
        int screenY = loc[1] + (int) cursorY - barHeight - gap;

        int screenW = context.getResources().getDisplayMetrics().widthPixels;

        if (screenY < 0) {
            screenY = loc[1] + (int) (cursorY + cursorHeight + gap);
        }

        int popupW = popupWindow.getWidth();
        if (popupW <= 0) popupW = contentView.getMeasuredWidth();
        if (popupW <= 0) popupW = dpToPx(200);

        if (screenX + popupW > screenW) {
            screenX = screenW - popupW;
        }
        screenX = Math.max(0, screenX);
        screenY = Math.max(0, screenY);

        popupWindow.update(screenX, screenY, -1, barHeight);
    }

    public void dismiss() {
        if (!popupWindow.isShowing()) return;
        fadeOut(() -> {
            if (popupWindow.isShowing()) popupWindow.dismiss();
        });
    }

    public void dismissImmediately() {
        if (popupWindow.isShowing()) popupWindow.dismiss();
    }

    private View buildContentView() {
        LinearLayout bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dpToPx(HORIZONTAL_PADDING_DP), 0, dpToPx(HORIZONTAL_PADDING_DP), 0);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dpToPx(CORNER_RADIUS_DP));
        bar.setBackground(bg);

        TextView acceptBtn = createButton("Accept", acceptTextColor, true);
        acceptBtn.setOnClickListener(v -> {
            if (callback != null) callback.onAccept();
        });

        View divider = createDivider();

        TextView dismissBtn = createButton("Dismiss", dismissTextColor, false);
        dismissBtn.setOnClickListener(v -> {
            if (callback != null) callback.onDismiss();
        });

        bar.addView(acceptBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        bar.addView(divider);
        bar.addView(dismissBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        return bar;
    }

    private TextView createButton(String text, int textColor, boolean bold) {
        TextView btn = new TextView(context);
        btn.setText(text);
        btn.setTextSize(11);
        btn.setTextColor(textColor);
        if (bold) btn.setTypeface(btn.getTypeface(), android.graphics.Typeface.BOLD);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dpToPx(BUTTON_HORIZONTAL_PADDING_DP), 0, dpToPx(BUTTON_HORIZONTAL_PADDING_DP), 0);

        GradientDrawable mask = new GradientDrawable();
        mask.setColor(Color.WHITE);
        mask.setCornerRadius(dpToPx(CORNER_RADIUS_DP));
        btn.setBackground(new RippleDrawable(
                ColorStateList.valueOf(rippleColor),
                null,
                mask));
        btn.setClickable(true);
        return btn;
    }

    private View createDivider() {
        View divider = new View(context);
        divider.setBackgroundColor(dividerColor);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                dpToPx(DIVIDER_WIDTH_DP), ViewGroup.LayoutParams.MATCH_PARENT);
        lp.topMargin = dpToPx(DIVIDER_VERTICAL_MARGIN_DP);
        lp.bottomMargin = dpToPx(DIVIDER_VERTICAL_MARGIN_DP);
        divider.setLayoutParams(lp);
        return divider;
    }

    private PopupWindow createPopupWindow(View content) {
        PopupWindow pw = new PopupWindow(content,
                ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(BAR_HEIGHT_DP));
        pw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        pw.setOutsideTouchable(true);
        pw.setFocusable(false);
        pw.setElevation(dpToPx(ELEVATION_DP));
        return pw;
    }

    private void fadeIn() {
        AlphaAnimation anim = new AlphaAnimation(0f, 1f);
        anim.setDuration(FADE_DURATION_MS);
        contentView.startAnimation(anim);
    }

    private void fadeOut(Runnable onEnd) {
        AlphaAnimation anim = new AlphaAnimation(1f, 0f);
        anim.setDuration(FADE_DURATION_MS);
        anim.setFillAfter(true);
        anim.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
            @Override public void onAnimationStart(android.view.animation.Animation a) {}
            @Override public void onAnimationRepeat(android.view.animation.Animation a) {}
            @Override public void onAnimationEnd(android.view.animation.Animation a) {
                contentView.post(onEnd);
            }
        });
        contentView.startAnimation(anim);
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
