package com.killswitch.alarm;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.webkit.WebView;

public class RingActivity extends Activity {
    static RingActivity cur;
    WebView w;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        cur = this;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        w = Web.create(this);
        setContentView(w);
        Intent in = getIntent();
        String l = in.getStringExtra("l");
        w.loadUrl(Web.URL + "?ring=1&label=" + Uri.encode(l == null ? "Alarm" : l)
            + "&test=" + (in.getBooleanExtra("test", false) ? 1 : 0)
            + "&sz=" + in.getIntExtra("sz", 5) + "&ch=" + in.getIntExtra("ch", 0)
            + "&timer=" + in.getIntExtra("timer", 0));
    }

    @Override public boolean onKeyDown(int k, KeyEvent e) {
        if (k == KeyEvent.KEYCODE_VOLUME_UP || k == KeyEvent.KEYCODE_VOLUME_DOWN) {
            String m = Storage.get(this, "vbtn").replace("\"", "");
            boolean locked = getIntent().getIntExtra("ch", 0) != 0;
            if (m.equals("snooze")) { startService(new Intent(this, RingService.class).setAction("SNOOZE")); return true; }
            if (m.equals("stop") && !locked) { startService(new Intent(this, RingService.class).setAction("DISMISS")); return true; }
            if (m.equals("none") || m.equals("stop")) return true;
        }
        return super.onKeyDown(k, e);
    }

    @Override public void onBackPressed() { }

    @Override protected void onDestroy() {
        if (cur == this) cur = null;
        if (w != null) w.destroy();
        super.onDestroy();
    }
}
