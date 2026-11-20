package com.bubai.riya;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final int OVERLAY_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 48, 32, 32);

        TextView title = new TextView(this);
        title.setText("RIYA ❤️\nBUBAI AI Companion");
        title.setTextSize(25);
        title.setPadding(0, 20, 0, 40);
        layout.addView(title);

        Button start = new Button(this);
        start.setText("Wake RIYA / Show on Screen");
        layout.addView(start);

        Button stop = new Button(this);
        stop.setText("Let RIYA Sleep / Hide");
        layout.addView(stop);

        start.setOnClickListener(v -> showRiya());
        stop.setOnClickListener(v ->
                stopService(new Intent(this, FloatingService.class)));

        setContentView(layout);
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
