package com.bubai.riya;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Outline;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

public class FloatingService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private boolean awake = false;
    private float touchX, touchY;
    private int startX, startY;

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(10, 10, 10, 10);
        panel.setBackgroundColor(Color.rgb(255, 225, 239));

        ImageView avatar = new ImageView(this);
        avatar.setImageResource(R.drawable.riya_sleeping);
        avatar.setScaleType(ImageView.ScaleType.FIT_CENTER);

        LinearLayout.LayoutParams imageParams =
                new LinearLayout.LayoutParams(240, 394);
        avatar.setLayoutParams(imageParams);
        avatar.setClipToOutline(true);
        avatar.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        avatar.setBackgroundColor(Color.WHITE);
        panel.addView(avatar);

        TextView status = new TextView(this);
        status.setText("RIYA • Sleeping 💤");
        status.setTextColor(Color.rgb(100, 40, 75));
        status.setTextSize(12);
        status.setTypeface(null, Typeface.BOLD);
        status.setGravity(Gravity.CENTER);
        status.setPadding(4, 8, 4, 2);
        panel.addView(status);

        floatingView = panel;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 20;
        params.y = 250;

        float[] initialSpan = {0f};
        int[] baseWidth = {240};
        int[] baseHeight = {394};
        float[] baseTextSize = {12f};
        boolean[] pinching = {false};

        View.OnTouchListener touchListener = (view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    touchX = event.getRawX();
                    touchY = event.getRawY();
                    startX = params.x;
                    startY = params.y;
                    pinching[0] = false;
                    return true;

                case MotionEvent.ACTION_POINTER_DOWN:
                    if (event.getPointerCount() >= 2) {
                        float x = event.getX(0) - event.getX(1);
                        float y = event.getY(0) - event.getY(1);
                        initialSpan[0] = (float) Math.hypot(x, y);

                        LinearLayout.LayoutParams lp =
                                (LinearLayout.LayoutParams) avatar.getLayoutParams();
                        baseWidth[0] = lp.width;
                        baseHeight[0] = lp.height;
                        baseTextSize[0] = status.getTextSize() /
                                getResources().getDisplayMetrics().scaledDensity;
                        pinching[0] = true;
                    }
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (event.getPointerCount() >= 2 && initialSpan[0] > 0) {
                        pinching[0] = true;
                        float x = event.getX(0) - event.getX(1);
                        float y = event.getY(0) - event.getY(1);
                        float currentSpan = (float) Math.hypot(x, y);
                        float scale = currentSpan / initialSpan[0];
                        scale = Math.max(0.65f, Math.min(scale, 1.8f));

                        int newWidth = Math.round(baseWidth[0] * scale);
                        int newHeight = Math.round(baseHeight[0] * scale);

                        LinearLayout.LayoutParams lp =
                                (LinearLayout.LayoutParams) avatar.getLayoutParams();
                        lp.width = newWidth;
                        lp.height = newHeight;
                        avatar.setLayoutParams(lp);

                        status.setTextSize(baseTextSize[0] * scale);
                        int pad = Math.round(10 * scale);
                        panel.setPadding(pad, pad, pad, pad);

                        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
                        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                        windowManager.updateViewLayout(floatingView, params);
                    } else if (event.getPointerCount() == 1 && !pinching[0]) {
                        params.x = startX + (int)(event.getRawX() - touchX);
                        params.y = startY + (int)(event.getRawY() - touchY);
                        windowManager.updateViewLayout(floatingView, params);
                    }
                    return true;

                case MotionEvent.ACTION_POINTER_UP:
                    pinching[0] = true;
                    return true;

                case MotionEvent.ACTION_UP:
                    float dx = event.getRawX() - touchX;
                    float dy = event.getRawY() - touchY;
                    if (!pinching[0] && Math.abs(dx) < 60 && Math.abs(dy) < 60) {
                        awake = !awake;
                        avatar.setImageResource(awake
                                ? R.drawable.riya_awake
                                : R.drawable.riya_sleeping);
                        status.setText(awake
                                ? "RIYA • Awake ✨"
                                : "RIYA • Sleeping 💤");
                    }
                    initialSpan[0] = 0f;
                    pinching[0] = false;
                    return true;
            }
            return true;
        };
        panel.setOnTouchListener(touchListener);
        avatar.setOnTouchListener(touchListener);
        status.setOnTouchListener(touchListener);

        windowManager.addView(floatingView, params);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
