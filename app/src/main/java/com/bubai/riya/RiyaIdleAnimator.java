package com.bubai.riya;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;

public class RiyaIdleAnimator {
    private final ImageView avatar;
    private AnimatorSet animator;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running = false;
    private boolean blinking = false;

    private final Runnable blinkRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running || avatar == null) return;

            blinking = true;
            avatar.setImageResource(R.drawable.riya_blink);

            handler.postDelayed(() -> {
                if (running && avatar != null) {
                    avatar.setImageResource(R.drawable.riya_awake);
                }
                blinking = false;
                if (running) handler.postDelayed(this, 4500);
            }, 180);
        }
    };

    public RiyaIdleAnimator(ImageView avatar) {
        this.avatar = avatar;
    }

    public void start() {
        if (running || avatar == null) return;

        running = true;

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

        handler.postDelayed(blinkRunnable, 4500);
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(blinkRunnable);
        handler.removeCallbacksAndMessages(null);

        if (blinking && avatar != null) {
            avatar.setImageResource(R.drawable.riya_awake);
        }
        blinking = false;

        if (animator != null) {
            animator.cancel();
            animator = null;
        }

        if (avatar != null) {
            avatar.setScaleY(1f);
            avatar.setTranslationY(0f);
        }
    }

    public boolean isRunning() {
        return running;
    }
}
