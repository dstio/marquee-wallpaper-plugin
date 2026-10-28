# Marquee

**Made for [ATV Launcher](https://atvlauncher.com)** ([Google Play](https://play.google.com/store/apps/details?id=ca.dstudio.atvlauncher.pro)).
Marquee is a wallpaper plugin: it shows nothing on its own, and its wallpapers appear through ATV
Launcher's Wallpaper settings.

It turns the home screen into a rotating showcase of films and TV shows. Marquee builds the
wallpapers on the device: each title's own backdrop with its logo, facts, rating, a short synopsis
and where it streams, from lists you choose.

- **Lists:** TMDB's Trending this week, In cinemas, Popular films, Upcoming, Top rated shows and
  On the air. No account needed.
- **Filters:** release date, genres, adult titles, no repeats, and titles with a logo or synopsis in
  your language.
- **Looks:** Cinematic, Minimal, Poster wall and Clean, with the text at the top left, top right or
  bottom left.
- **Updates:** new wallpapers every 6 hours to every week, or on demand.
- **Settings** open from the launcher's wallpaper page, over the live wallpaper, and a change shows
  on the home screen as soon as they close.

Marquee talks to the launcher through the
[ATV Launcher Wallpaper API](https://github.com/dstio/wallpaper-api).

## Installing

1. Install ATV Launcher from [Google Play](https://play.google.com/store/apps/details?id=ca.dstudio.atvlauncher.pro).
2. Download `marquee-<version>.apk` from this repository's
   [Releases](https://github.com/dstio/marquee-wallpaper-plugin/releases) and install it. Each
   release lists the APK's SHA-256, so you can check the download.
3. In ATV Launcher, open Settings > Wallpaper, choose Marquee under Plugins and turn it on.

Marquee asks for nothing but internet access. What it sends where is in [PRIVACY.md](PRIVACY.md).

## Building

You need JDK 17 or newer and the Android SDK (platform 36).

Marquee reads titles from [TMDB](https://www.themoviedb.org). Create a free TMDB account, request an
API key (Settings > API), and put its **API Read Access Token** in `local.properties` at the project
root:

```
tmdb.token=<your read access token>
```

`local.properties` is not committed. Without a token, a debug build shows a set of sample titles so
the app can still be tried; a release build makes no wallpapers.

```
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## Credits

This product uses the TMDB API but is not endorsed or certified by TMDB. Streaming availability is
provided by JustWatch. Fonts: Jost and Fraunces, SIL Open Font License 1.1.

## Licence

Apache License 2.0 - see [LICENSE](LICENSE) and [NOTICE](NOTICE).

Copyright 2026 DStudio.
