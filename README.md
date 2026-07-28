# LightQR

A QR scanner for the Light Phone III, and a companion web page that turns text into a
QR code.

Part of the [gi-os Light App collection](#the-gi-os-light-app-collection).

| Piece | What it is | Where |
| --- | --- | --- |
| **LightQR** | Android tool. Scan a code, read the text, open the link, keep a history. | `app/` |
| **QR Generator** | Static web page. Type anything, download the code. | `docs/`, served by GitHub Pages |

## What the scanner does

- Reads QR codes with the back camera. CameraX supplies the frames and ZXing decodes
  them off the luminance plane. ZXing is pure Java, so it runs on LightOS, which has no
  Google Play Services.
- Takes a typed or pasted address instead, when a camera is the wrong tool.
- Shows the decoded text on a result screen. An **Open link** button appears for a URL.
- Keeps a local history of every scan and entry. There is no cloud and no account.
- Uses black on white, Akkurat type, and the LightOS top and bottom bars.

## The generator

[gi-os.github.io/LightQR](https://gi-os.github.io/LightQR/) encodes text in the browser.
Nothing leaves the page. Use it to hand a long address to the phone without typing it.

## Install

### Obtainium, with automatic updates

1. Install [Obtainium](https://github.com/ImranR98/Obtainium).
2. Add `https://github.com/gi-os/LightQR` as an app.

Every push to `main` publishes a new versioned release, so Obtainium sees each update.

### Manual sideload

Download the APK from [Releases](https://github.com/gi-os/LightQR/releases/latest) and
run:

```sh
adb install -r LightQR-vX.Y.Z.apk
```

On LightOS, set tool permissions to **Any tools** to launch it from the toolbox.

## Releases and signing

`.github/workflows/release.yml` builds a signed APK on each push to `main`.

- `versionCode` is the CI run number. `versionName` is `1.0.<run>`. The workflow tags
  the commit `vX.Y.Z`.
- The build signs with a fixed key, `app/lightqr.keystore`, so Obtainium can upgrade in
  place.
- The workflow attaches the APK to the release and marks it latest.

The keystore sits in the repo with default passwords, because this is a personal tool
and a stable signature matters more here than a secret. To sign with your own key, set
the repo secrets and pass them to the workflow as `KEYSTORE_FILE`, `KEYSTORE_PASS`,
`KEY_ALIAS` and `KEY_PASS`. The Gradle config already reads those.

## Build

```sh
gradle :app:assembleRelease
```

You need JDK 17 and the Android SDK, with `compileSdk` 34. The APK lands in
`app/build/outputs/apk/release/`.

## A note on the SDK

LightQR is a plain Android app. It does not use
[light-sdk](https://github.com/lightphone/light-sdk). The trade is deliberate. CameraX
and the standard soft keyboard work without an API allowlist, but the tool cannot enter
the official Tool Library until someone ports it into the SDK `tool` module.

## Origin and credits

- **[gi-os/LightPass](https://github.com/gi-os/LightPass)** is where this started. The
  QR key scanner inside LightPass became a tool of its own, and LightQR keeps its
  CameraX setup and its Akkurat type handling.
- **[ZXing](https://github.com/zxing/zxing)** by the ZXing authors does the decoding.
  Nothing else in this space works as well without Play Services. Thank you.
- **[The Light Phone](https://www.thelightphone.com/)** for LightOS and for opening the
  platform to community tools.
- **[Obtainium](https://github.com/ImranR98/Obtainium)** by ImranR98 handles updates, so
  this repo does not need a store.
- CameraX and Jetpack Compose do the rest.

Two later tools took from LightQR. [LightRSS](https://github.com/gi-os/LightRSS) copied
the `docs/` generator page to make its feed QR codes.
[LightFog](https://github.com/gi-os/LightFog) copied the release workflow, because plain
Gradle in GitHub Actions beats an EAS build for a single-developer project.

## The gi-os Light App collection

Eight tools for the Light Phone III, all open source, all built in one run.

| Tool | What it does | Built on |
| --- | --- | --- |
| [LightPass](https://github.com/gi-os/LightPass) | Photograph a movie ticket, keep the stub | Plain Android |
| **LightQR** (this repo) | QR scanner, plus a browser generator | Plain Android |
| [LightRSS](https://github.com/gi-os/LightRSS) | RSS and Atom reader with images and QR subscribe | light-sdk, fork of [zachattack323/LightRSS](https://github.com/zachattack323/LightRSS) |
| [LightNYCSubway](https://github.com/gi-os/LightNYCSubway) | Live MTA subway arrivals | light-sdk fork |
| [chat](https://github.com/gi-os/chat) | iMessage over a self-hosted BlueBubbles server | Fork of [craigeley/chat](https://github.com/craigeley/chat) |
| [LightFog](https://github.com/gi-os/LightFog) | Fog of World companion, GPS recorder and fog map | Expo, [vandamd/light-template](https://github.com/vandamd/light-template) |
| [LightNonogram](https://github.com/gi-os/LightNonogram) | Picross, plus a generator that only ships solvable puzzles | Kotlin generator, light-sdk tool |
| [LightSolitaire](https://github.com/gi-os/LightSolitaire) | Klondike, draw one, unlimited redeals | light-sdk |

The Light Phone does not sponsor or endorse any of these.

## License

MIT. See [LICENSE](LICENSE).
