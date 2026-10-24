package com.killswitch.alarm;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

final class Perms {
    /** Order matches the UI: 0 exact alarms, 1 notifications, 2 full-screen, 3 samsung (manual), 4 battery, 5 DND, 6 sound. */
    static boolean[] check(Context c) {
        boolean[] r = new boolean[7];
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        AudioManager au = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        r[0] = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
        r[1] = nm.areNotificationsEnabled();
        r[2] = Build.VERSION.SDK_INT < 34 || nm.canUseFullScreenIntent();
        r[3] = "1".equals(Storage.get(c, "samsungOk"));
        r[4] = pm.isIgnoringBatteryOptimizations(c.getPackageName());
        r[5] = nm.getCurrentInterruptionFilter() != NotificationManager.INTERRUPTION_FILTER_NONE;
        r[6] = au.getStreamVolume(AudioManager.STREAM_ALARM) > 0 && !btOut(au);
        return r;
    }

    static boolean btOut(AudioManager au) {
        for (AudioDeviceInfo d : au.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            int t = d.getType();
            if (t == 8 || t == 26 || t == 27) return true; // A2DP, BLE headset, BLE speaker
        }
        return false;
    }

    static void go(Activity a, Intent in) {
        try { a.startActivity(in); }
        catch (Exception e) {
            a.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + a.getPackageName())));
        }
    }

    static void fix(Activity a, int i) {
        String pkg = a.getPackageName();
        Uri pk = Uri.parse("package:" + pkg);
        switch (i) {
            case 0: go(a, new Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM", pk)); break;
            case 1:
                if (Build.VERSION.SDK_INT >= 33 && !"1".equals(Storage.get(a, "askedN"))) {
                    Storage.set(a, "askedN", "1");
                    a.requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
                } else {
                    go(a, new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg));
                }
                break;
            case 2: go(a, new Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT", pk)); break;
            case 3: samsung(a); break;
            case 4: go(a, new Intent("android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS", pk)); break;
            case 5: go(a, new Intent("android.settings.ZEN_MODE_SETTINGS")); break;
            default: go(a, new Intent(Settings.ACTION_SOUND_SETTINGS));
        }
    }

    /** Android has no public API to read Samsung's "Never sleeping apps" list, so this one is confirmed by hand. */
    static void samsung(Activity a) {
        if ("1".equals(Storage.get(a, "samsungOk"))) { Storage.set(a, "samsungOk", "0"); return; }
        Storage.set(a, "samsungOk", "1");
        String[][] c = {
            {"com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"},
            {"com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"},
            {"com.samsung.android.sm_cn", "com.samsung.android.sm.ui.battery.BatteryActivity"}};
        for (String[] x : c) {
            try { a.startActivity(new Intent().setClassName(x[0], x[1])); return; } catch (Exception e) { }
        }
        go(a, new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
    }
}
