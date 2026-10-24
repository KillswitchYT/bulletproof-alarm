package com.killswitch.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.json.JSONArray;
import org.json.JSONObject;

public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent in) {
        String act = in.getAction();
        if ("TIMER".equals(act)) { Timers.fired(c, in.getIntExtra("id", 0)); return; }
        if ("T_ADD".equals(act) || "T_STOP".equals(act)) { Timers.action(c, in); return; }
        if ("BED".equals(act)) { Scheduler.bedNotify(c); Scheduler.scheduleAll(c); return; }
        long t = in.getLongExtra("t", 0);
        int code = in.getIntExtra("i", 0);
        String key = "f" + (code / 2);
        if (String.valueOf(t).equals(Storage.get(c, key))) return;
        if ("BACKUP".equals(act)) Storage.set(c, "missed", "1");
        Storage.set(c, key, String.valueOf(t));
        if (code < 9000) markFired(c, code / 2, t);
        Intent s = new Intent(c, RingService.class)
            .putExtra("cfg", in.getStringExtra("cfg"))
            .putExtra("test", in.getBooleanExtra("test", false));
        c.startForegroundService(s);
        Scheduler.scheduleAll(c);
    }

    /** One-time and fixed-date alarms switch themselves off after ringing; spent skips are cleared. */
    static void markFired(Context c, int idx, long t) {
        try {
            JSONArray a = Scheduler.load(c);
            JSONObject o = a.optJSONObject(idx);
            if (o == null) return;
            JSONArray d = o.optJSONArray("d");
            boolean once = d == null || d.length() == 0;
            if (once || !o.optString("dt", "").isEmpty()) o.put("on", 0);
            if (o.optLong("sk", 0) <= t) o.remove("sk");
            Storage.set(c, "al", a.toString());
        } catch (Exception e) { }
    }
}
