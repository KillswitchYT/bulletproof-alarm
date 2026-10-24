package com.killswitch.alarm;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.widget.RemoteViews;

public class ClockWidget extends AppWidgetProvider {
    static RemoteViews views(Context c) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget);
        long t = 0;
        try { t = Long.parseLong(Storage.get(c, "nextT")); } catch (Exception e) { }
        boolean h24 = Fmt.is24(c);
        v.setCharSequence(R.id.wc, "setFormat12Hour", h24 ? "HH:mm" : "h:mm");
        v.setCharSequence(R.id.wc, "setFormat24Hour", h24 ? "HH:mm" : "h:mm");
        v.setTextViewText(R.id.wn, t > 0 ? "Next alarm  " + DateFormat.format(h24 ? "EEE HH:mm" : "EEE h:mm a", t) : "No alarm set");
        v.setOnClickPendingIntent(R.id.wr, PendingIntent.getActivity(c, 5,
            new Intent(c, MainActivity.class), PendingIntent.FLAG_IMMUTABLE));
        return v;
    }

    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, views(c));
    }

    static void refresh(Context c) {
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            for (int id : m.getAppWidgetIds(new ComponentName(c, ClockWidget.class))) m.updateAppWidget(id, views(c));
        } catch (Exception e) { }
    }
}
