package com.bubai.riya;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public class RiyaMouthView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator animator;
    private float opening = 0f;

    public RiyaMouthView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setVisibility(INVISIBLE);
    }

    public void startSpeaking() {
        if (animator != null && animator.isRunning()) return;

        setVisibility(VISIBLE);
        animator = ValueAnimator.ofFloat(0f, 1f, 0.25f, 0.8f, 0f);
        animator.setDuration(520);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(value -> {
            opening = (float) value.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    public void stopSpeaking() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        opening = 0f;
        setVisibility(INVISIBLE);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (opening < 0.08f) return;

        float cx = getWidth() * 0.62f;
        float cy = getHeight() * 0.43f;
        float halfWidth = getWidth() * 0.34f;
        float height = getHeight() * (0.08f + opening * 0.25f);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF54212B);

        RectF mouth = new RectF(
                cx - halfWidth,
                cy - height,
                cx + halfWidth,
                cy + height
        );
        canvas.drawOval(mouth, paint);

        if (opening > 0.55f) {
            paint.setColor(0xFF9E4053);
            RectF tongue = new RectF(
                    cx - halfWidth * 0.45f,
                    cy + height * 0.25f,
                    cx + halfWidth * 0.45f,
                    cy + height * 0.82f
            );
            canvas.drawOval(tongue, paint);
        }
    }
}
