package com.bubai.riya;

import android.app.Service;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
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
    private TextToSpeech textToSpeech;
    private boolean ttsReady = false;
    private String pendingReply;
    private BroadcastReceiver speechReceiver;
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

        textToSpeech = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                int language = textToSpeech.setLanguage(new Locale("bn", "IN"));
                if (language == TextToSpeech.LANG_MISSING_DATA ||
                        language == TextToSpeech.LANG_NOT_SUPPORTED) {
                    language = textToSpeech.setLanguage(new Locale("bn", "BD"));
                }
                ttsReady = language != TextToSpeech.LANG_MISSING_DATA &&
                        language != TextToSpeech.LANG_NOT_SUPPORTED;
                if (ttsReady) {
                    StringBuilder voices = new StringBuilder("বাংলা Voices: ");
                    for (android.speech.tts.Voice voice : textToSpeech.getVoices()) {
                        if (voice.getLocale().getLanguage().equals("bn")) {
                            voices.append(voice.getName()).append(" | ");
                        }
                    }
                    status.setText(voices.length() > 15 ? voices.toString() : "RIYA • বাংলা Voice পাওয়া গেছে");
                    speakPendingReply();
                } else status.setText("RIYA • বাংলা Voice পাওয়া যায়নি");
            } else {
                status.setText("RIYA • Voice চালু হয়নি");
            }
        });

        speechReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String reply = intent.getStringExtra("reply");
                if (reply == null || reply.trim().isEmpty()) return;

                awake = true;
                avatar.setImageResource(R.drawable.riya_awake);
                status.setText("RIYA • কথা বলছে 🗣️");
                pendingReply = reply;
                speakPendingReply();
            }
        };

        IntentFilter speechFilter = new IntentFilter("com.bubai.riya.SPEAK_REPLY");
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(speechReceiver, speechFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(speechReceiver, speechFilter);
        }

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

    private void speakPendingReply() {
        if (!ttsReady || textToSpeech == null || pendingReply == null) return;
        textToSpeech.speak(
                pendingReply,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "RIYA_REPLY"
        );
        pendingReply = null;
    }

    @Override
    public void onDestroy() {
        if (speechReceiver != null) {
            unregisterReceiver(speechReceiver);
            speechReceiver = null;
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
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
