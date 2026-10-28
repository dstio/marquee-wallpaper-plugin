# Privacy

Marquee makes its wallpapers on your device. It has no account, no analytics, no advertising and
no crash reporting, and it sends nothing to DStudio.

## What leaves the device

Marquee talks to one service, [TMDB](https://www.themoviedb.org), over HTTPS:

- `api.themoviedb.org`, for the lists you turn on and the details of the titles on them. A request
  carries Marquee's own TMDB key (the same for every copy of the app), the language and country
  Marquee uses for titles, and the title it asks about.
- `image.tmdb.org`, for each title's backdrop, poster and logo.

As with any website, TMDB sees your device's IP address when it answers. Its handling of that is
covered by [TMDB's privacy policy](https://www.themoviedb.org/privacy-policy). Where to stream comes
from TMDB as well, which takes it from JustWatch; Marquee does not contact JustWatch.

## What stays on the device

- Your settings.
- The pictures Marquee downloads from TMDB, kept in its cache and trimmed when the cache grows.
- The wallpapers it makes, and a note of why each title was shown or left out (the Debug page).

None of this is backed up by Android (`allowBackup` is off). Clearing Marquee's storage or
uninstalling it removes all of it.

## Who can read the wallpapers

Only a launcher that holds ATV Launcher's wallpaper permission can read the wallpapers Marquee makes.
That permission can be held only by an app signed with ATV Launcher's key, so other apps on the
device cannot read them.

## Permissions

- **Internet** and **network state**, to reach TMDB and to wait for a connection before a run.

## Contact

info@dstudio.ca
