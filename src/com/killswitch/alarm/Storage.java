package com.killswitch.alarm;

import android.content.Context;
import android.content.SharedPreferences;

final class Storage {
    static SharedPreferences p(Context c) {
        Context d = c.isDeviceProtectedStorage() ? c : c.createDeviceProtectedStorageContext();
        return d.getSharedPreferences("s", 0);
    }
    static String get(Context c, String k) { return p(c).getString(k, ""); }
    static void set(Context c, String k, String v) { p(c).edit().putString(k, v).commit(); }
}
