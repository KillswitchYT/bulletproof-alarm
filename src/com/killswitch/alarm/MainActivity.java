package com.killswitch.alarm;

import android.app.Activity;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.webkit.WebView;
import java.util.ArrayList;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    WebView w;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        w = Web.create(this);
        setContentView(w);
        w.loadUrl(Web.URL);
        handle(getIntent());
        Scheduler.scheduleAll(this);
    }

    @Override protected void onNewIntent(Intent in) {
        super.onNewIntent(in);
        handle(in);
        Scheduler.scheduleAll(this);
        ping();
    }

    /** Lets voice assistants and other apps create alarms and timers here. */
    void handle(Intent in) {
        if (in == null || in.getAction() == null) return;
        String a = in.getAction();
        try {
            if (AlarmClock.ACTION_SET_ALARM.equals(a)) {
                int h = in.getIntExtra(AlarmClock.EXTRA_HOUR, 7), m = in.getIntExtra(AlarmClock.EXTRA_MINUTES, 0);
                String msg = in.getStringExtra(AlarmClock.EXTRA_MESSAGE);
                ArrayList<Integer> days = in.getIntegerArrayListExtra(AlarmClock.EXTRA_DAYS);
                JSONArray d = new JSONArray();
                if (days != null) for (Integer x : days) d.put(x - 1);
                JSONObject o = new JSONObject().put("t", String.format(Locale.US, "%02d:%02d", h, m))
                    .put("l", msg == null ? "" : msg).put("on", 1).put("d", d).put("dt", "")
                    .put("snd", "").put("vb", 1).put("sz", 5).put("ch", 0);
                JSONArray all = Scheduler.load(this);
                all.put(o);
                Storage.set(this, "al", all.toString());
                Storage.set(this, "goto", "home");
            } else if (AlarmClock.ACTION_SET_TIMER.equals(a)) {
                int len = in.getIntExtra(AlarmClock.EXTRA_LENGTH, 0);
                if (len > 0) {
                    String msg = in.getStringExtra(AlarmClock.EXTRA_MESSAGE);
                    long now = System.currentTimeMillis();
                    JSONObject o = new JSONObject().put("id", (int) (now % 900000000L) + 1000000)
                        .put("l", msg == null ? "" : msg).put("dur", len * 1000L).put("st", "run")
                        .put("end", now + len * 1000L).put("left", len * 1000L);
                    JSONArray all = Timers.load(this);
                    all.put(o);
                    Storage.set(this, "tm", all.toString());
                    Storage.set(this, "goto", "timer");
                }
            } else if (AlarmClock.ACTION_SHOW_ALARMS.equals(a)) {
                Storage.set(this, "goto", "home");
            }
        } catch (Exception e) { }
    }

    void pick() {
        Intent i = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        startActivityForResult(i, 77);
    }

    @Override protected void onActivityResult(int rc, int res, Intent d) {
        super.onActivityResult(rc, res, d);
        if (rc != 77 || res != RESULT_OK || d == null || w == null) return;
        Uri u = d.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        String title = "Custom sound";
        try { if (u != null) title = RingtoneManager.getRingtone(this, u).getTitle(this); } catch (Exception e) { }
        w.evaluateJavascript("window.onSound&&window.onSound(" + JSONObject.quote(u == null ? "" : u.toString())
            + "," + JSONObject.quote(title) + ")", null);
    }

    void ping() {
        if (w != null) w.evaluateJavascript("window.onResume&&window.onResume()", null);
    }

    @Override protected void onResume() { super.onResume(); ping(); }

    @Override public void onRequestPermissionsResult(int rc, String[] p, int[] r) {
        super.onRequestPermissionsResult(rc, p, r);
        ping();
    }

    @Override protected void onDestroy() {
        if (w != null) w.destroy();
        super.onDestroy();
    }
}
