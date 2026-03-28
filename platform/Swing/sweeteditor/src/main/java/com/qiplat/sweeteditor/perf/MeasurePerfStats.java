package com.qiplat.sweeteditor.perf;

public final class MeasurePerfStats {
    private int textCount;
    private long textNanosTotal;
    private long textNanosMax;
    private int textMaxLen;
    private int textMaxStyle;

    private int inlayCount;
    private long inlayNanosTotal;
    private long inlayNanosMax;
    private int inlayMaxLen;

    private int iconCount;
    private long iconNanosTotal;
    private long iconNanosMax;

    public void reset() {
        textCount = 0;
        textNanosTotal = 0L;
        textNanosMax = 0L;
        textMaxLen = 0;
        textMaxStyle = 0;
        inlayCount = 0;
        inlayNanosTotal = 0L;
        inlayNanosMax = 0L;
        inlayMaxLen = 0;
        iconCount = 0;
        iconNanosTotal = 0L;
        iconNanosMax = 0L;
    }

    public void recordText(long elapsedNanos, int textLen, int fontStyle) {
        textCount++;
        textNanosTotal += elapsedNanos;
        if (elapsedNanos > textNanosMax) {
            textNanosMax = elapsedNanos;
            textMaxLen = textLen;
            textMaxStyle = fontStyle;
        }
    }

    public void recordInlay(long elapsedNanos, int textLen) {
        inlayCount++;
        inlayNanosTotal += elapsedNanos;
        if (elapsedNanos > inlayNanosMax) {
            inlayNanosMax = elapsedNanos;
            inlayMaxLen = textLen;
        }
    }

    public void recordIcon(long elapsedNanos) {
        iconCount++;
        iconNanosTotal += elapsedNanos;
        if (elapsedNanos > iconNanosMax) {
            iconNanosMax = elapsedNanos;
        }
    }

    public String buildSummary() {
        return String.format(
                "measure{text=%d/%.2fms max=%.2fms(len=%d,style=%d) inlay=%d/%.2fms max=%.2fms(len=%d) icon=%d/%.2fms max=%.2fms}",
                textCount,
                textNanosTotal / 1_000_000f,
                textNanosMax / 1_000_000f,
                textMaxLen,
                textMaxStyle,
                inlayCount,
                inlayNanosTotal / 1_000_000f,
                inlayNanosMax / 1_000_000f,
                inlayMaxLen,
                iconCount,
                iconNanosTotal / 1_000_000f,
                iconNanosMax / 1_000_000f);
    }
}
