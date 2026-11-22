package com.bubai.riya;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public class RiyaMouthView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Bitmap closedMouth;
    private final Bitmap smallMouth;
    private final Bitmap openMouth;

    private ValueAnimator animator;
    private float opening = 0f;

    public RiyaMouthView(Context context) {
        super(context);

        closedMouth = BitmapFactory.decodeResource(getResources(), R.drawable.riya_mouth_closed);
        smallMouth = BitmapFactory.decodeResource(getResources(), R.drawable.riya_mouth_small);
        openMouth = BitmapFactory.decodeResource(getResources(), R.drawable.riya_mouth_open);

        setVisibility(INVISIBLE);
    }

    public void startSpeaking() {
        stopAnimator();
        opening = 0.25f;
        setVisibility(VISIBLE);
        invalidate();
    }

    public void setMouthForCharacter(char ch) {
        float target;

        switch (ch) {
            case 'আ': case 'া':
                target = 1.00f;
                break;

            case 'অ': case 'ও': case 'ো':
                target = 0.95f;
                break;

            case 'ঔ': case 'ৌ':
                target = 1.00f;
                break;

            case 'এ': case 'ঐ': case 'ে': case 'ৈ':
                target = 0.78f;
                break;

            case 'ই': case 'ঈ': case 'ি': case 'ী':
            case 'উ': case 'ঊ': case 'ু': case 'ূ':
                target = 0.65f;
                break;

            case 'ম': case 'প': case 'ব': case 'ভ':
                target = 0.05f;
                break;

            case '।': case ',': case '?': case '!':
                target = 0.05f;
                break;

            case ' ':
                target = 0.18f;
                break;

            default:
                target = 0.82f;
                break;
        }

        stopAnimator();

        animator = ValueAnimator.ofFloat(opening, target);
        animator.setDuration(110);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());

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

        Bitmap mouth;

        if (opening >= 0.72f) {
            mouth = openMouth;
        } else if (opening >= 0.25f) {
            mouth = smallMouth;
        } else {
            mouth = closedMouth;
        }

        if (mouth != null && !mouth.isRecycled()) {
            canvas.drawBitmap(
                mouth,
                null,
                new RectF(0, 0, getWidth(), getHeight()),
                paint
            );
        }
    }
}
