package com.bubai.riya;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class RiyaMouthView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Bitmap closedMouth;
    private final Bitmap smallMouth;
    private final Bitmap openMouth;

    private ValueAnimator animator;
    private float opening = 0f;

    public RiyaMouthView(Context context) {
        super(context);

        closedMouth = BitmapFactory.decodeResource(
                getResources(), R.drawable.riya_mouth_closed);
        smallMouth = BitmapFactory.decodeResource(
                getResources(), R.drawable.riya_mouth_small);
        openMouth = BitmapFactory.decodeResource(
                getResources(), R.drawable.riya_mouth_open);

        setVisibility(INVISIBLE);
    }

    public void startSpeaking() {
        stopAnimator();
        opening = 0.03f;
        setVisibility(VISIBLE);
        invalidate();
    }

    public void setMouthForCharacter(char ch) {
        float target;

        switch (ch) {
            case 'আ': case 'া':
                target = 0.95f;
                break;
            case 'অ': case 'ও': case 'ো':
                target = 0.75f;
                break;
            case 'ই': case 'ঈ': case 'ি': case 'ী':
            case 'উ': case 'ঊ': case 'ু': case 'ূ':
                target = 0.30f;
                break;
            case 'এ': case 'ঐ': case 'ে': case 'ৈ':
                target = 0.55f;
                break;
            case 'ঔ': case 'ৌ':
                target = 0.85f;
                break;
            case 'ম': case 'প': case 'ব': case 'ভ':
                target = 0.02f;
                break;
            case '।': case ',': case '?': case '!':
                target = 0.02f;
                break;
            case ' ':
                target = 0.08f;
                break;
            default:
                target = 0.40f;
                break;
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

        Bitmap mouth;
        if (opening >= 0.60f) {
            mouth = openMouth;
        } else if (opening >= 0.12f) {
            mouth = smallMouth;
        } else {
            mouth = closedMouth;
        }

        if (mouth != null && !mouth.isRecycled()) {
            canvas.drawBitmap(
                    mouth,
                    null,
                    new android.graphics.RectF(0, 0, getWidth(), getHeight()),
                    paint
            );
        }
    }
}
