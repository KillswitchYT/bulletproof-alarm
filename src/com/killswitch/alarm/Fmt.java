package com.killswitch.alarm;

import android.content.Context;
import android.text.format.DateFormat;

final class Fmt {
    /** 24-hour display? Honors the in-app override ("12" or "24"), otherwise the phone setting. */
    static boolean is24(Context c) {
        String t = Storage.get(c, "tfmt").replace("\"", "");
        if (t.equals("12")) return false;
        if (t.equals("24")) return true;
        return DateFormat.is24HourFormat(c);
    }
}
