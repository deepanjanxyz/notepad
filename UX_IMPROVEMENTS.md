# UX & Accessibility Refactor

This document is the reference for the `feat/ux-polish-a11y-refactor` branch.
It lists every UX change, the reasoning behind it and how it was verified.

## Why

A review of the `dev` branch surfaced four classes of problem:

1. **Accessibility** — most icon-only controls had labels, but the note cards,
   the colour-filter row, the editor's save button and the app-lock screen did not.
   Several interactive targets were 16–38 dp, well below the 48 dp Material/WCAG
   minimum.
2. **Contrast** — tinted note cards inherited the active colour scheme's text
   colour. In the light theme that meant near-black text on a dark note tint.
3. **Missing states** — every list screen rendered its empty illustration while
   the Room query was still running, and no operation could report a failure.
4. **Feedback & navigation** — destructive actions gave no undo, and screen
   transitions were an undifferentiated cross-fade.

## Accessibility

| Change | Rationale |
| --- | --- |
| Note cards expose a spoken description (`title, drawing, pinned, selected`) via `contentDescription` / `stateDescription` and named click / long-click actions. | TalkBack previously read only the clipped snippet. |
| Colour filters are `toggleable` nodes with a per-colour label built from `NoteColorNames` (previously declared but never used). | The row was invisible to screen readers and unusable with a keyboard/D-pad. |
| Filter row targets are 48 dp; the 30 dp dot stays as the visual. | WCAG 2.5.5 target size. |
| Trash/archive card action buttons no longer override their size to 28 dp; icons are 20 dp inside the default 48 dp container. | Target size. |
| Selection counter, filter sub-header and loading view are polite live regions. | Changes are announced instead of being silent. |
| App-lock screen gains a labelled, reachable secondary action. | The previous `onBypass` parameter was never wired to any control. |
| Navigation drawer exposes a `paneTitle`; decorative icons are marked as such. | Avoids duplicated announcements. |
| Settings switch declares `Role.Switch`. | Assistive tech reports the control type. |

## Contrast & visual polish

- New `NoteTintContent` / `NoteTintContentMuted` / `NoteTintOutline` / `NoteTintChip`
  tokens keep tinted note cards readable in **both** themes: text on a tint now
  uses a light foreground (~9:1 contrast) instead of near-black on dark.
- Complete Material 3 type scale (`bodySmall`, `labelLarge`, `labelMedium`,
  `titleSmall`, `headlineSmall` were silently falling back to library defaults).
- Spacing, corner radii and elevation come from `Spacing` / `EliteMemoShapes`
  instead of per-screen magic numbers.
- `values-night/themes.xml` gives the system bars the right icon polarity, and
  the bar colour follows the theme instead of being forced transparent.

## Loading, empty and error states

- New `LoadingState`, `EmptyState` and `ErrorState` components replace four
  bespoke copies of the same markup.
- Screens distinguish **loading** from **empty** using `NotesUiState.isContentReady`,
  so the "No notes yet" illustration no longer flashes before the first Room emission.
- Database operations are wrapped so a failure emits `FeedbackType.ACTION_FAILED`
  and reaches the user as a snackbar instead of failing silently.
- Empty states now carry a primary action (create a note / clear filters).

## Interaction feedback & navigation

- Moving notes to trash or archive now shows a snackbar with **Undo**
  (`NotesViewModel.undoLastAction` restores the affected notes).
- Restore and permanent-delete actions report their result.
- Screen transitions are direction-aware (slide + fade by navigation depth)
  rather than an undifferentiated cross-fade.
- The reminder-notification deep link no longer navigates past the app lock.

## Bugs fixed along the way

- **Trash multi-select was a no-op**: `TrashScreen` kept its own selection set
  while `NotesViewModel` read `uiState.selectedNoteIds`, so *Restore selected*
  and *Delete permanently* operated on an always-empty list. Archive and Trash
  now share the view-model selection state.
- **Navigation drawer gesture was active on the drawing canvas**, so an edge
  swipe over a sketch opened the drawer (it now excludes the editor screen).
- **Word count in Settings** counted the serialised drawing payload as text,
  inflating the workspace statistics.
- **The empty-state icon used identical branches** for the search and no-notes cases.
- **`SetupScreen` is not reachable** (no navigation entry and no dialog host),
  while `SetupRepository` is a self-contained unused helper. Left untouched here —
  flagged as a follow-up cleanup so this PR stays an UX-only change.

## Verification

- `./gradlew test` — new `UxPolishTest` covers the pure helpers (colour-name
  lookup, drawing-aware word count, filter classification, card description).
- The existing `NoteLogicTest` is untouched and still passes; the public API it
  uses (`NoteColorOptions`, `NoteColorNames`, `getSelectionBounds`, `NoteFilter`)
  is unchanged.
- Manual check list for reviewers:
  1. TalkBack traversal of the home grid, the colour filter row and the editor top bar.
  2. Light/dark comparison of a tinted note — body text must stay legible.
  3. Cold start with an empty database — loading view first, then the empty state.
  4. Trash: select two notes → *Restore selected* must restore both.
  5. Move a note to trash → *Undo* in the snackbar must bring it back.
