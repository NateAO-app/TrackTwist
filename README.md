# TrackTwist

Android app that lets users spin up short music previews by genre or artist, save favorite genres, and stream 30-second previews from Deezer.

## Build

- Android Studio (Groovy Gradle), JDK 17
- compileSdk 36, targetSdk 36, minSdk 24
- Dependencies: Gson, OkHttp, AppCompat, Material, ConstraintLayout

**Steps**
1. Open in Android Studio.
2. Sync Gradle.
3. Run on an emulator or device.

## Features

- Genre spinner with Randomize to pick a track.
- Local previews from `res/raw` for offline testing.
- Artist search via Deezer with preview streaming.
- Play or Pause current selection.
- Favorites persisted with SharedPreferences.
- Loading indicator during network search.
- TLS-only networking via `network_security_config.xml`.

## Architecture

- `TrackRepository` interface abstracts data source.
    - `LocalRepository` loads `res/raw/seed_tracks.json` and returns local raw resources.
    - `DeezerRepository` queries Deezer Search API and returns preview URLs.
- `MainActivity` binds UI to a repository instance without knowing local vs remote.
- Media playback pipeline:
    - Local: resolve raw resource id, `MediaPlayer.create(context, resId)`.
    - Remote: `MediaPlayer` with `setDataSource(url)` and `prepareAsync()`.

## Data Model

- `seed_tracks.json` format:
  ```json
  {
    "genres": [
      {
        "name": "Rock",
        "tracks": [
          { "title": "Rock Demo", "artist": "Studio Demo", "raw": "personalholloway" }
        ]
      }
    ]
  }
