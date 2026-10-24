package com.killswitch.alarm;

import android.app.Activity;
import android.content.Intent;
import android.webkit.JavascriptInterface;

public class Bridge {
    final Activity a;

    Bridge(Activity a) { this.a = a; }

    @JavascriptInterface public String get(String k) { return Storage.get(a, k); }

    @JavascriptInterface public void set(String k, String v) {
        Storage.set(a, k, v);
        if ("al".equals(k) || "bed".equals(k)) Scheduler.scheduleAll(a);
        else if ("tm".equals(k)) Timers.scheduleAll(a);
        else if ("tfmt".equals(k)) ClockWidget.refresh(a);
    }

    /** The phone's own 24-hour setting (the page applies any in-app override itself). */
    @JavascriptInterface public boolean is24() { return android.text.format.DateFormat.is24HourFormat(a); }

    @JavascriptInterface public String perms() {
        boolean[] r = Perms.check(a);
        StringBuilder s = new StringBuilder("[");
        for (int i = 0; i < r.length; i++) s.append(i > 0 ? "," : "").append(r[i] ? 1 : 0);
        return s.append("]").toString();
    }

    @JavascriptInterface public void fix(final int i) {
        a.runOnUiThread(new Runnable() { public void run() { Perms.fix(a, i); } });
    }

    @JavascriptInterface public void rearm() { Scheduler.scheduleAll(a); }

    @JavascriptInterface public void test(String cfg) {
        a.startForegroundService(new Intent(a, RingService.class).putExtra("cfg", cfg).putExtra("test", true));
    }

    /** Toggles "skip the next ring" for alarm idx and returns the updated alarm list. */
    @JavascriptInterface public String skipNext(int idx) {
        org.json.JSONArray all = Scheduler.load(a);
        try {
            org.json.JSONObject o = all.optJSONObject(idx);
            if (o != null) {
                if (o.optLong("sk", 0) > System.currentTimeMillis()) o.remove("sk");
                else { long t = Scheduler.nextFor(o); if (t > 0) o.put("sk", t); }
            }
        } catch (Exception e) { }
        Storage.set(a, "al", all.toString());
        Scheduler.scheduleAll(a);
        return all.toString();
    }

    @JavascriptInterface public void pickSound() {
        a.runOnUiThread(new Runnable() { public void run() { if (a instanceof MainActivity) ((MainActivity) a).pick(); } });
    }

    @JavascriptInterface public void night(final boolean on) {
        a.runOnUiThread(new Runnable() {
            public void run() {
                android.view.WindowManager.LayoutParams lp = a.getWindow().getAttributes();
                lp.screenBrightness = on ? 0.02f : -1f;
                a.getWindow().setAttributes(lp);
                if (on) a.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else a.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
    }

    @JavascriptInterface public void snooze() {
        a.startService(new Intent(a, RingService.class).setAction("SNOOZE"));
    }

    @JavascriptInterface public void dismiss() {
        a.startService(new Intent(a, RingService.class).setAction("DISMISS"));
    }

    @JavascriptInterface public void bars(final int rgb, final boolean dark) {
        a.runOnUiThread(new Runnable() {
            public void run() {
                android.view.Window w = a.getWindow();
                int c = 0xFF000000 | rgb;
                w.setStatusBarColor(c);
                w.setNavigationBarColor(c);
                android.view.View d = w.getDecorView();
                int f = d.getSystemUiVisibility();
                if (dark) f &= ~(0x2000 | 0x10); else f |= (0x2000 | 0x10);
                d.setSystemUiVisibility(f);
            }
        });
    }
}
