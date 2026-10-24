package com.killswitch.alarm;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import org.json.JSONArray;
import org.json.JSONObject;

final class Timers {
    static JSONArray load(Context c) {
        try {
            String s = Storage.get(c, "tm");
            return new JSONArray(s.isEmpty() ? "[]" : s);
        } catch (Exception e) { return new JSONArray(); }
    }

    static PendingIntent tpi(Context c, int i, int id, int flags) {
        Intent in = new Intent(c, AlarmReceiver.class).setAction("TIMER").putExtra("id", id);
        return PendingIntent.getBroadcast(c, 5000 + i, in, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    static PendingIntent act(Context c, String a, int id) {
        Intent in = new Intent(c, AlarmReceiver.class).setAction(a).putExtra("id", id);
        return PendingIntent.getBroadcast(c, 7000 + (id % 1000) * 2 + (a.equals("T_ADD") ? 1 : 0), in,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void scheduleAll(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel("timer", "Running timers", NotificationManager.IMPORTANCE_LOW));
        for (int i = 0; i < 64; i++) {
            PendingIntent p = tpi(c, i, 0, PendingIntent.FLAG_NO_CREATE);
            if (p != null) { am.cancel(p); p.cancel(); }
        }
        JSONArray a = load(c);
        long now = System.currentTimeMillis();
        for (int i = 0; i < a.length() && i < 64; i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            int id = o.optInt("id");
            nm.cancel(6000 + (id % 1000));
            long end = o.optLong("end");
            if (!"run".equals(o.optString("st")) || end <= now) continue;
            try { am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, tpi(c, i, id, PendingIntent.FLAG_UPDATE_CURRENT)); }
            catch (SecurityException e) { }
            nm.notify(6000 + (id % 1000), new Notification.Builder(c, "timer").setSmallIcon(R.drawable.ic_small)
                .setContentTitle(o.optString("l", "").isEmpty() ? "Timer" : o.optString("l"))
                .setUsesChronometer(true).setChronometerCountDown(true).setWhen(end)
                .setOngoing(true).setOnlyAlertOnce(true).setContentIntent(Scheduler.show(c))
                .addAction(0, "+1 min", act(c, "T_ADD", id))
                .addAction(0, "Stop", act(c, "T_STOP", id)).build());
        }
    }

    static void fired(Context c, int id) {
        JSONArray a = load(c);
        String label = "Timer";
        try {
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o != null && o.optInt("id") == id) {
                    if (!o.optString("l", "").isEmpty()) label = o.optString("l");
                    o.put("st", "idle");
                    o.put("left", o.optLong("dur"));
                }
            }
            Storage.set(c, "tm", a.toString());
            JSONObject cfg = new JSONObject().put("l", label).put("timer", 1).put("sz", 0).put("ch", 0).put("vb", 1);
            c.startForegroundService(new Intent(c, RingService.class).putExtra("cfg", cfg.toString()));
        } catch (Exception e) { }
        scheduleAll(c);
    }

    static void action(Context c, Intent in) {
        int id = in.getIntExtra("id", 0);
        JSONArray a = load(c);
        try {
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o == null || o.optInt("id") != id) continue;
                if ("T_STOP".equals(in.getAction())) { o.put("st", "idle"); o.put("left", o.optLong("dur")); }
                else o.put("end", o.optLong("end") + 60000L);
            }
            Storage.set(c, "tm", a.toString());
        } catch (Exception e) { }
        scheduleAll(c);
    }
}
