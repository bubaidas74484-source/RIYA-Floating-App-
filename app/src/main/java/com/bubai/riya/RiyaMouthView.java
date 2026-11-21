package com.bubai.riya;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

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
        stopAnimator();
        opening = 0.04f;
        setVisibility(VISIBLE);
        invalidate();
    }

    public void setMouthForCharacter(char ch) {
        float target;
        switch (ch) {
            case 'আ': case 'া': target = 0.90f; break;
            case 'অ': case 'ও': case 'ো': target = 0.62f; break;
            case 'ই': case 'ঈ': case 'ি': case 'ী':
            case 'উ': case 'ঊ': case 'ু': case 'ূ':
                target = 0.28f; break;
            case 'এ': case 'ঐ': case 'ে': case 'ৈ':
                target = 0.52f; break;
            case 'ঔ': case 'ৌ': target = 0.72f; break;
            case 'ম': case 'প': case 'ব': case 'ভ':
                target = 0.035f; break;
            case '।': case ',': case '?': case '!':
                target = 0.02f; break;
            case ' ': target = 0.08f; break;
            default: target = 0.34f; break;
        }

        stopAnimator();
        animator = ValueAnimator.ofFloat(opening, target);
        animator.setDuration(85);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(v -> {
            opening = (float) v.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    private void stopAnimator() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    public void stopSpeaking() {
        stopAnimator();
        opening = 0f;
        setVisibility(INVISIBLE);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (opening < 0.02f) return;

        float w = getWidth(), h = getHeight();
        float cx = w * 0.55f, cy = h * 0.49f;
        float halfW = w * 0.43f;
        float openH = h * (0.025f + opening * 0.25f);

        Path mouth = new Path();
        mouth.moveTo(cx - halfW, cy);
        mouth.cubicTo(cx - halfW * 0.5f, cy - openH,
                cx + halfW * 0.5f, cy - openH, cx + halfW, cy);
        mouth.cubicTo(cx + halfW * 0.5f, cy + openH,
                cx - halfW * 0.5f, cy + openH, cx - halfW, cy);
        mouth.close();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF54212B);
        canvas.drawPath(mouth, paint);

        if (opening > 0.35f) {
            paint.setColor(0xFFE56F83);
            canvas.drawOval(cx - halfW * 0.35f, cy,
                    cx + halfW * 0.35f, cy + openH * 0.75f, paint);
        }

        paint.setColor(0xFFB94F65);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, h * 0.035f));
        canvas.drawPath(mouth, paint);
        paint.setStyle(Paint.Style.FILL);
    }
}
