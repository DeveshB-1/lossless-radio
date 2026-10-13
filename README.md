# Lossless Radio

CD-quality internet radio for Android. Finds FLAC stations from around the world, plays any stream URL, and shows you
exactly what happens to the audio on its way to your ears.

- **Discover:** ~80 lossless (FLAC / HLS FLAC) stations from the [radio-browser.info](https://www.radio-browser.info)
  community directory, plus search across 50,000+ stations.
- **Library:** favourites and recently played; add any stream by URL (direct, `.pls`, `.m3u`, HLS).
- **Player:** live song info when the station sends it, sleep timer, background playback with media controls,
  automatic reconnects, clear messages for offline stations.
- **Signal path:** real decoded codec / sample rate / bit depth, output device, and an honest lossless verdict.
- **Bit-perfect USB** on Android 14+.
- Optional Google Play tip jar. No ads, no accounts, no tracking.

## Install

Download `LosslessRadio.apk` from [Releases](../../releases). Android 10+.

## Build

```sh
./gradlew testReleaseUnitTest assembleRelease   # JDK 17–21, Android SDK 36
```

Release signing reads `keystore.properties` or `KEYSTORE_FILE` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`,
falling back to the debug key. CI attaches the APK and AAB to `v*` tag releases.
