# Elite Memo Pro

Elite Memo Pro is an Android note-taking app written in Kotlin with a Jetpack Compose interface. It stores notes and labels in a local Room database.

## Features

- Create and edit text notes, including checklist items.
- Create freehand drawing notes with pen, marker, highlighter, selection, and eraser tools. Drawing notes support undo and redo, selectable canvas backgrounds, and blank, ruled, grid, or dotted guides.
- Search active notes by title, text, or tags; filter by note color or tag.
- Pin notes and switch between grid and list layouts.
- Organize notes with labels. Add, rename, and delete labels, and apply labels to notes.
- Archive notes and restore them later; move notes to Trash, restore them, or permanently delete them.
- Set a scheduled reminder for a note. When it fires, the app posts a notification that can open the note.
- Choose light, dark, or system appearance, and optionally require biometric or device credential authentication when opening the app.
- View counts for active notes, pinned notes, and words in Settings.

## Project structure

This is a Gradle project with three Android modules:

- `:app` — the Android application and the current implementation: Compose screens and components, note and label domain models and use cases, Room database/DAOs and repository, and WorkManager reminder code.
- `:core` — Android library module included by the app. It currently contains its Gradle configuration and manifest, with no Kotlin source files.
- `:features` — Android library module included by the app. It currently contains its Gradle configuration and manifest, with no Kotlin source files.

Within `:app`, Kotlin code is organized under `com.deepanjanxyz.notepad` into `data` (Room persistence and repository), `domain` (models, repository contract, filtering and use cases), `ui` (Compose screens, components, theme, and `NotesViewModel`), and `worker` (note reminder scheduling and notifications). `MainActivity` connects the screen state, view model, and navigation between the app's screens.

## Technology

- Kotlin and Android Gradle Plugin, with Java 17 compile options.
- Jetpack Compose UI and Material 3, using the Compose BOM declared by the project.
- AndroidX Activity Compose, Lifecycle, Core KTX, and Fragment KTX.
- Room 2.8.5 for local note and label storage; KSP runs Room's compiler.
- WorkManager 2.11.2 for scheduled note reminders.
- AndroidX Biometric 1.4.0-alpha07 for the optional app lock.
- JUnit 4.13.2 is declared for unit tests.

## Build setup

Requirements: JDK 17 and the Android SDK components required by the project (`compileSdk` 37). The Gradle wrapper is included.

The application Gradle script validates release signing configuration while configuring the project, including for local debug tasks. Provide a regular keystore at `app/keystore.jks` and these values through `app/.env`, `app/local.properties`, or matching environment variables:

- `NOTEPAD_STORE_PASSWORD`
- `NOTEPAD_KEY_ALIAS`
- `NOTEPAD_KEY_PASSWORD`

The same signing values are required for all Gradle tasks that configure `:app`. Keep signing credentials private.

From the repository root, build the debug APK with:

```sh
./gradlew assembleDebug
```

The APK is written under `app/build/outputs/apk/debug/`. To install it on a connected device or emulator:

```sh
./gradlew installDebug
```

The application module uses `applicationId` `com.deepanjanxyz.notepad`, `minSdk` 24, `targetSdk` 37, and currently declares version name `1.0.11` (version code `11`).

## License

See [LICENSE](LICENSE).
