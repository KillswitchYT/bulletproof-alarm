package com.killswitch.alarm;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class Scheduler {
    static final long BACKUP_MS = 120000L;

    static PendingIntent pi(Context c, int code, String act, long t, String cfg, int flags) {
        Intent in = new Intent(c, AlarmReceiver.class).setAction(act)
            .putExtra("t", t).putExtra("cfg", cfg).putExtra("i", code);
        return PendingIntent.getBroadcast(c, code, in, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    static boolean hasDay(JSONArray d, int day) {
        if (d == null) return false;
        for (int i = 0; i < d.length(); i++) if (d.optInt(i, -1) == day) return true;
        return false;
    }

    /** Next ring time for one alarm (repeat days, one-time, fixed date, skip-next), or -1. */
    static long nextFor(JSONObject o) {
        long now = System.currentTimeMillis(), skip = o.optLong("sk", 0);
        String[] hm = o.optString("t", "07:00").split(":");
        int h = Integer.parseInt(hm[0]), m = Integer.parseInt(hm[1]);
        JSONArray d = o.optJSONArray("d");
        String dt = o.optString("dt", "");
        if (!dt.isEmpty()) {
            String[] p = dt.split("-");
            Calendar c = Calendar.getInstance();
            c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]), h, m, 0);
            c.set(Calendar.MILLISECOND, 0);
            long t = c.getTimeInMillis();
            return (t > now && t != skip) ? t : -1;
        }
        boolean any = d != null && d.length() > 0;
        for (int i = 0; i < 8; i++) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, i);
            c.set(Calendar.HOUR_OF_DAY, h);
            c.set(Calendar.MINUTE, m);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            long t = c.getTimeInMillis();
            if (t <= now) continue;
            if (any && !hasDay(d, c.get(Calendar.DAY_OF_WEEK) - 1)) continue;
            if (t == skip) continue;
            return t;
        }
        return -1;
    }

    static PendingIntent show(Context c) {
        return PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
    }

    static JSONArray load(Context c) {
        try {
            String s = Storage.get(c, "al");
            return new JSONArray(s.isEmpty() ? "[]" : s);
        } catch (JSONException e) { return new JSONArray(); }
    }

    static void scheduleAll(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        for (int i = 0; i < 128; i++) {
            PendingIntent p = pi(c, i, i % 2 == 0 ? "FIRE" : "BACKUP", 0, "", PendingIntent.FLAG_NO_CREATE);
            if (p != null) { am.cancel(p); p.cancel(); }
        }
        PendingIntent bp = pi(c, 9100, "BED", 0, "", PendingIntent.FLAG_NO_CREATE);
        if (bp != null) { am.cancel(bp); bp.cancel(); }
        JSONArray a = load(c);
        boolean fail = false;
        long first = 0;
        for (int i = 0; i < a.length() && i < 64; i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null || o.optInt("on", 0) == 0) continue;
            long t;
            try { t = nextFor(o); } catch (Exception e) { continue; }
            if (t < 0) continue;
            if (first == 0 || t < first) first = t;
            String cfg = o.toString();
            try {
                am.setAlarmClock(new AlarmManager.AlarmClockInfo(t, show(c)),
                    pi(c, i * 2, "FIRE", t, cfg, PendingIntent.FLAG_UPDATE_CURRENT));
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t + BACKUP_MS,
                    pi(c, i * 2 + 1, "BACKUP", t, cfg, PendingIntent.FLAG_UPDATE_CURRENT));
            } catch (SecurityException e) { fail = true; }
        }
        try {
            String bs = Storage.get(c, "bed");
            JSONObject b = new JSONObject(bs.isEmpty() ? "{}" : bs);
            if (b.optInt("on", 0) == 1 && first > 0) {
                long r = first - b.optInt("goal", 8) * 3600000L - b.optInt("wind", 30) * 60000L;
                if (r > System.currentTimeMillis())
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r, pi(c, 9100, "BED", r, "", PendingIntent.FLAG_UPDATE_CURRENT));
            }
        } catch (Exception e) { }
        Storage.set(c, "schedFail", fail ? "1" : "0");
        Storage.set(c, "nextT", String.valueOf(first));
        Timers.scheduleAll(c);
        ClockWidget.refresh(c);
    }

    static void snooze(Context c, String cfg, int secs, boolean test) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        long t = System.currentTimeMillis() + secs * 1000L;
        Intent in = new Intent(c, AlarmReceiver.class).setAction("FIRE")
            .putExtra("t", t).putExtra("cfg", cfg).putExtra("i", 9000).putExtra("test", test);
        PendingIntent p = PendingIntent.getBroadcast(c, 9000, in,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(t, show(c)), p);
        } catch (SecurityException e) {
            am.set(AlarmManager.RTC_WAKEUP, t, p);
        }
    }

    static void bedNotify(Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel("bed", "Bedtime", NotificationManager.IMPORTANCE_DEFAULT));
        int w = 30;
        try { w = new JSONObject(Storage.get(c, "bed")).optInt("wind", 30); } catch (Exception e) { }
        nm.notify(8, new Notification.Builder(c, "bed").setSmallIcon(R.drawable.ic_small)
            .setContentTitle("Time to wind down")
            .setContentText("Bedtime in " + w + " min to reach your sleep goal")
            .setAutoCancel(true).setContentIntent(show(c)).build());
    }
}
