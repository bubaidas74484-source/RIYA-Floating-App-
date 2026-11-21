package com.bubai.riya;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class RiyaMouthView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator animator;
    private float opening = 0f;

    public RiyaMouthView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setVisibility(INVISIBLE);
        paint.setStyle(Paint.Style.FILL);
    }

    public void startSpeaking() {
        if (animator != null && animator.isRunning()) return;

        setVisibility(VISIBLE);

        animator = ValueAnimator.ofFloat(
            0.08f, 0.42f, 0.16f, 0.72f,
            0.28f, 0.92f, 0.12f, 0.55f, 0.08f
        );
        animator.setDuration(1050);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
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

        if (opening < 0.03f) return;

        float w = getWidth();
        float h = getHeight();

        float cx = w * 0.55f;
        float cy = h * 0.49f;
        float halfW = w * 0.43f;
        float openH = h * (0.025f + opening * 0.25f);

        Path mouth = new Path();

        mouth.moveTo(cx - halfW, cy);
        mouth.cubicTo(
            cx - halfW * 0.55f, cy - openH * 0.45f,
            cx - halfW * 0.25f, cy - openH * 0.75f,
            cx, cy - openH * 0.38f
        );
        mouth.cubicTo(
            cx + halfW * 0.25f, cy - openH * 0.75f,
            cx + halfW * 0.55f, cy - openH * 0.45f,
            cx + halfW, cy
        );
        mouth.cubicTo(
            cx + halfW * 0.55f, cy + openH * 0.8f,
            cx + halfW * 0.25f, cy + openH,
            cx, cy + openH * 0.85f
        );
        mouth.cubicTo(
            cx - halfW * 0.25f, cy + openH,
            cx - halfW * 0.55f, cy + openH * 0.8f,
            cx - halfW, cy
        );
        mouth.close();

        paint.setColor(0xFF54212B);
        canvas.drawPath(mouth, paint);

        if (opening > 0.35f) {
            Path tongue = new Path();
            tongue.moveTo(cx - halfW * 0.38f, cy + openH * 0.38f);
            tongue.quadTo(
                cx, cy + openH * 1.05f,
                cx + halfW * 0.38f, cy + openH * 0.38f
            );
            tongue.quadTo(
                cx, cy + openH * 0.65f,
                cx - halfW * 0.38f, cy + openH * 0.38f
            );
            tongue.close();

            paint.setColor(0xFFE56F83);
            canvas.drawPath(tongue, paint);
        }

        paint.setColor(0xFFB94F65);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, h * 0.035f));
        canvas.drawPath(mouth, paint);
        paint.setStyle(Paint.Style.FILL);
    }
}
