# Changelog

All notable changes to **TrackTwist** will be documented here.

---

## [Unreleased]
## [0.3.0] - 2025-08-22

### Added
* Artist search powered by Deezer API with 30 second preview streaming
* Album artwork display for remote tracks
* Share button to send the current preview link
* Loading indicator that disables all controls during network search
* Favorites persistence for genres and for remote tracks

### Changed
* Data access behind `TrackRepository` with `LocalRepository` (JSON) and `DeezerRepository` (API)
* Artist search now uses artist-only flow: match artist, then pick from that artist’s top tracks

### Fixed
* Local fallback when no artist result or on network error
* Disable every control during search to prevent double taps
* Correct raw resource name for Rock track `personalholloway`

### Data
* `seed_tracks.json` expanded to nine genres

### Security
* HTTPS only via `res/xml/network_security_config.xml`
* Internet permission added for API use

### Docs
* `README.md` with build steps, features, architecture, and roadmap

---

## [0.1.0] - 2025-08-20
### Added
- Created new Android project with Empty Activity
- Configured Gradle, JDK (Java 17), and compileSdk 36
- Added Spinner with static genres (LoFi, Rock, Pop)
- Added Randomize button to select genre
- Added Play/Pause button for audio
- Added Save Favorite and View Favorites features
- Created `res/raw` folder and added first audio file (`pompeii.mp3`)
- Integrated MediaPlayer to play/pause audio
