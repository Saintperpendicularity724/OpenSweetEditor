package com.qiplat.sweeteditor.completion;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.os.Build;
import android.view.KeyEvent;
import android.view.WindowInsets;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.qiplat.sweeteditor.EditorTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * Completion popup controller: PopupWindow + RecyclerView.
 * <p>Cursor-following positioning, up/down key navigation, Enter to confirm, Escape to dismiss.</p>
 */
public class CompletionPopupController implements CompletionProviderManager.CompletionUpdateListener {

    public interface CompletionConfirmListener {
        void onCompletionConfirmed(@NonNull CompletionItem item);
    }

    private static final int MAX_VISIBLE_ITEMS = 6;
    private static final int ITEM_HEIGHT_DP = 32;
    private static final int POPUP_WIDTH_DP = 300;
    private static final int GAP_DP = 4;

    private final Context context;
    private final View anchorView;
    @Nullable private CompletionConfirmListener confirmListener;
    @Nullable private CompletionItemViewFactory viewFactory;

    private PopupWindow popupWindow;
    private RecyclerView recyclerView;
    private CompletionAdapter adapter;
    private final List<CompletionItem> items = new ArrayList<>();
    private int selectedIndex = 0;

    private int panelBgColor;
    private int panelBorderColor;
    private int selectedBgColor;
    private int labelColor;
    private int detailColor;

    private float cachedCursorX = 0;
    private float cachedCursorY = 0;
    private float cachedCursorHeight = 0;

    public CompletionPopupController(@NonNull Context context, @NonNull View anchorView, @NonNull EditorTheme theme) {
        this.context = context;
        this.anchorView = anchorView;
        panelBgColor = theme.completionBgColor;
        panelBorderColor = theme.completionBorderColor;
        selectedBgColor = theme.completionSelectedBgColor;
        labelColor = theme.completionLabelColor;
        detailColor = theme.completionDetailColor;
        initPopup();
    }

    public void applyTheme(@NonNull EditorTheme theme) {
        panelBgColor = theme.completionBgColor;
        panelBorderColor = theme.completionBorderColor;
        selectedBgColor = theme.completionSelectedBgColor;
        labelColor = theme.completionLabelColor;
        detailColor = theme.completionDetailColor;
        if (recyclerView != null) {
            GradientDrawable panelBg = new GradientDrawable();
            panelBg.setColor(panelBgColor);
            panelBg.setCornerRadius(dpToPx(context, 12));
            panelBg.setStroke(dpToPx(context, 1), panelBorderColor);
            recyclerView.setBackground(panelBg);
            recyclerView.setClipToOutline(true);
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    public void setConfirmListener(@Nullable CompletionConfirmListener listener) {
        this.confirmListener = listener;
    }

    public void setViewFactory(@Nullable CompletionItemViewFactory factory) {
        this.viewFactory = factory;
        if (adapter != null) adapter.setViewFactory(factory);
    }

    public boolean isShowing() {
        return popupWindow != null && popupWindow.isShowing();
    }

    @Override
    public void onCompletionItemsUpdated(@NonNull List<CompletionItem> newItems) {
        items.clear();
        items.addAll(newItems);
        selectedIndex = 0;
        adapter.notifyDataSetChanged();
        if (items.isEmpty()) {
            dismiss();
        } else {
            show();
        }
    }

    @Override
    public void onCompletionDismissed() {
        dismiss();
    }

    /**
     * Handle Android KeyEvent keyCode for completion panel navigation.
     */
    public boolean handleAndroidKeyCode(int androidKeyCode) {
        if (!isShowing() || items.isEmpty()) return false;
        switch (androidKeyCode) {
            case KeyEvent.KEYCODE_ENTER:
                confirmSelected();
                return true;
            case KeyEvent.KEYCODE_ESCAPE:
                dismiss();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                moveSelection(-1);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                moveSelection(1);
                return true;
            default:
                return false;
        }
    }

    /**
     * Update cached cursor screen coordinates (called by SweetEditor every frame in onDraw).
     * If panel is showing, also refresh panel position.
     */
    public void updateCursorPosition(float cursorScreenX, float cursorScreenY, float cursorHeight) {
        cachedCursorX = cursorScreenX;
        cachedCursorY = cursorScreenY;
        cachedCursorHeight = cursorHeight;
        if (isShowing()) {
            applyPosition();
        }
    }

    /**
     * Calculate and apply panel position based on cached cursor coordinates.
     * cachedCursorX/Y are coordinates within anchorView, need to be converted to screen coordinates for PopupWindow positioning.
     */
    private void applyPosition() {
        int gap = dpToPx(context, GAP_DP);
        int popupHeight = popupWindow.getHeight();
        if (popupHeight <= 0) {
            popupHeight = dpToPx(context, ITEM_HEIGHT_DP * Math.min(items.size(), MAX_VISIBLE_ITEMS));
        }

        int[] anchorLocation = new int[2];
        anchorView.getLocationOnScreen(anchorLocation);

        int cursorLeft = anchorLocation[0] + (int) cachedCursorX;
        int cursorTop = anchorLocation[1] + (int) cachedCursorY;
        int cursorBottom = anchorLocation[1] + (int) (cachedCursorY + cachedCursorHeight);

        Rect visibleFrame = new Rect();
        anchorView.getWindowVisibleDisplayFrame(visibleFrame);
        if (visibleFrame.width() <= 0 || visibleFrame.height() <= 0) {
            int screenHeight = anchorView.getResources().getDisplayMetrics().heightPixels;
            int screenWidth = anchorView.getResources().getDisplayMetrics().widthPixels;
            visibleFrame.set(0, 0, screenWidth, screenHeight);
        }

        int imeBottom = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsets rootInsets = anchorView.getRootWindowInsets();
            if (rootInsets != null) {
                imeBottom = rootInsets.getInsets(WindowInsets.Type.ime()).bottom;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            WindowInsets rootInsets = anchorView.getRootWindowInsets();
            if (rootInsets != null) {
                int systemBottom = rootInsets.getSystemWindowInsetBottom();
                int stableBottom = rootInsets.getStableInsetBottom();
                imeBottom = Math.max(0, systemBottom - stableBottom);
            }
        }

        if (imeBottom > 0) {
            View rootView = anchorView.getRootView();
            int rootHeight = rootView != null ? rootView.getHeight() : 0;
            if (rootView != null && rootHeight > 0) {
                int[] rootLocation = new int[2];
                rootView.getLocationOnScreen(rootLocation);
                int imeTop = rootLocation[1] + rootHeight - imeBottom;
                if (imeTop > visibleFrame.top && imeTop < visibleFrame.bottom) {
                    visibleFrame.bottom = imeTop;
                }
            } else if (imeBottom < visibleFrame.height()) {
                visibleFrame.bottom = visibleFrame.bottom - imeBottom;
            }
        }

        if (imeBottom <= 0) {
            // Fallback for edge-to-edge windows where IME insets may temporarily report 0.
            View rootView = anchorView.getRootView();
            if (rootView != null) {
                Rect rootVisible = new Rect();
                rootView.getWindowVisibleDisplayFrame(rootVisible);
                int keyboardHeight = rootView.getHeight() - rootVisible.height();
                int keyboardThreshold = dpToPx(context, 80);
                if (keyboardHeight > keyboardThreshold) {
                    int[] rootLocation = new int[2];
                    rootView.getLocationOnScreen(rootLocation);
                    int imeTop = rootLocation[1] + rootVisible.bottom;
                    if (imeTop > visibleFrame.top && imeTop < visibleFrame.bottom) {
                        visibleFrame.bottom = imeTop;
                    }
                }
            }
        }

        if (visibleFrame.height() < dpToPx(context, ITEM_HEIGHT_DP)) {
            return;
        }

        int desiredPopupWidth = dpToPx(context, POPUP_WIDTH_DP);
        int minPopupWidth = dpToPx(context, 120);
        int availableWidth = Math.max(1, visibleFrame.width() - dpToPx(context, 8));
        int popupWidth = Math.min(desiredPopupWidth, Math.max(minPopupWidth, availableWidth));
        if (popupWidth > visibleFrame.width()) {
            popupWidth = visibleFrame.width();
        }

        int maxPopupHeight = Math.max(dpToPx(context, ITEM_HEIGHT_DP), visibleFrame.height() - dpToPx(context, 8));
        if (popupHeight > maxPopupHeight) {
            popupHeight = maxPopupHeight;
        }

        int maxX = visibleFrame.right - popupWidth;
        int screenX = Math.max(visibleFrame.left, Math.min(cursorLeft, maxX));

        int belowY = cursorBottom + gap;
        int aboveY = cursorTop - popupHeight - gap;

        boolean canShowBelow = belowY + popupHeight <= visibleFrame.bottom;
        boolean canShowAbove = aboveY >= visibleFrame.top;

        int screenY;
        if (canShowBelow) {
            screenY = belowY;
        } else if (canShowAbove) {
            screenY = aboveY;
        } else {
            int clampedBelow = Math.max(visibleFrame.top,
                    Math.min(belowY, visibleFrame.bottom - popupHeight));
            int clampedAbove = Math.max(visibleFrame.top,
                    Math.min(aboveY, visibleFrame.bottom - popupHeight));
            int spaceBelow = visibleFrame.bottom - cursorBottom - gap;
            int spaceAbove = cursorTop - gap - visibleFrame.top;
            screenY = spaceAbove > spaceBelow ? clampedAbove : clampedBelow;
        }

        screenY = Math.max(visibleFrame.top, screenY);
        if (screenY + popupHeight > visibleFrame.bottom) {
            screenY = Math.max(visibleFrame.top, visibleFrame.bottom - popupHeight);
        }

        popupWindow.update(screenX, screenY, popupWidth, popupHeight);
    }
    public void dismiss() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }

    private void initPopup() {
        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        GradientDrawable panelBg = new GradientDrawable();
        panelBg.setColor(panelBgColor);
        panelBg.setCornerRadius(dpToPx(context, 12));
        panelBg.setStroke(dpToPx(context, 1), panelBorderColor);
        recyclerView.setBackground(panelBg);
        recyclerView.setClipToOutline(true);
        recyclerView.setPadding(dpToPx(context, 4), dpToPx(context, 6), dpToPx(context, 4), dpToPx(context, 6));
        recyclerView.setClipToPadding(false);

        adapter = new CompletionAdapter();
        recyclerView.setAdapter(adapter);

        int width = dpToPx(context, POPUP_WIDTH_DP);
        popupWindow = new PopupWindow(recyclerView, width, ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(false);
        popupWindow.setElevation(dpToPx(context, 8));
    }

    private void show() {
        int maxHeight = dpToPx(context, ITEM_HEIGHT_DP * Math.min(items.size(), MAX_VISIBLE_ITEMS));
        popupWindow.setHeight(maxHeight);
        if (!popupWindow.isShowing()) {
            popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, 0, 0);
        }
        // Position near cursor immediately using cached cursor coordinates
        applyPosition();
    }

    private void moveSelection(int delta) {
        if (items.isEmpty()) return;
        int old = selectedIndex;
        selectedIndex = Math.max(0, Math.min(items.size() - 1, selectedIndex + delta));
        if (old != selectedIndex) {
            adapter.notifyItemChanged(old);
            adapter.notifyItemChanged(selectedIndex);
            recyclerView.scrollToPosition(selectedIndex);
        }
    }

    private void confirmSelected() {
        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            CompletionItem item = items.get(selectedIndex);
            dismiss();
            if (confirmListener != null) {
                confirmListener.onCompletionConfirmed(item);
            }
        }
    }

    private static int dpToPx(@NonNull Context ctx, int dp) {
        return (int) (dp * ctx.getResources().getDisplayMetrics().density + 0.5f);
    }

    private class CompletionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Nullable private CompletionItemViewFactory factory;

        void setViewFactory(@Nullable CompletionItemViewFactory factory) {
            this.factory = factory;
        }

        @NonNull @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (factory != null) {
                View view = factory.createItemView(parent);
                return new RecyclerView.ViewHolder(view) {};
            }
            return new DefaultViewHolder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            CompletionItem item = items.get(position);
            boolean isSelected = position == selectedIndex;
            if (factory != null) {
                factory.bindItemView(holder.itemView, item, isSelected);
            } else {
                ((DefaultViewHolder) holder).bind(item, isSelected, selectedBgColor, labelColor, detailColor);
            }
            holder.itemView.setOnClickListener(v -> {
                selectedIndex = position;
                confirmSelected();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private static class DefaultViewHolder extends RecyclerView.ViewHolder {
        private final TextView kindBadge;
        private final ImageView iconView;
        private final TextView labelView;
        private final TextView detailView;
        private final GradientDrawable rowBg;
        private final GradientDrawable badgeBg;

        DefaultViewHolder(@NonNull ViewGroup parent) {
            super(createDefaultItemView(parent.getContext()));
            kindBadge = itemView.findViewWithTag("kindBadge");
            iconView = itemView.findViewWithTag("icon");
            labelView = itemView.findViewWithTag("label");
            detailView = itemView.findViewWithTag("detail");

            rowBg = new GradientDrawable();
            rowBg.setCornerRadius(dpToPx(parent.getContext(), 6));
            itemView.setBackground(rowBg);

            badgeBg = new GradientDrawable();
            badgeBg.setCornerRadius(dpToPx(parent.getContext(), 4));
            kindBadge.setBackground(badgeBg);
        }

        void bind(@NonNull CompletionItem item, boolean isSelected, int selectedColor, int lblColor, int dtlColor) {
            labelView.setText(item.label);
            labelView.setTextColor(lblColor);
            if (item.detail != null && !item.detail.isEmpty()) {
                detailView.setVisibility(View.VISIBLE);
                detailView.setText(item.detail);
                detailView.setTextColor(dtlColor);
            } else {
                detailView.setVisibility(View.GONE);
            }

            rowBg.setColor(isSelected ? selectedColor : Color.TRANSPARENT);

            iconView.setVisibility(View.GONE);
            kindBadge.setVisibility(View.VISIBLE);
            applyKindBadge(kindBadge, item.kind);
        }

        private static void applyKindBadge(TextView badge, int kind) {
            int color;
            String letter;
            switch (kind) {
                case CompletionItem.KIND_KEYWORD:
                    color = 0xFFC678DD; letter = "K"; break;
                case CompletionItem.KIND_FUNCTION:
                    color = 0xFF61AFEF; letter = "F"; break;
                case CompletionItem.KIND_VARIABLE:
                    color = 0xFFE5C07B; letter = "V"; break;
                case CompletionItem.KIND_CLASS:
                    color = 0xFFE06C75; letter = "C"; break;
                case CompletionItem.KIND_INTERFACE:
                    color = 0xFF56B6C2; letter = "I"; break;
                case CompletionItem.KIND_MODULE:
                    color = 0xFFD19A66; letter = "M"; break;
                case CompletionItem.KIND_PROPERTY:
                    color = 0xFF98C379; letter = "P"; break;
                case CompletionItem.KIND_SNIPPET:
                    color = 0xFFBE5046; letter = "S"; break;
                default:
                    color = 0xFF7A8494; letter = "T"; break;
            }
            badge.setText(letter);
            GradientDrawable bg = (GradientDrawable) badge.getBackground();
            bg.setColor(color);
        }

        private static View createDefaultItemView(@NonNull Context context) {
            float density = context.getResources().getDisplayMetrics().density;
            int hPadding = (int) (8 * density);
            int vPadding = (int) (2 * density);
            int height = (int) (ITEM_HEIGHT_DP * density);

            android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
            layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            layout.setGravity(Gravity.CENTER_VERTICAL);
            layout.setPadding(hPadding, vPadding, hPadding, vPadding);
            layout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));

            TextView kindBadge = new TextView(context);
            int badgeSize = (int) (18 * density);
            android.widget.LinearLayout.LayoutParams badgeLp =
                    new android.widget.LinearLayout.LayoutParams(badgeSize, badgeSize);
            badgeLp.setMarginEnd((int) (8 * density));
            kindBadge.setLayoutParams(badgeLp);
            kindBadge.setGravity(Gravity.CENTER);
            kindBadge.setTextSize(10);
            kindBadge.setTextColor(0xFFFFFFFF);
            kindBadge.setTypeface(Typeface.DEFAULT_BOLD);
            kindBadge.setTag("kindBadge");
            layout.addView(kindBadge);

            ImageView icon = new ImageView(context);
            int iconSize = (int) (16 * density);
            android.widget.LinearLayout.LayoutParams iconLp =
                    new android.widget.LinearLayout.LayoutParams(iconSize, iconSize);
            iconLp.setMarginEnd((int) (8 * density));
            icon.setLayoutParams(iconLp);
            icon.setTag("icon");
            icon.setVisibility(View.GONE);
            layout.addView(icon);

            TextView label = new TextView(context);
            label.setTextSize(13);
            label.setTextColor(0xFFD8DEE9);
            label.setSingleLine(true);
            label.setEllipsize(TextUtils.TruncateAt.END);
            label.setTag("label");
            android.widget.LinearLayout.LayoutParams labelLp =
                    new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            label.setLayoutParams(labelLp);
            layout.addView(label);

            TextView detail = new TextView(context);
            detail.setTextSize(11);
            detail.setTextColor(0xFF7A8494);
            detail.setSingleLine(true);
            detail.setEllipsize(TextUtils.TruncateAt.END);
            detail.setTag("detail");
            detail.setVisibility(View.GONE);
            android.widget.LinearLayout.LayoutParams detailLp =
                    new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            detailLp.setMarginStart((int) (8 * density));
            detail.setLayoutParams(detailLp);
            layout.addView(detail);

            return layout;
        }
    }
}
