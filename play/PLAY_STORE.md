# Play Store submission checklist

| Play Console field | Use |
| --- | --- |
| App bundle | `LosslessRadio.aab` from the latest GitHub release |
| App name | `listing/title.txt` |
| Short / full description | `listing/short_description.txt`, `listing/full_description.txt` |
| Release notes | `listing/release_notes.txt` |
| App icon (512×512) | `icon-512.png` |
| Feature graphic (1024×500) | `feature-graphic.png` |
| Phone screenshots | `screenshots/` (station logos belong to their stations — consider retaking with your own favourites) |
| Privacy policy URL | https://deveshb-1.github.io/lossless-radio/privacy.html |
| Category | Music & Audio |
| Contains ads / in-app purchases | No / Yes (optional tips) |

## App content answers

- **Data safety:** no data collected or shared by the developer. Station plays are reported anonymously to
  radio-browser.info (no identifiers). Encrypted in transit: mostly (some stations only offer HTTP streams).
- **Foreground service declaration:** `mediaPlayback` — "Plays user-selected internet radio in the background with a
  media notification." Attach a short screen recording.
- **Content rating:** music streaming app; stations are third-party live radio, so answer that users may encounter
  unmoderated third-party audio content (lyrics).
- **Target audience:** 18+.

## Tip jar

Same as the other apps: create one-time products `tip_small`, `tip_medium`, `tip_large` under Monetize → Products
after uploading the first AAB, and activate them.
