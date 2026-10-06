# F-Droid submission (main repo)

Recipe: `de.bgghome.webtrees.nativ.yml` in this folder. Store texts and screenshots:
`fastlane/metadata/android/{en-US,de-DE}/` (F-Droid reads them from the repository at the tagged commit).

## How a release reaches F-Droid

First version in the recipe is 1.36 (versionCode 38): the 1.35 APK was built from a working tree whose version
numbers were not committed yet, so no commit reproduces it. From 1.36 on, the release APK is built from the tagged
commit with a clean tree (`tools/fdroid-reference.sh`, not in the repository), and the release build no longer
embeds `META-INF/version-control-info.textproto`.

1. Tag `vX.Y` on `main`, pushed. Tags are never moved afterwards – F-Droid builds the commit hash in the recipe.
2. The reference APK is built in the F-Droid build-server image, signed with the project key and attached to the
   GitHub release as `wtAnd-vX.Y.apk` (field `Binaries:` in the recipe). F-Droid builds the same commit, copies the
   signature over and only ships the APK if both are byte-identical – so F-Droid users get the same signature as
   GitHub users and can switch between the two without reinstalling.
3. The recipe is updated by F-Droid's checkupdates bot (`UpdateCheckMode: Tags`, version from `gradle.properties`),
   or by hand with a new `Builds:` entry (versionName, versionCode, commit hash) and `CurrentVersion`.

## Merge request text

> **New App: wtAnd (de.bgghome.webtrees.nativ)**
>
> Android client for webtrees, the free web-based genealogy software: the family tree as an hourglass view,
> person profiles with timeline, family and map, editing of events and relatives, photos and the archive of the
> server, anniversaries. The app talks to the user's own webtrees server through the module
> [api4webtrees](https://github.com/thobgg/api4webtrees) (GPL-3.0, same author). GPL-3.0-or-later.
>
> Source: https://github.com/thobgg/app4webtrees – a Kotlin/Compose Multiplatform project; the `app` module is the
> Android app, `desktop` is the Linux/Windows client built from the same `shared` module (not part of this recipe).
> Pure Kotlin, no native code, dependencies from Maven Central and Google only (Compose, OkHttp, Coil, osmdroid).
>
> Reproducible build: `Binaries:` points to the release APK on GitHub, signed with the project key
> (`AllowedAPKSigningKeys`). The APK was built in `fdroidserver:buildserver-trixie` and verified with
> `apksigcopier compare` against the unsigned build.
>
> Network: only the user's own webtrees server (address entered by the user) and OpenStreetMap tiles for the map.
> No analytics, no ads, no Google services; permissions INTERNET and POST_NOTIFICATIONS (optional daily reminder).
