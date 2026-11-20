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
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);

        LinearLayout.LayoutParams imageParams =
                new LinearLayout.LayoutParams(180, 180);
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

        View.OnTouchListener touchListener = (view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchX = event.getRawX();
                    touchY = event.getRawY();
                    startX = params.x;
                    startY = params.y;
                    return true;

                case MotionEvent.ACTION_UP:
                    float dx = event.getRawX() - touchX;
                    float dy = event.getRawY() - touchY;
                    if (Math.abs(dx) < 60 && Math.abs(dy) < 60) {
                        awake = !awake;
                        avatar.setImageResource(awake
                                ? R.drawable.riya_awake
                                : R.drawable.riya_sleeping);
                        status.setText(awake
                                ? "RIYA • Awake ✨"
                                : "RIYA • Sleeping 💤");
                    }
                    return true;

                case MotionEvent.ACTION_MOVE:
                    params.x = startX + (int)(event.getRawX() - touchX);
                    params.y = startY + (int)(event.getRawY() - touchY);
                    windowManager.updateViewLayout(floatingView, params);
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
