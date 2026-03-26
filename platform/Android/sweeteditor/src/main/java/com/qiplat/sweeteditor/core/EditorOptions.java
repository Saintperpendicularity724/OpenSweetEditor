package com.qiplat.sweeteditor.core;

/**
 * Construction-time immutable options for EditorCore.
 * <p>
 * Fields mirror the C++ {@code EditorOptions} struct.
 * Use {@link ProtocolEncoder#packEditorOptions(EditorOptions)} to encode into the binary payload
 * expected by the native C API.
 * <p>
 * Binary layout (LE byte order):
 * <pre>
 *   f32  touch_slop
 *   i64  double_tap_timeout
 *   i64  long_press_ms
 *   f32  fling_friction
 *   f32  fling_min_velocity
 *   f32  fling_max_velocity
 *   u64  max_undo_stack_size
 * </pre>
 */
public class EditorOptions {
    /** Threshold to determine if a gesture is a move; below this threshold, it's considered a tap */
    public final float touchSlop;
    /** Double-tap time threshold (milliseconds) */
    public final long doubleTapTimeout;
    /** Long press time threshold (milliseconds) */
    public final long longPressMs;
    /** Fling friction coefficient (higher = faster deceleration) */
    public final float flingFriction;
    /** Minimum fling velocity threshold in pixels/second */
    public final float flingMinVelocity;
    /** Maximum fling velocity cap in pixels/second */
    public final float flingMaxVelocity;
    /** Max undo stack size (0 = unlimited) */
    public final long maxUndoStackSize;

    public EditorOptions() {
this(10f, 300, 500, 2.0f, 30f, 12000f, 512);
    }

    public EditorOptions(float touchSlop, long doubleTapTimeout) {
this(touchSlop, doubleTapTimeout, 500, 2.0f, 30f, 12000f, 512);
    }

    public EditorOptions(float touchSlop, long doubleTapTimeout, long longPressMs, long maxUndoStackSize) {
this(touchSlop, doubleTapTimeout, longPressMs, 2.0f, 30f, 12000f, maxUndoStackSize);
    }

    public EditorOptions(float touchSlop, long doubleTapTimeout, long longPressMs,
                         float flingFriction, float flingMinVelocity, float flingMaxVelocity,
                         long maxUndoStackSize) {
        this.touchSlop = touchSlop;
        this.doubleTapTimeout = doubleTapTimeout;
        this.longPressMs = longPressMs;
        this.flingFriction = flingFriction;
        this.flingMinVelocity = flingMinVelocity;
        this.flingMaxVelocity = flingMaxVelocity;
        this.maxUndoStackSize = maxUndoStackSize;
    }
}
