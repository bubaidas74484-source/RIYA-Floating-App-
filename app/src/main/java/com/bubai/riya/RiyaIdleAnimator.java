package com.bubai.riya;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.widget.ImageView;

public class RiyaIdleAnimator {
    private final ImageView avatar;
    private AnimatorSet animator;
    private boolean running = false;

    public RiyaIdleAnimator(ImageView avatar) {
        this.avatar = avatar;
    }

    public void start() {
        if (running || avatar == null) return;

        ObjectAnimator breathe = ObjectAnimator.ofFloat(
                avatar, View.SCALE_Y, 1.0f, 1.008f, 1.0f
        );
        breathe.setDuration(2600);
        breathe.setRepeatCount(ValueAnimator.INFINITE);
        breathe.setRepeatMode(ValueAnimator.RESTART);

        ObjectAnimator sway = ObjectAnimator.ofFloat(
                avatar, View.TRANSLATION_Y, 0f, -1.5f, 0f
        );
        sway.setDuration(2600);
        sway.setRepeatCount(ValueAnimator.INFINITE);
        sway.setRepeatMode(ValueAnimator.RESTART);

        animator = new AnimatorSet();
        animator.playTogether(breathe, sway);
        animator.start();
        running = true;
    }

    public void stop() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        if (avatar != null) {
            avatar.setScaleY(1f);
            avatar.setTranslationY(0f);
        }
        running = false;
    }

    public boolean isRunning() {
        return running;
    }
}
