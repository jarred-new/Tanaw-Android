package com.jarredapps.tanaw;

import android.content.Context;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation;

public class FABAnimator {

    public static void rotateInFab(Context context, final View view) {
        Animation rotateIn = AnimationUtils.loadAnimation(context, R.anim.rotate_fab_in);

        view.startAnimation(rotateIn);
    }

    public static void rotateOutFab(Context context, final View view) {
        Animation rotateOut = AnimationUtils.loadAnimation(context, R.anim.rotate_fab_out);

        view.startAnimation(rotateOut);
    }

    public static void showButtonsIn(Context context, final View view) {
        Animation rotateIn = AnimationUtils.loadAnimation(context, R.anim.from_bottom);

        rotateIn.setAnimationListener(
                new Animation.AnimationListener() {

                    @Override
                    public void onAnimationEnd(Animation arg0) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation arg0) {
                    }

                    @Override
                    public void onAnimationStart(Animation arg0) {
                        view.setVisibility(View.VISIBLE);
                    }
                });

        view.startAnimation(rotateIn);
    }

    public static void showButtonsOut(Context context, final View view) {
        Animation rotateOut = AnimationUtils.loadAnimation(context, R.anim.from_top);

        rotateOut.setAnimationListener(
                new Animation.AnimationListener() {

                    @Override
                    public void onAnimationEnd(Animation arg0) {
                        view.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation arg0) {
                    }

                    @Override
                    public void onAnimationStart(Animation arg0) {
                    }
                });

        view.startAnimation(rotateOut);
    }
}
