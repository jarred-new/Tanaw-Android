package com.jarredapps.tanaw;

import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

public class FABAnimator {
    private static final long SHOW_DURATION_MS = 220L;
    private static final long HIDE_DURATION_MS = 150L;
    private static final float START_SCALE = 0.82f;
    private static final float MOTION_DISTANCE_DP = 12f;

    private FABAnimator() {
    }

    public static void rotateInFab(View view) {
        view.animate().cancel();
        view.animate()
                .rotation(45f)
                .setDuration(SHOW_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();
    }

    public static void rotateOutFab(View view) {
        view.animate().cancel();
        view.animate()
                .rotation(0f)
                .setDuration(HIDE_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();
    }

    public static void showButtonsIn(View view, long startDelayMillis) {
        view.animate().cancel();
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.setScaleX(START_SCALE);
        view.setScaleY(START_SCALE);
        view.setTranslationY(getMotionDistancePx(view));

        view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setStartDelay(startDelayMillis)
                .setDuration(SHOW_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();
    }

    public static void showButtonsOut(View view, long startDelayMillis) {
        if (view.getVisibility() != View.VISIBLE) {
            return;
        }

        view.animate().cancel();
        view.animate()
                .alpha(0f)
                .scaleX(START_SCALE)
                .scaleY(START_SCALE)
                .translationY(getMotionDistancePx(view))
                .setStartDelay(startDelayMillis)
                .setDuration(HIDE_DURATION_MS)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    view.setVisibility(View.GONE);
                    view.setAlpha(1f);
                    view.setScaleX(1f);
                    view.setScaleY(1f);
                    view.setTranslationY(0f);
                })
                .start();
    }

    private static float getMotionDistancePx(View view) {
        return MOTION_DISTANCE_DP
                * view.getResources().getDisplayMetrics().density;
    }
}
