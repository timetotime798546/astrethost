package com.payquick.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

public class AnimationHelper {

    /**
     * Interactive spring bounce feedback for buttons and clickable cards.
     */
    public static void bounceClick(final View view, final Runnable onEndAction) {
        if (view == null) return;

        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 0.92f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 0.92f);
        scaleDownX.setDuration(90);
        scaleDownY.setDuration(90);

        final ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 0.92f, 1.0f);
        final ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.92f, 1.0f);
        scaleUpX.setDuration(140);
        scaleUpY.setDuration(140);
        scaleUpX.setInterpolator(new OvershootInterpolator(3.0f));
        scaleUpY.setInterpolator(new OvershootInterpolator(3.0f));

        scaleUpX.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onEndAction != null) {
                    onEndAction.run();
                }
            }
        });

        AnimatorSet downSet = new AnimatorSet();
        downSet.playTogether(scaleDownX, scaleDownY);
        downSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                AnimatorSet upSet = new AnimatorSet();
                upSet.playTogether(scaleUpX, scaleUpY);
                upSet.start();
            }
        });
        downSet.start();
    }

    /**
     * Gentle pulse animation to highlight a view (such as bonus calculation badge).
     */
    public static void pulse(View view) {
        if (view == null) return;
        ObjectAnimator pulseX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 1.12f, 1.0f);
        ObjectAnimator pulseY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 1.12f, 1.0f);
        pulseX.setDuration(280);
        pulseY.setDuration(280);
        pulseX.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseY.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        set.playTogether(pulseX, pulseY);
        set.start();
    }

    /**
     * Horizontal shake animation for errors and failed inputs.
     */
    public static void shake(View view) {
        if (view == null) return;
        ObjectAnimator shake = ObjectAnimator.ofFloat(view, "translationX",
                0f, 24f, -24f, 18f, -18f, 12f, -12f, 6f, -6f, 0f);
        shake.setDuration(450);
        shake.setInterpolator(new DecelerateInterpolator());
        shake.start();
    }

    /**
     * Staggered slide-in and fade-in entrance for list and card items.
     */
    public static void entranceSlideUp(View view, long delay) {
        if (view == null) return;
        view.setAlpha(0.0f);
        view.setTranslationY(60f);

        view.animate()
                .alpha(1.0f)
                .translationY(0.0f)
                .setDuration(320)
                .setStartDelay(delay)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    /**
     * Pop-in scale and fade entrance animation for cards and galleries.
     */
    public static void popIn(View view, long delay) {
        if (view == null) return;
        view.setAlpha(0.0f);
        view.setScaleX(0.82f);
        view.setScaleY(0.82f);

        view.animate()
                .alpha(1.0f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(350)
                .setStartDelay(delay)
                .setInterpolator(new OvershootInterpolator(1.4f))
                .start();
    }

    /**
     * Smooth cross-fade transition for container view swapping.
     */
    public static void transitionFade(final View view, final Runnable midAction) {
        if (view == null) {
            if (midAction != null) midAction.run();
            return;
        }

        view.animate()
                .alpha(0.0f)
                .translationY(-15f)
                .setDuration(140)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (midAction != null) {
                            midAction.run();
                        }
                        view.setTranslationY(15f);
                        view.animate()
                                .alpha(1.0f)
                                .translationY(0.0f)
                                .setDuration(220)
                                .setInterpolator(new DecelerateInterpolator())
                                .start();
                    }
                }).start();
    }
}