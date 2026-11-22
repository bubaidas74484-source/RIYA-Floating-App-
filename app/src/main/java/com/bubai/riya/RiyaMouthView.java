package com.bubai.riya;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class RiyaMouthView extends View {
    private final Paint paint = new Paint(
            Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG
    );

    private final Bitmap closedMouth;
    private final Bitmap smallMouth;
    private final Bitmap openMouth;

    private ValueAnimator animator;
    private float opening = 0f;
    private float characterStrength = 0.90f;

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

        opening = 0.08f;
        characterStrength = 0.90f;

        setVisibility(VISIBLE);

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(420);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());

        animator.addUpdateListener(v -> {
            float phase = (float) v.getAnimatedValue();
            float pulse = (float) Math.sin(phase * Math.PI);

            opening = 0.04f + characterStrength * pulse * 0.96f;
            invalidate();
        });

        animator.start();
        invalidate();
    }

    public void setMouthForCharacter(char ch) {
        switch (ch) {
            case 'আ': case 'া':
            case 'অ': case 'ও': case 'ো':
            case 'ঔ': case 'ৌ':
                characterStrength = 1.00f;
                break;

            case 'এ': case 'ঐ': case 'ে': case 'ৈ':
            case 'ই': case 'ঈ': case 'ি': case 'ী':
            case 'উ': case 'ঊ': case 'ু': case 'ূ':
                characterStrength = 0.90f;
                break;

            case 'ম': case 'প': case 'ব': case 'ভ':
                characterStrength = 0.35f;
                break;

            case '।': case ',': case '?': case '!':
                characterStrength = 0.12f;
                break;

            case ' ':
                characterStrength = 0.30f;
                break;

            default:
                characterStrength = 0.85f;
                break;
        }
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

        if (opening >= 0.58f) {
            mouth = openMouth;
        } else if (opening >= 0.16f) {
            mouth = smallMouth;
        } else {
            mouth = closedMouth;
        }

        if (mouth != null && !mouth.isRecycled()) {
            float drawHeight = getHeight() * (0.85f + opening * 0.55f);
            float centerY = getHeight() / 2f;

            RectF destination = new RectF(
                    0,
                    centerY - drawHeight / 2f,
                    getWidth(),
                    centerY + drawHeight / 2f
            );

            canvas.drawBitmap(mouth, null, destination, paint);
        }
    }
}
