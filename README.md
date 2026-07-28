# LightQR

A minimalist **QR scanner** for the Light Phone III, plus a companion **text→QR generator** website.

Two pieces, one repo:

| Piece | What it is | Where |
|---|---|---|
| **LightQR** | Native Android tool for LightOS. Scan a QR → see the text → open links, keep history. Styled to match LightOS (Akkurat type, black/white, top + bottom bars). | `app/` |
| **QR Generator** | Static web page. Type/paste anything → downloadable QR code. | `docs/` → GitHub Pages |

> **Note on the Light SDK:** this is a plain, standalone Android app — *not* built on `light-sdk`. That's deliberate: it uses the standard Android camera (CameraX), the normal soft keyboard for manual entry, and no SDK API restrictions. It installs on the Light Phone III via sideload / Developer Mode ("Any tools"). The trade-off is it isn't eligible for the official Tool Library without porting into the SDK's `tool` module later.

## Scanner features

- Scan QR codes with the back camera (CameraX + **ZXing** — pure Java, works without Google Play Services, which LightOS lacks).
- **Manual entry:** paste or type a link/address with the normal keyboard instead of scanning.
- Result screen shows the decoded text; **Open link** button appears for URLs.
- **History** of everything scanned/entered, stored locally on device (no cloud, no accounts).
- Black-and-white, low-distraction UI in keeping with the Light aesthetic.

## Install on the Light Phone III

### Option A — Obtainium (recommended, auto-updates)
1. Install [Obtainium](https://github.com/ImranR98/Obtainium).
2. Add App → paste: `https://github.com/gi-os/LightQR`
3. Obtainium reads the GitHub Releases and installs the latest APK. Every push to `main` publishes a new versioned release, so updates are automatic.

### Option B — manual sideload
Download the APK from [Releases](https://github.com/gi-os/LightQR/releases) and:
```
adb install -r LightQR-vX.Y.Z.apk
```
On LightOS, set tool permissions to **"Any tools"** to launch it from the toolbox.

## The generator

Live at **https://gi-os.github.io/LightQR/** (once Pages is enabled on `/docs`).
Runs entirely in the browser — nothing is sent anywhere.

## Auto-versioned releases (for Obtainium)

`.github/workflows/release.yml` builds a **signed** APK on each push to `main`:

- `versionCode` = CI run number, `versionName` = `1.0.<run>`, tagged `vX.Y.Z`.
- Signed with a **consistent key** (`app/lightqr.keystore`) so Obtainium can upgrade in place.
- APK attached to the GitHub Release, marked `latest`.

> The signing keystore is committed with default passwords because this is a personal hobby tool — simplest path to consistent signatures. To use your own key, set repo secrets and pass them as `KEYSTORE_FILE` / `KEYSTORE_PASS` / `KEY_ALIAS` / `KEY_PASS` env vars in the workflow (the Gradle config already reads them).

## Build locally

```
gradle :app:assembleRelease
# APK → app/build/outputs/apk/release/app-release.apk
```
Requires JDK 17 + Android SDK (compileSdk 34).

## Layout
```
app/            Android scanner tool (Kotlin + Compose)
docs/           text→QR generator (GitHub Pages)
.github/workflows/release.yml   auto-build + release
```

MIT licensed.
