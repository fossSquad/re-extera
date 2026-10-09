package com.exteragram.messenger.utils.system;

import android.os.VibrationEffect;
import android.view.View;

/**
 * Runtime shim for exteraGram's vibration helpers.
 *
 * re:extera code only calls {@link #getType(int)} before
 * {@link View#performHapticFeedback(int, int)}; the rest exists to keep the surface
 * compatible with the original class.
 */
public abstract class VibratorUtils {

    public static void disableHapticFeedback(View view) {
    }

    public static int getType(int type) {
        return type;
    }

    public static void vibrate() {
    }

    public static void vibrate(long duration) {
    }

    public static void vibrateEffect(VibrationEffect effect) {
    }
}
