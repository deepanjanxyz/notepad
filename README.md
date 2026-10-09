![Elite Memo Pro](fastlane/metadata/android/en-US/images/featureGraphic.png)

# Elite Memo Pro

*A local-first Android notes app — text, checklists, and freehand drawing — built with Kotlin and Jetpack Compose.*

## Overview

Elite Memo Pro brings text notes and freehand drawings together in one app. Write notes with titles and checklists, sketch on a canvas with pen, marker, highlighter, selection, and eraser tools, and keep everything organised with labels, colours, pins, search, reminders, and a built-in archive and trash.

The app is **local-first**: notes, labels, and settings live in an on-device Room database and DataStore. No account and no server are required to use it.

## Features

**Notes**
- Rich text notes with a title and body.
- Inline checklists with per-item completion state.
- Per-note colour and pinning.

**Drawing**
- Freehand drawing notes with pen, marker, highlighter, selection, and eraser tools.
- Eraser supports both segment and whole-stroke modes.
- Undo and redo for drawing edits.
- Selectable canvas backgrounds with blank, ruled, grid, and dotted guides.
- A colour palette sheet for stroke colours.

**Organise**
- Labels: add, rename, delete, and apply labels to notes.
- Search across note titles, body text, and tags; filter by label or colour.
- Grid and list layouts on the home screen.
- Archive to set notes aside, and Trash with restore or permanent delete (plus empty-trash).

**Reminders & security**
- Schedule a reminder per note; when it fires, WorkManager posts a notification that opens the note.
- Light, dark, or system appearance.
- Optional app lock using biometrics or the device credential.

**Extras**
- Counts for active notes, pinned notes, and words in Settings.
- Edge-to-edge Material 3 interface.

## Screenshots

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/01-home-grid-dark.png" width="180" alt="Home (grid)"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/05-note-editor-dark.png" width="180" alt="Note editor"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/09-drawing-note-dark.png" width="180" alt="Drawing note"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/12-settings-dark.png" width="180" alt="Settings"/>
</p>

<details>
<summary>View all screenshots</summary>

| Screen | Preview |
|---|---|
| Home (grid) | ![](fastlane/metadata/android/en-US/images/phoneScreenshots/01-home-grid-dark.png) |
| Home (list) | ![](fastlane/metadata/android/en-US/images/phoneScreenshots/02-home-list-dark.png) |
| Search results | ![](fastlane/metadata/android/en-US/images/phoneScreenshots/03-search-results-dark.png) |
| Navigation drawer | ![](fastlane/metadata/android/en-US/images/phoneScreenshots/04-navigation-drawer-dark.png) |
| Note editor | ![](fastlane/metadata/android/en-US/images/phoneScreenshots/05-note-editor-dark.png) |
| Labels sheet | ![](fastlane/metadata/android/en-US/images/phoneScreenshots/06-labels-sheet-dark.png) |
| Colour picker | ![](fastlane/metadata/android/en-US/images/phoneScreenshots/07-color-picker-dark.png) |
| Checklist editor | ![](fastlane/metadata/android/en-US/images/phoneScreenshots/08-checklist-editor-dark.png) |
| Drawing note | ![](fastlane/metadata/android/en-US/images/phoneScreenshots/09-drawing-note-dark.png) |
| Archive | ![](fastlane/metadata/android/en-US/images/phoneScreenshots/10-archive-dark.png) |
| Trash | ![](fastlane/metadata/android/en-US/images/phoneScreenshots/11-trash-dark.png) |
| Settings | ![](fastlane/metadata/android/en-US/images/phoneScreenshots/12-settings-dark.png) |
| Reminder dialog | ![](fastlane/metadata/android/en-US/images/phoneScreenshots/13-reminder-dialog-dark.png) |
| Reminder applied | ![](fastlane/metadata/android/en-US/images/phoneScreenshots/14-reminder-applied-dark.png) |

</details>

## Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose with Material 3 (Compose BOM `2026.08.00`) |
| Local storage | Room (notes & labels) and DataStore Preferences (settings) |
| Background work | WorkManager (note reminders) |
| Security | AndroidX Biometric (optional app lock) |
| Async | Kotlin Coroutines and Flow |
| Remote foundation | Supabase (`gotrue-kt` / `postgrest-kt`) over Ktor |
| Testing | JUnit 4 |

`compileSdk` / `targetSdk` are **37**, `minSdk` is **24**.

## Build & release

Elite Memo Pro is a standard Gradle project built with Kotlin and the Android Gradle Plugin against **JDK 17**. Debug builds need no secrets. Release builds are signed from a keystore at `app/keystore.jks` together with the `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` values, supplied through `app/.env`, `local.properties`, or matching environment variables. A release build without them fails fast rather than producing an unsigned artifact, and none of these files are committed.

Versions follow **Semantic Versioning** (`X.Y.Z`) with a `versionCode` that increments by one per release. `dev` is the integration branch and `main` is the release branch. Pull requests into `main` that change application code receive an automatic patch bump, and merging to `main` publishes a tagged GitHub release (`v<versionName>`) with a signed APK and SHA-256 checksums. Continuous integration runs the unit tests and a debug build on every push and pull request, and a per-pull-request check adds lint, static analysis, and a secret scan, reported back as a single status table.

## Contributing

Contributions are welcome.

1. Fork the repository and create a branch off `dev`.
2. Make your change and make sure the unit tests pass.
3. Open a pull request **against `dev`** — the Universal PR Check runs automatically and reports the result on the pull request.
4. Once verified, changes flow `dev` → `main`, where the release pipeline handles versioning and publishing.

A few conventions:

- Keep the domain module free of Android dependencies.
- Prefer one focused use case per operation.
- Don't commit keystores, `.env`, or `local.properties`.

## License

Released under the **MIT License**. See [LICENSE](LICENSE).

```
Copyright (c) 2026 Deepanjan Biswas
```

---

<p align="center"><sub>Built with Kotlin and Jetpack Compose.</sub></p>
