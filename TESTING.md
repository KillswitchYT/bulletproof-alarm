# Test plan (v2.0). Report failures as: test number + what you saw.

0. LAUNCH CHECK. Open app. PASS: popup scans 7 rows in ~1s, shows real states, closes itself if all granted.
   Revoke notifications in Android settings, reopen: that row must be unchecked and the popup must stay open.
1. ONE-TIME ALARM. New alarm, 2 min ahead, no days. PASS: appears in list, Next alarm card counts down,
   status bar shows the alarm icon. Lock phone. At the time: screen wakes, ring screen, sound + vibration.
   Dismiss stops it and the alarm switch turns itself OFF.
2. REPEAT DAYS. Alarm 2 min ahead with only today's weekday selected. PASS: rings, stays ON,
   Next alarm now says about 6d 23h.
3. SNOOZE. Alarm with Snooze 1 min. PASS: Snooze button says "Snooze 1 min", rings again after ~1 min.
   Set Snooze Off: the button must not exist.
4. SKIP NEXT. Edit a repeating alarm, Skip next. PASS: row says "skipping next", Next alarm jumps to the next day, no ring today.
5. CHALLENGE. Alarm with Math. PASS: Dismiss shows 3 sums, wrong answer shakes the box, notification has no Dismiss button.
   Shake challenge: bar fills after 25 shakes.
6. SOUNDS. Edit alarm, pick each tone, tap Test. PASS: plays that tone. Gradual volume: starts silent, loud after ~30 s.
   "Pick from phone..." chooses a system ringtone and the name appears in the list.
7. VOLUME BUTTONS. Settings > While ringing > Snooze. PASS: pressing a volume key during a ring snoozes it.
8. REBOOT. Alarm 5 min ahead, restart phone, do NOT open the app. PASS: still rings.
9. TIMER. Preset 1 min. PASS: notification with countdown + Stop / +1 min, rings at 0 with "Timer" and no Snooze,
   keeps running if you close the app. Pause, Resume, Reset and +1:00 work in-app.
10. STOPWATCH. Start, Lap x3, leave the app 10 s, return while running. PASS: time kept counting, fastest lap green,
    slowest red, Copy works, Reset clears.
11. WORLD CLOCK. Add Tokyo and Cairo. PASS: offsets are correct, Analog switch draws clock faces, converter updates.
12. BEDTIME. Alarm 7 h ahead, goal 6 h, wind down 45 min, switch on. PASS: notification "Time to wind down" in ~15 min.
13. NIGHT CLOCK. Settings > Night clock. PASS: black screen, dim red clock that drifts, screen stays on, tap exits
    and brightness returns.
14. WIDGET. Home screen > Widgets > Bulletproof Alarm. PASS: shows time and next alarm; changes after you edit alarms.
15. VOICE. "Set an alarm for 7 am" may land here or in another clock app (Android decides). PASS if it appears in the list.
16. BACKUP. Copy backup, delete an alarm, paste backup, Restore. PASS: alarm returns.
17. TIME FORMAT. Settings > Time format. PASS: "Phone setting" matches your phone (12h shows AM/PM, 24h does not) on the
    home clock, alarm list, next alarm, ring screen, world clock and widget. 12-hour / 24-hour override it instantly.
18. CITY SEARCH. World > Add city, type "egypt", "usa", "asia", "tokyo". PASS: countries, aliases, regions and cities all find results with their GMT offset.
19. CONVERTER. Open World. PASS: the converter shows your current time and keeps following the clock; edit it and it stops; "Now" snaps it back.
20. SELF-TEST BAR. Settings tab. PASS: one compact pill, text on one line, round Run self-test button that fits the pill.
