package com.killswitch.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.json.JSONObject;

public class RingService extends Service {
    MediaPlayer mp;
    Ringtone rt;
    Vibrator vib;
    PowerManager.WakeLock wl;
    AudioManager au;
    final Handler h = new Handler(Looper.getMainLooper());
    JSONObject cfg = new JSONObject();
    String cfgStr = "{}", label = "Alarm";
    boolean test, ringing, timer;
    int snoozeMin = 5, ch = 0, origVol = -1;
    long t0;

    final Runnable ramp = new Runnable() {
        public void run() {
            if (mp == null) return;
            float v = Math.min(1f, (System.currentTimeMillis() - t0) / 30000f);
            try { mp.setVolume(v, v); } catch (Exception e) { }
            if (v < 1f) h.postDelayed(this, 500);
        }
    };
    final Runnable auto = new Runnable() {
        public void run() { Storage.set(RingService.this, "autoStop", "1"); stopRing(); }
    };

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public int onStartCommand(Intent in, int fl, int id) {
        String act = in == null ? null : in.getAction();
        if ("DISMISS".equals(act)) { stopRing(); return START_NOT_STICKY; }
        if ("SNOOZE".equals(act)) {
            int secs = test ? 10 : snoozeMin * 60;
            if (secs <= 0 || timer) return START_NOT_STICKY;
            Scheduler.snooze(this, cfgStr, secs, test);
            stopRing();
            return START_NOT_STICKY;
        }
        String s = in == null ? null : in.getStringExtra("cfg");
        cfgStr = s == null ? "{}" : s;
        try { cfg = new JSONObject(cfgStr); } catch (Exception e) { cfg = new JSONObject(); }
        timer = cfg.optInt("timer", 0) == 1;
        label = cfg.optString("l", "");
        if (label.isEmpty()) label = timer ? "Timer" : "Alarm";
        snoozeMin = cfg.optInt("sz", 5);
        ch = cfg.optInt("ch", 0);
        test = in != null && in.getBooleanExtra("test", false);
        foreground();
        if (ringing) return START_NOT_STICKY;
        ringing = true;
        t0 = System.currentTimeMillis();
        sound();
        Storage.set(this, "lastFired", new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date()));
        h.postDelayed(auto, 600000L);
        return START_NOT_STICKY;
    }

    PendingIntent svc(String act, int code) {
        return PendingIntent.getService(this, code, new Intent(this, RingService.class).setAction(act),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    void foreground() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel nc = new NotificationChannel("ring", "Alarm ringing", NotificationManager.IMPORTANCE_HIGH);
        nc.setSound(null, null);
        nc.enableVibration(false);
        nc.setBypassDnd(true);
        nm.createNotificationChannel(nc);
        Intent ra = new Intent(this, RingActivity.class).putExtra("l", label).putExtra("test", test)
            .putExtra("sz", snoozeMin).putExtra("ch", ch).putExtra("timer", timer ? 1 : 0)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent full = PendingIntent.getActivity(this, 1, ra,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = new Notification.Builder(this, "ring")
            .setSmallIcon(R.drawable.ic_small).setContentTitle(label)
            .setContentText(timer ? "Timer finished" : "Alarm ringing")
            .setCategory(Notification.CATEGORY_ALARM).setOngoing(true)
            .setFullScreenIntent(full, true).setContentIntent(full);
        if ((snoozeMin > 0 || test) && !timer) b.addAction(0, "Snooze", svc("SNOOZE", 2));
        if (ch == 0) b.addAction(0, "Dismiss", svc("DISMISS", 3));
        startForeground(7, b.build(), 2); // 2 = FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        try { startActivity(ra); } catch (Exception e) { }
    }

    Uri soundUri(String snd) {
        try {
            if (snd.startsWith("raw:")) {
                int id = getResources().getIdentifier("tone" + snd.substring(4), "raw", getPackageName());
                if (id != 0) return Uri.parse("android.resource://" + getPackageName() + "/" + id);
            } else if (!snd.isEmpty()) return Uri.parse(snd);
        } catch (Exception e) { }
        return defUri();
    }

    Uri defUri() {
        Uri u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        return u != null ? u : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
    }

    void sound() {
        au = (AudioManager) getSystemService(AUDIO_SERVICE);
        String ro = Storage.get(this, "ro");
        boolean gr = cfg.has("gr") ? cfg.optInt("gr") == 1 : ro.equals("0");
        int max = au.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        origVol = au.getStreamVolume(AudioManager.STREAM_ALARM);
        try {
            if (ro.equals("2")) au.setStreamVolume(AudioManager.STREAM_ALARM, max, 0);
            else if (origVol == 0) au.setStreamVolume(AudioManager.STREAM_ALARM, Math.max(1, max / 2), 0);
        } catch (Exception e) { }
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "bulletproof:ring");
        wl.acquire(600000L);
        AudioAttributes aa = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        Uri u = soundUri(cfg.optString("snd", ""));
        try {
            mp = new MediaPlayer();
            mp.setAudioAttributes(aa);
            mp.setDataSource(this, u);
            mp.setLooping(true);
            mp.prepare();
            if (gr) { mp.setVolume(0f, 0f); h.post(ramp); }
            mp.start();
        } catch (Exception e) {
            Storage.set(this, "soundErr", String.valueOf(e));
            try {
                try { if (mp != null) mp.release(); } catch (Exception x) { }
                mp = null;
                rt = RingtoneManager.getRingtone(this, u);
                rt.setAudioAttributes(aa);
                rt.setLooping(true);
                rt.play();
                if (!rt.isPlaying()) {
                    rt = RingtoneManager.getRingtone(this, defUri());
                    rt.setAudioAttributes(aa);
                    rt.setLooping(true);
                    rt.play();
                }
            } catch (Exception e2) { }
        }
        vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (cfg.optInt("vb", 1) == 1 && vib != null && vib.hasVibrator()) {
            vib.vibrate(VibrationEffect.createWaveform(new long[]{0, 700, 500}, 0),
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
        }
    }

    void stopRing() {
        h.removeCallbacksAndMessages(null);
        try { if (mp != null) { mp.stop(); mp.release(); } } catch (Exception e) { }
        mp = null;
        try { if (rt != null) rt.stop(); } catch (Exception e) { }
        rt = null;
        if (vib != null) vib.cancel();
        if (au != null && origVol >= 0) {
            try { au.setStreamVolume(AudioManager.STREAM_ALARM, origVol, 0); } catch (Exception e) { }
        }
        if (wl != null && wl.isHeld()) wl.release();
        ringing = false;
        if (RingActivity.cur != null) RingActivity.cur.finish();
        stopForeground(true);
        stopSelf();
    }
}
