package com.bubai.riya;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 24, 24, 16);

        TextView title = new TextView(this);
        title.setText("RIYA ❤️\nBUBAI AI Companion");
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 12, 0, 20);
        layout.addView(title);

        Button settingsButton = new Button(this);
        settingsButton.setText("⚙️ Settings");
        settingsButton.setOnClickListener(v -> showTokenSettings());
        layout.addView(settingsButton);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);

        Button start = new Button(this);
        start.setText("Wake RIYA");
        controls.addView(start, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button stop = new Button(this);
        stop.setText("Let RIYA Sleep");
        controls.addView(stop, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        layout.addView(controls);

        TextView chatTitle = new TextView(this);
        chatTitle.setText("💗 RIYA-র সঙ্গে কথা বলো");
        chatTitle.setTextSize(19);
        chatTitle.setPadding(4, 18, 4, 10);
        layout.addView(chatTitle);

        chatScroll = new ScrollView(this);
        chatMessages = new LinearLayout(this);
        chatMessages.setOrientation(LinearLayout.VERTICAL);
        chatMessages.setPadding(8, 8, 8, 8);
        chatScroll.addView(chatMessages);

        layout.addView(chatScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);

        messageInput = new EditText(this);
        messageInput.setSingleLine(true);
        messageInput.setTextSize(16);
        messageInput.setHint("এখানে মেসেজ লেখো...");
        inputRow.addView(messageInput, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        sendButton = new Button(this);
        sendButton.setText("পাঠাও");
        inputRow.addView(sendButton);

        layout.addView(inputRow);

        start.setOnClickListener(v -> showRiya());
        stop.setOnClickListener(v ->
                stopService(new Intent(this, FloatingService.class)));

        sendButton.setOnClickListener(v -> sendMessage());

        setContentView(layout);
        addMessage("RIYA", "হ্যালো বুবাই! ❤️ আমি এখানে আছি। আমার সঙ্গে কথা বলো।");
    }

    private void addMessage(String sender, String message) {
        TextView bubble = new TextView(this);
        bubble.setText(sender + ":\n" + message);
        bubble.setTextSize(16);
        bubble.setPadding(16, 12, 16, 12);
        bubble.setBackgroundColor(sender.equals("তুমি")
                ? 0xFFE3F2FD : 0xFFFCE4EC);

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

                runOnUiThread(() -> addMessage("RIYA ❤️", reply));

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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_REQUEST && Settings.canDrawOverlays(this)) {
            showRiya();
        }
    }
}
