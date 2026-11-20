package com.bubai.riya;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.speech.RecognizerIntent;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private static final int OVERLAY_REQUEST = 1001;
    private LinearLayout chatMessages;
    private ScrollView chatScroll;
    private EditText messageInput;
    private Button sendButton;
    private Button micButton, voiceButton, replayButton;
    private SpeechRecognizer speechRecognizer;
    private boolean voiceEnabled = true;
    private static final int MIC_PERMISSION = 2002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(15, 17, 38));
        getWindow().setNavigationBarColor(Color.rgb(15, 17, 38));
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        voiceEnabled = getSharedPreferences("riya_settings", MODE_PRIVATE)
                .getBoolean("voice_enabled", true);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 12, 16, 10);
        layout.setBackgroundColor(Color.rgb(15, 17, 38));
        layout.setFocusableInTouchMode(true);

        TextView title = new TextView(this);
        title.setText("RIYA  ♥\\nBUBAI AI COMPANION");
        title.setTextSize(23);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(8, 14, 8, 14);
        title.setBackground(roundBg(0xFF29234F, 22));
        layout.addView(title);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setGravity(Gravity.CENTER);
        topRow.setOrientation(LinearLayout.HORIZONTAL);

        Button settingsButton = makeButton("⚙ Settings", 0xFF39345F);
        settingsButton.setOnClickListener(v -> showTokenSettings());
        topRow.addView(settingsButton, weightedParams());

        Button start = makeButton("☀ Wake RIYA", 0xFF6846A5);
        start.setOnClickListener(v -> showRiya());
        topRow.addView(start, weightedParams());

        Button stop = makeButton("☾ Sleep", 0xFF39345F);
        stop.setOnClickListener(v ->
                stopService(new Intent(this, FloatingService.class)));
        topRow.addView(stop, weightedParams());
        layout.addView(topRow);

        LinearLayout voiceRow = new LinearLayout(this);
        voiceRow.setGravity(Gravity.CENTER);
        voiceRow.setPadding(0, 8, 0, 8);

        voiceButton = makeButton("", 0xFF39345F);
        updateVoiceButton();
        voiceButton.setOnClickListener(v -> toggleVoice());
        voiceRow.addView(voiceButton, weightedParams());

        replayButton = makeButton("↻ Listen Again", 0xFF6846A5);
        replayButton.setOnClickListener(v -> {
            Intent replay = new Intent("com.bubai.riya.REPLAY_LAST_REPLY");
            replay.setPackage(getPackageName());
            sendBroadcast(replay);
            Toast.makeText(this, "শেষ উত্তরটি আবার শোনানোর চেষ্টা করছি ❤️",
                    Toast.LENGTH_SHORT).show();
        });
        voiceRow.addView(replayButton, weightedParams());
        layout.addView(voiceRow);

        TextView chatTitle = new TextView(this);
        chatTitle.setText("💜  RIYA-র সঙ্গে কথা বলো");
        chatTitle.setTextSize(18);
        chatTitle.setTextColor(0xFFFFD6EB);
        chatTitle.setPadding(6, 12, 6, 10);
        layout.addView(chatTitle);

        chatScroll = new ScrollView(this);
        chatScroll.setFillViewport(true);

        chatMessages = new LinearLayout(this);
        chatMessages.setOrientation(LinearLayout.VERTICAL);
        chatMessages.setPadding(8, 10, 8, 10);
        chatScroll.addView(chatMessages);

        layout.addView(chatScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        inputRow.setPadding(8, 8, 8, 8);
        inputRow.setBackground(roundBg(0xFF292542, 24));

        micButton = makeButton("🎙", 0xFF6846A5);
        micButton.setOnClickListener(v -> startVoiceInput());
        inputRow.addView(micButton);

        messageInput = new EditText(this);
        messageInput.setSingleLine(false);
        messageInput.setMaxLines(4);
        messageInput.setTextSize(16);
        messageInput.setTextColor(Color.WHITE);
        messageInput.setHintTextColor(0xFFB8B3CC);
        messageInput.setHint("এখানে মেসেজ লেখো...");
        messageInput.setPadding(14, 10, 14, 10);
        messageInput.setBackground(roundBg(0xFF393550, 18));
        messageInput.setImeOptions(EditorInfo.IME_ACTION_SEND);
        inputRow.addView(messageInput, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        sendButton = makeButton("পাঠাও ➤", 0xFFB04D91);
        inputRow.addView(sendButton);
        layout.addView(inputRow);

        sendButton.setOnClickListener(v -> sendMessage());

        setContentView(layout);
        addMessage("RIYA ❤️", "হ্যালো বুবাই! ❤️ আমি এখানে আছি। আমার সঙ্গে কথা বলো।");
    }

    private GradientDrawable roundBg(int color, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(radius);
        return bg;
    }

    private Button makeButton(String text, int color) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setPadding(8, 6, 8, 6);
        button.setBackground(roundBg(color, 18));
        return button;
    }

    private LinearLayout.LayoutParams weightedParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(3, 3, 3, 3);
        return params;
    }

    private void updateVoiceButton() {
        if (voiceButton != null) {
            voiceButton.setText(voiceEnabled ? "🔊 RIYA Voice: ON" : "🔇 RIYA Voice: OFF");
        }
    }

    private void toggleVoice() {
        voiceEnabled = !voiceEnabled;
        getSharedPreferences("riya_settings", MODE_PRIVATE)
                .edit().putBoolean("voice_enabled", voiceEnabled).apply();

        Intent intent = new Intent("com.bubai.riya.SET_VOICE_ENABLED");
        intent.setPackage(getPackageName());
        intent.putExtra("enabled", voiceEnabled);
        sendBroadcast(intent);
        updateVoiceButton();

        Toast.makeText(this, voiceEnabled ? "RIYA Voice চালু ❤️" : "RIYA Voice বন্ধ",
                Toast.LENGTH_SHORT).show();
    }

    private void startVoiceInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "এই ফোনে Speech Recognition পাওয়া যাচ্ছে না।",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},
                    MIC_PERMISSION);
            return;
        }

        if (speechRecognizer != null) speechRecognizer.destroy();
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                runOnUiThread(() -> micButton.setText("🎙 শুনছি..."));
            }
            @Override public void onResults(Bundle results) {
                java.util.ArrayList<String> matches =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    messageInput.setText(matches.get(0));
                    messageInput.setSelection(messageInput.length());
                }
                micButton.setText("🎙");
            }
            @Override public void onError(int error) {
                micButton.setText("🎙");
                Toast.makeText(MainActivity.this, "কথা বোঝা যায়নি, আবার চেষ্টা করো।",
                        Toast.LENGTH_SHORT).show();
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-IN");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-IN");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        speechRecognizer.startListening(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_PERMISSION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startVoiceInput();
        } else if (requestCode == MIC_PERMISSION) {
            Toast.makeText(this, "Microphone permission প্রয়োজন।",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void addMessage(String sender, String message) {
        TextView bubble = new TextView(this);
        bubble.setText(sender + ":\n" + message);
        bubble.setTextSize(16);
        bubble.setTextColor(Color.WHITE);
        bubble.setPadding(16, 12, 16, 12);
        bubble.setBackground(roundBg(sender.equals("তুমি") ? 0xFF514078 : 0xFF34304D, 18));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 5, 0, 5);
        chatMessages.addView(bubble, params);
        chatScroll.post(() -> chatScroll.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void sendMessage() {
        String message = messageInput.getText().toString().trim();

        if (message.isEmpty()) return;

        messageInput.setText("");
        addMessage("তুমি", message);
        sendButton.setEnabled(false);
        sendButton.setText("...");

        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(messageInput.getWindowToken(), 0);

        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL("http://127.0.0.1:8765/api/riya/chat");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(60000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                JSONObject request = new JSONObject();
                request.put("message", message);

                byte[] body = request.toString().getBytes("UTF-8");
                connection.setFixedLengthStreamingMode(body.length);

                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body);
                }

                int statusCode = connection.getResponseCode();
                InputStream stream = statusCode >= 200 && statusCode < 300
                        ? connection.getInputStream() : connection.getErrorStream();

                StringBuilder response = new StringBuilder();
                if (stream != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(stream, "UTF-8"))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }
                }

                if (statusCode < 200 || statusCode >= 300) {
                    throw new Exception("Server HTTP " + statusCode);
                }

                JSONObject json = new JSONObject(response.toString());
                JSONObject result = json.optJSONObject("result");
                String reply = result != null
                        ? result.optString("message", "বুবাই, উত্তরটা বুঝতে পারিনি।")
                        : "বুবাই, সার্ভারের উত্তর ঠিকমতো পাওয়া যায়নি।";

                runOnUiThread(() -> {
                    addMessage("RIYA ❤️", reply);
                    Intent speechIntent = new Intent("com.bubai.riya.SPEAK_REPLY");
                    speechIntent.setPackage(getPackageName());
                    speechIntent.putExtra("reply", reply);
                    sendBroadcast(speechIntent);
                });

            } catch (Exception error) {
                runOnUiThread(() -> addMessage("RIYA",
                        "বুবাই, BUBAI Brain-এর সঙ্গে সংযোগ হচ্ছে না। "
                        + "Termux-এ সার্ভার চালু আছে কি না দেখো।"));
            } finally {
                if (connection != null) connection.disconnect();
                runOnUiThread(() -> {
                    sendButton.setEnabled(true);
                    sendButton.setText("পাঠাও");
                });
            }
        }).start();
    }


    private void showTokenSettings() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL("http://127.0.0.1:8765/api/riya/settings");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                }

                int current = new JSONObject(response.toString())
                        .optInt("max_output_tokens", 500);
                runOnUiThread(() -> showTokenDialog(current));

            } catch (Exception error) {
                runOnUiThread(() -> Toast.makeText(this,
                        "বুবাই, Settings লোড হয়নি। BUBAI Server চালু আছে কি না দেখো।",
                        Toast.LENGTH_LONG).show());
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void showTokenDialog(int current) {
        int[] values = {200, 400, 500, 700, 1000};
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);
        group.setPadding(40, 20, 40, 20);

        for (int value : values) {
            RadioButton option = new RadioButton(this);
            option.setId(value);
            option.setText(value + " tokens");
            option.setTextSize(17);
            group.addView(option);
        }

        group.check(current);
        new AlertDialog.Builder(this)
                .setTitle("RIYA Response Length")
                .setMessage("RIYA-র উত্তরের দৈর্ঘ্য বেছে নাও")
                .setView(group)
                .setPositiveButton("Save", (dialog, which) -> {
                    int selected = group.getCheckedRadioButtonId();
                    if (selected == -1) selected = 500;
                    saveTokenLimit(selected);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveTokenLimit(int value) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL("http://127.0.0.1:8765/api/riya/settings");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type",
                        "application/json; charset=UTF-8");

                JSONObject request = new JSONObject();
                request.put("max_output_tokens", value);
                byte[] body = request.toString().getBytes("UTF-8");

                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body);
                }

                int status = connection.getResponseCode();
                if (status < 200 || status >= 300) {
                    throw new Exception("HTTP " + status);
                }

                runOnUiThread(() -> Toast.makeText(this,
                        "RIYA Settings saved: " + value + " tokens ❤️",
                        Toast.LENGTH_LONG).show());

            } catch (Exception error) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Settings save হয়নি। Server connection পরীক্ষা করো।",
                        Toast.LENGTH_LONG).show());
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void showRiya() {
        if (Settings.canDrawOverlays(this)) {
            startService(new Intent(this, FloatingService.class));
        } else {
            Intent permission = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            startActivityForResult(permission, OVERLAY_REQUEST);
        }
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_REQUEST && Settings.canDrawOverlays(this)) {
            showRiya();
        }
    }
}
