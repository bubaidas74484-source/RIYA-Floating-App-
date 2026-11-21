package com.bubai.riya;

import android.app.Service;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
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
import android.widget.FrameLayout;
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
    private boolean voiceEnabled = true;
    private String lastSpokenReply;
    private volatile String currentSpeechText = "";
    private RiyaIdleAnimator idleAnimator;
    private RiyaSpeakingAnimator speakingAnimator;
    private float touchX, touchY;
    private int startX, startY;

    @Override
    public void onCreate() {
        super.onCreate();

        voiceEnabled = getSharedPreferences("riya_settings", MODE_PRIVATE)
                .getBoolean("voice_enabled", true);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(10, 10, 10, 10);
        panel.setBackgroundColor(Color.rgb(255, 225, 239));

        FrameLayout avatarFrame = new FrameLayout(this);
        LinearLayout.LayoutParams frameParams =
                new LinearLayout.LayoutParams(240, 394);
        avatarFrame.setLayoutParams(frameParams);

        ImageView avatar = new ImageView(this);
        avatar.setImageResource(R.drawable.riya_sleeping);
        avatar.setScaleType(ImageView.ScaleType.FIT_CENTER);

        FrameLayout.LayoutParams imageParams =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                );
        avatar.setLayoutParams(imageParams);
        avatar.setClipToOutline(true);
        avatar.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        avatar.setBackgroundColor(Color.WHITE);
        avatarFrame.addView(avatar);

        RiyaMouthView mouth = new RiyaMouthView(this);
        FrameLayout.LayoutParams mouthParams =
                new FrameLayout.LayoutParams(62, 39);
        mouthParams.leftMargin = 140;
        mouthParams.topMargin = 131;
        avatarFrame.addView(mouth, mouthParams);

        idleAnimator = new RiyaIdleAnimator(avatar);
        speakingAnimator = new RiyaSpeakingAnimator(mouth);
        panel.addView(avatarFrame);

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

        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                if ("RIYA_REPLY".equals(utteranceId)) {
                    new android.os.Handler(getMainLooper()).post(() -> {
                        idleAnimator.stop();
                        speakingAnimator.start();
                        status.setText("RIYA • কথা বলছে 🗣️");
                    });
                }
            }

            @Override
            public void onRangeStart(String utteranceId, int start, int end, int frame) {
                if ("RIYA_REPLY".equals(utteranceId)) {
                    String spoken = currentSpeechText;
                    if (spoken == null || spoken.isEmpty()) return;
                    int index = Math.max(0, Math.min(start, spoken.length() - 1));
                    int codePoint = spoken.codePointAt(index);
                    char spokenChar = Character.toChars(codePoint)[0];
                    new android.os.Handler(getMainLooper()).post(() ->
                            speakingAnimator.onTextCharacter(spokenChar));
                }
            }

            @Override
            public void onDone(String utteranceId) {
                if ("RIYA_REPLY".equals(utteranceId)) {
                    new android.os.Handler(getMainLooper()).post(() -> {
                        speakingAnimator.stop();
                        status.setText(awake ? "RIYA • Awake ✨" : "RIYA • Sleeping 💤");
                        if (awake) idleAnimator.start(); else idleAnimator.stop();
                    });
                }
            }

            @Override
            public void onError(String utteranceId) {
                if ("RIYA_REPLY".equals(utteranceId)) {
                    new android.os.Handler(getMainLooper()).post(() -> {
                        speakingAnimator.stop();
                        if (awake) idleAnimator.start(); else idleAnimator.stop();
                    });
                }
            }
        });

        speechReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if ("com.bubai.riya.SET_VOICE_ENABLED".equals(action)) {
                    voiceEnabled = intent.getBooleanExtra("enabled", true);
                    getSharedPreferences("riya_settings", MODE_PRIVATE)
                            .edit().putBoolean("voice_enabled", voiceEnabled).apply();
                    if (!voiceEnabled && textToSpeech != null) {
                        textToSpeech.stop();
                    }
                    return;
                }

                if ("com.bubai.riya.REPLAY_LAST_REPLY".equals(action)) {
                    if (lastSpokenReply != null) {
                        pendingReply = lastSpokenReply;
                        speakPendingReply();
                    }
                    return;
                }

                String reply = intent.getStringExtra("reply");
                if (reply == null || reply.trim().isEmpty()) return;

                awake = true;
                avatar.setImageResource(R.drawable.riya_awake);
                idleAnimator.stop();
                status.setText("RIYA • কথা বলছে 🗣️");
                lastSpokenReply = reply;
                pendingReply = reply;
                speakPendingReply();
            }
        };

        IntentFilter speechFilter = new IntentFilter();
        speechFilter.addAction("com.bubai.riya.SPEAK_REPLY");
        speechFilter.addAction("com.bubai.riya.SET_VOICE_ENABLED");
        speechFilter.addAction("com.bubai.riya.REPLAY_LAST_REPLY");

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
                                (LinearLayout.LayoutParams) avatarFrame.getLayoutParams();
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
                                (LinearLayout.LayoutParams) avatarFrame.getLayoutParams();
                        lp.width = newWidth;
                        lp.height = newHeight;
                        avatarFrame.setLayoutParams(lp);
                        float scaleX = newWidth / 240f;
                        float scaleY = newHeight / 394f;
                        mouth.setPivotX(0f);
                        mouth.setPivotY(0f);
                        mouth.setScaleX(scaleX);
                        mouth.setScaleY(scaleY);
                        mouth.setTranslationX(140f * (scaleX - 1f));
                        mouth.setTranslationY(131f * (scaleY - 1f));

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
                        if (awake) idleAnimator.start(); else idleAnimator.stop();
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
        if (!voiceEnabled || !ttsReady || textToSpeech == null || pendingReply == null) return;
        String speechText = pendingReply
                .replaceAll("[\\p{So}\\p{Sk}\\uFE0F\\u200D\\u20E3]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (!speechText.isEmpty()) {
            currentSpeechText = speechText;
            textToSpeech.speak(
                    speechText,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "RIYA_REPLY"
            );
        }
        pendingReply = null;
    }

    @Override
    public void onDestroy() {
        if (idleAnimator != null) idleAnimator.stop();
        if (speakingAnimator != null) speakingAnimator.stop();
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
