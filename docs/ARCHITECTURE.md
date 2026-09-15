# Elite Memo Pro — Target Architecture

**Status:** proposal (tracked in #6) · **Base:** current Java/View codebase on `main`

This document defines the target design for Elite Memo Pro: a **Kotlin + Jetpack Compose (Material 3)** app, split into **Gradle modules**, organised by **Clean Architecture** (presentation → domain → data).

---

## 1. Goals

- Modern, declarative **Jetpack Compose UI** — no more XML layouts, View adapters, or `findViewById`.
- **Modular Gradle project** so build times, ownership boundaries, and reusability improve as the app grows.
- **Clean Architecture layering**: UI never touches SQLite/SharedPreferences; everything flows through use cases and repositories.
- Preserve all current features: biometric lock, auto-save, timestamps, search, dark theme, Markdown/PDF export, 100% offline.
- Preserve **existing user data** (`notes.db`) via a Room migration — no destructive upgrade.
- Testable at every layer.

### Non-goals (for this redesign)

- No cloud sync, no accounts, no analytics — the app stays offline-first and tracker-free.
- No feature changes beyond what the migration requires.

---

## 2. Module graph

```
                       ┌─────────────┐
                       │    :app     │  single-Activity host, NavHost, Hilt wiring
                       └──────┬──────┘
        ┌──────────────┬──────┴───────┬───────────────┐
        ▼              ▼              ▼               ▼
 ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐
 │ :feature:  │ │ :feature:  │ │ :feature:  │ │   :data     │
 │   notes    │ │   editor   │ │  settings  │ │ (repos,     │
 │ Compose +  │ │ Compose +  │ │ Compose +  │ │  mappers)   │
 │ ViewModel  │ │ ViewModel  │ │ ViewModel  │ └─────┬──────┘
 └─────┬──────┘ └─────┬──────┘ └─────┬──────┘       │
       │               │             │              │
       ▼               ▼             ▼              ▼
            ┌──────────────────────────────┐   ┌────────────────┐
            │           :domain            │   │ :core:database │
            │  use cases + repo interfaces│   │     (Room)     │
            └──────────────┬───────────────┘   └────────────────┘
                           ▼                        ┌────────────────┐
                  ┌────────────────┐               │ :core:datastore│
                  │  :core:model   │               │    (settings)  │
                  │   Note, etc.   │               └────────────────┘
                  └────────────────┘

  :core:designsystem  ← theming + shared Composables (used by every :feature:*)
  :core:common        ← dispatchers, Result wrapper, time source
```

**Rule of thumb:** dependencies point inwards/downwards only. `:feature:*` modules depend on `:domain` and `:core:designsystem` — never on `:core:database`, `:core:datastore`, or `:data`.

---

## 3. Module map

| Module | Type | Contents |
|---|---|---|
| `:app` | Android app | `MainActivity` (single activity), `NotesApp` (`@HiltAndroidApp`), `NavHost`, DI wiring |
| `:core:designsystem` | Android lib | Compose Material 3 theme (light/dark), typography, colours, shared components (`EmptyState`, `NoteCard`) |
| `:core:model` | Kotlin lib | `Note` domain model |
| `:core:common` | Kotlin lib | `DispatchersProvider`, `AppResult<T>`, `TimeProvider` |
| `:core:database` | Android lib | Room database, `NoteEntity`, `NoteDao`, migrations |
| `:core:datastore` | Android lib | `SettingsDataStore` (theme mode, biometric lock) replacing `SharedPreferences` |
| `:domain` | Kotlin lib | `NoteRepository` / `SettingsRepository` interfaces + use cases |
| `:data` | Android lib | Repository implementations, entity↔model mappers |
| `:feature:notes` | Android lib | Notes list screen: search, staggered grid, multi-select, biometric gate |
| `:feature:editor` | Android lib | Note editor screen: title/content fields, debounced auto-save, Markdown preview, PDF export |
| `:feature:settings` | Android lib | Settings screen in Compose |

Gradle hygiene: a **version catalog** (`gradle/libs.versions.toml`) plus **convention plugins** hold the shared Android/Kotlin/Hilt config so no module's `build.gradle.kts` drifts.

---

## 4. Data layer

### Room replaces `SQLiteOpenHelper`

The current `DatabaseHelper` creates:

```sql
CREATE TABLE notes_table (
  ID      INTEGER PRIMARY KEY AUTOINCREMENT,
  TITLE   TEXT,
  CONTENT TEXT,
  DATE    TEXT
);
```

The Room entity keeps the **exact same column and table names**, so the existing database opens without a destructive migration:

```kotlin
@Entity(tableName = "notes_table")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "ID") val id: Long = 0,
    @ColumnInfo(name = "TITLE") val title: String,
    @ColumnInfo(name = "CONTENT") val content: String,
    @ColumnInfo(name = "DATE") val date: String,
)
```

`NoteDao` exposes:

- `observeNotes(query: String): Flow<List<NoteEntity>>` — uses `WHERE TITLE LIKE :q OR CONTENT LIKE :q ORDER BY ID DESC` (search moves from Java filtering into SQL)
- `getNote(id: Long): NoteEntity?`
- `insert(note: NoteEntity): Long`
- `update(note: NoteEntity)`
- `deleteByIds(ids: List<Long>)`

All DAO methods are suspend/`Flow`; repositories map `NoteEntity` → `Note` domain model in `:data`.

### DataStore replaces `SharedPreferences`

`pref_lock` (biometric lock) and the theme choice move to Preferences DataStore, exposed as `Flow<AppSettings>`. `PreferenceManager` and `androidx.preference` drop out entirely.

---

## 5. Domain layer

Pure Kotlin, no Android dependencies:

- `NoteRepository` — `observeNotes(query)`, `getNote(id)`, `upsert(note)`, `delete(ids)`
- `SettingsRepository` — `settings: Flow<AppSettings>`, `setThemeMode(...)`, `setBiometricLock(...)`
- Use cases (one class, one `operator fun invoke`):
  - `ObserveNotesUseCase(query: String): Flow<List<Note>>`
  - `GetNoteUseCase(id: Long)`
  - `SaveNoteUseCase(note: Note)` — handles both create and update
  - `DeleteNotesUseCase(ids: List<Long>)`
  - `ObserveSettingsUseCase` / `UpdateSettingUseCase`

This keeps ViewModels thin and makes business rules unit-testable without Android.

---
## 6. Presentation layer

Each feature follows **unidirectional data flow (UDF)**:

```
UI events → ViewModel (reduce) → StateFlow<UiState> → @Composable renders
```

Example — `NotesViewModel`:

```kotlin
@Stable
data class NotesUiState(
    val isLoading: Boolean = true,
    val notes: List<Note> = emptyList(),
    val query: String = "",
    val selection: Set<Long> = emptySet(),
    val isLocked: Boolean = false,   // biometric gate
)
```

- **Notes screen**: `LazyVerticalStaggeredGrid` of `NoteCard`s, top-app-bar search, multi-select with contextual action bar, empty state, biometric gate composable (`BiometricPrompt` via `LocalActivityResult` wrapper).
- **Editor screen**: title + content `TextField`s; **auto-save is debounced (500 ms)** via a `viewModelScope` job collecting text changes — replacing the per-keystroke `TextWatcher` that wrote to SQLite on every character.
- **Settings screen**: Compose `Switch`/radio list bound to `AppSettings` flow.

Navigation: single `MainActivity` + `NavHost` (`androidx.navigation:navigation-compose`) with routes `notes`, `editor/{noteId?}`, `settings`.

---

## 7. Tech stack

| Concern | Choice |
|---|---|
| Language | Kotlin (JVM toolchain 17) |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| State | `StateFlow` + `ViewModel` (UDF/MVI-style) |
| Async | Kotlin coroutines (`DispatchersProvider` for testability) |
| DI | Hilt |
| Persistence | Room (+ migration), Preferences DataStore |
| Security | `androidx.biometric` (unchanged) |
| Markdown / PDF | Markwon (wrapped behind an `export` interface until a Compose-native renderer is chosen) |
| Build | Gradle version catalog + convention plugins, AGP/Kotlin/Compose BOM aligned |

---

## 8. Testing strategy

- **Domain/data**: JUnit + Turbine for Flow tests; fake repository implementations; Room in-memory DB tests for DAO + migration (`MigrationTestHelper`).
- **Features**: Compose UI tests (`createAndroidComposeRule`) for list/search/selection and editor auto-save behaviour.
- **DI**: `@TestInstallIn` swaps real bindings for fakes.

---

## 9. Migration plan

| Phase | Scope | Notes |
|---|---|---|
| 0 | Design doc (this file), version catalog, convention plugins, empty module skeletons | no runtime change |
| 1 | `:core:database` (Room + migration), `:core:datastore`, `:domain`, `:data` + tests | old `DatabaseHelper` still live in `:app` |
| 2 | `:feature:notes` in Compose, wired behind the new `:app` host | old list UI removed |
| 3 | `:feature:editor` in Compose with debounced auto-save | |
| 4 | `:feature:settings` in Compose on DataStore | |
| 5 | Delete legacy Java/XML code and unused View dependencies (`appcompat` UI, `constraintlayout`, `preference`) | CI: `assembleDebug` + `test` green |

Each phase ships behind a green CI build so `main` is always releasable.

---

## 10. Open questions

1. Markdown rendering in Compose — keep Markwon (View-interop) vs. adopt a Compose-native renderer?
2. Keep `DATE` as pre-formatted string (current) vs. migrate to epoch-millis column (better sorting; needs a one-time data migration)?
3. Should the biometric gate re-arm on `onStop` (backgrounding), as most secure notepads do?
