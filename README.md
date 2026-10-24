# Bulletproof Alarm

A private, reliable alarm clock for Android. **No internet permission, no ads, no accounts, no tracking.**
Everything stays on your phone.

> Status: beta. If an alarm ever fails, please open an issue. Reliability is the whole point.

## Why

Most alarm apps fail silently or collect your data. Bulletproof Alarm is built around two promises:

1. **It rings.** Alarms use Android's alarm-clock scheduling, a backup alarm rings 2 minutes later only if the main one did not, alarms are rebuilt after a reboot, and the app checks every permission on **every launch**.
2. **It keeps quiet about you.** The manifest has no `INTERNET` permission, so the app physically cannot send anything anywhere.

## Features

- **Alarms:** repeat days, one-time, specific date, skip next ring, labels, three bundled tones or any phone ringtone, vibrate, gradual volume, snooze length (or off), wake challenge (math or shake), volume-button action
- **Timer** with multiple timers, presets and a running notification (+1 min, Stop)
- **Stopwatch** with laps (best and worst highlighted) that keeps counting when you leave the app
- **World clock** with an offline search by city, country or region, analog or digital, and a time converter
- **Bedtime reminder** (no sleep tracking) and a dim **night clock**
- **Home-screen widget**, voice-assistant hooks (`SET_ALARM`, `SET_TIMER`, `SHOW_ALARMS`), local backup and restore
- **12h / 24h** follows your phone, with an override
- Six visual styles (Liquid Glass, Glassmorphism, Claymorphism, Neumorphism, Neobrutalism, Aurora), each in light and dark

## Install

1. Open the [Releases](../../releases) page and download the latest `BulletproofAlarm-*.apk`.
2. Open it on your phone. Android will ask you to allow installs from your browser or file manager. Play Protect may warn about an unknown app, which is normal for apps installed outside the Play Store.
3. Open the app. The permission check runs first. Tap each unchecked row and allow it.

For reliable alarms, also set these (the in-app checks guide you):
exact alarms, notifications, full-screen alarm, battery **Unrestricted**, alarms allowed in Do Not Disturb, alarm volume up and no Bluetooth device connected. On Samsung, add the app to **Never sleeping apps** (Android cannot read that list, so the app asks you to confirm it manually).

## Build from source

No Gradle or Android Studio needed.

**On your phone (Termux, installed from F-Droid or GitHub, not the Play Store):**

```sh
pkg update
pkg install openjdk-17 aapt2 d8 apksigner zipalign zip unzip curl git
git clone https://github.com/KillswitchYT/bulletproof-alarm.git && cd bulletproof-alarm
bash get-jar.sh     # one time: downloads the API 34 android.jar
bash build.sh       # creates BulletproofAlarm.apk, signed with a local debug key
termux-open BulletproofAlarm.apk
```

**On GitHub:** every push builds the app (Actions tab). Pushing a tag like `v2.1.0` builds, signs with your release key and publishes a Release. See [docs/RELEASING.md](docs/RELEASING.md).

How it is built: a small native Java app (alarm engine, services, receivers) hosts the UI, a single HTML file (`assets/index.html`), inside a WebView loaded from the APK's own assets. The page talks to the engine through a tiny JavaScript bridge. Source is in `src/com/killswitch/alarm/`.

## Testing

See [TESTING.md](TESTING.md) for a step-by-step checklist of what to try and what a pass or fail looks like.

## Privacy

- No `INTERNET` permission. No analytics, ads, accounts or cloud.
- Alarms, timers and settings are stored only on your device. Backup is copy and paste.
- Fonts and icons are bundled inside the app.


## Contributing

Bug reports with your phone model and Android version are the most useful thing. Pull requests are welcome.

## License

GNU GPL v3. See [LICENSE](LICENSE). Fonts and icons carry their own licenses, listed in [THIRD_PARTY.md](THIRD_PARTY.md).
Copyright (C) 2026 Killswitch.
