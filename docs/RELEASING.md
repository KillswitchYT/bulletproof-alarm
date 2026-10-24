# Releasing

## One-time setup: make your release signing key
Do this once and **never lose or share the key**. If you lose it, nobody can update the app.

```sh
keytool -genkeypair -v -keystore release.jks -alias bulletproof -keyalg RSA -keysize 4096 -validity 10000
```
Back up `release.jks` and its passwords somewhere safe that is NOT this repository (the `.gitignore` already blocks `*.jks`).

Turn it into text for GitHub:

```sh
base64 -w0 release.jks > release.b64
```

On GitHub open **Settings > Secrets and variables > Actions > New repository secret** and add:

| Secret | Value |
|---|---|
| `KEYSTORE_B64` | the contents of `release.b64` |
| `KS_PASS` | the keystore password |
| `KEY_ALIAS` | `bulletproof` (the alias you chose) |

Then delete `release.b64` from the phone.

## Every release
1. Update `CHANGELOG.md`.
2. Commit and push to `main`.
3. Tag and push:
   ```sh
   git tag v2.2.0
   git push origin v2.2.0
   ```
4. Open the **Actions** tab and wait for the green check. The APK appears on the **Releases** page.
5. Install it on your own phone and run the key tests in `TESTING.md` before telling anyone.

## Notes
- The version number comes from the tag (`v2.2.0` becomes 2.2.0). The build number rises automatically.
- Do not change the package name (`com.killswitch.alarm`) after people install the app.
- Only the release key can update an installed release. Debug-signed builds from your phone will not install over it, so uninstall first when switching.
