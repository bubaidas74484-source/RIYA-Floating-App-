package com.bubai.riya;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.widget.ImageView;

public class RiyaSpeakingAnimator {
    private final ImageView avatar;
    private AnimatorSet animator;

    public RiyaSpeakingAnimator(ImageView avatar) {
        this.avatar = avatar;
    }

    public void start() {
        if (avatar == null || animator != null) return;

        ObjectAnimator pulseX = ObjectAnimator.ofFloat(
                avatar, View.SCALE_X, 1f, 1.045f, 1f
        );
        ObjectAnimator pulseY = ObjectAnimator.ofFloat(
                avatar, View.SCALE_Y, 1f, 1.035f, 1f
        );
        ObjectAnimator movement = ObjectAnimator.ofFloat(
                avatar, View.TRANSLATION_Y, 0f, -3f, 0f
        );

        pulseX.setDuration(650);
        pulseY.setDuration(650);
        movement.setDuration(650);

        pulseX.setRepeatCount(ValueAnimator.INFINITE);
        pulseY.setRepeatCount(ValueAnimator.INFINITE);
        movement.setRepeatCount(ValueAnimator.INFINITE);

        animator = new AnimatorSet();
        animator.playTogether(pulseX, pulseY, movement);
        animator.start();
    }

    public void stop() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        if (avatar != null) {
            avatar.setScaleX(1f);
            avatar.setScaleY(1f);
            avatar.setTranslationY(0f);
        }
    }
}
