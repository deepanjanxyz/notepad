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
4. **Feedback & navigation** — destructive actions gave no confirmation and no
   undo, and the navigation transitions were an undifferentiated cross-fade.

## Accessibility

| Change | Rationale |
| --- | --- |
| Note cards expose a spoken description (`title, drawing, pinned, selected`) via `stateDescription` and `onClickLabel` / `onLongClickLabel`. | TalkBack previously read only the clipped snippet. |
| Colour filters are `toggleable` nodes with a per-colour label built from `NoteColorNames` (previously declared but never used). | The row was invisible to screen readers and unusable with a keyboard. |
| Filter row targets are 48 dp; the 30 dp dot stays as the visual. | WCAG 2.5.5 target size. |
| Trash/archive card action buttons no longer override their size to 28 dp; icons are 20 dp inside the default 48 dp container. | Target size. |
| Editor save control is a `Role.Button` with a 48 dp touch target. | It was a bare `Box` + `clickable`. |
| Selection counter, filter sub-header and loading view are polite live regions. | Changes are announced instead of being silent. |
| App-lock screen gains a labelled, reachable secondary action. | The previous `onBypass` parameter was never wired to any control. |

## Contrast & visual polish

- New `NoteTintContent` / `NoteTintContentMuted` / `NoteTintOutline` tokens keep
  tinted note cards readable in **both** themes, and chips on a tinted card use a
  translucent surface instead of a light one.
- Complete Material 3 type scale (`bodySmall`, `labelLarge`, `labelMedium`,
  `titleSmall`, `headlineSmall` were silently falling back to library defaults).
- Spacing, corner radii and elevation come from `Spacing` / `EliteMemoShapes`
  instead of per-screen magic numbers.

## Loading, empty and error states

- New `LoadingState`, `EmptyState` and `ErrorState` components replace four
  bespoke copies of the same markup.
- Screens distinguish **loading** from **empty** using `NotesUiState.isContentReady`,
  so the "No notes yet" illustration no longer flashes before the first Room emission.
- Database failures set `NotesUiState.errorMessage`, which is surfaced in a
  dismissible snackbar instead of failing silently.

## Interaction feedback & navigation

- Moving notes to Trash or Archive now shows a snackbar with **Undo**.
- Restore and permanent-delete actions report their result.
- Screen transitions are direction-aware (slide + fade by navigation depth)
  rather than an undifferentiated cross-fade.

## Bugs fixed along the way

- **Trash multi-select was a no-op**: `TrashScreen` kept its own selection set
  while `NotesViewModel` read `uiState.selectedNoteIds`, so *Restore selected*
  and *Delete permanently* restored/deleted nothing. Archive and Trash now share
  the view-model selection state.
- **Navigation drawer gesture was active on the drawing canvas**, so an edge
  swipe over a sketch opened the drawer. The gesture is now disabled for the
  editor and the drawing screen.
- **Word count in Settings** counted the serialised drawing payload as text.
- The empty-state icon used identical branches for the search and no-notes cases.

## Verification

- `./gradlew test` — new `UxPolishTest` covers the pure helpers (colour-name
  lookup, drawing-aware word count, filter classification, card description).
- Existing `NoteLogicTest` is untouched and still passes; the public API it uses
  (`NoteColorOptions`, `NoteColorNames`, `getSelectionBounds`, `NoteFilter`) is unchanged.
- Manual check list for reviewers: TalkBack traversal of the home grid, colour
  filter row and editor top bar; light/dark theme comparison of a tinted note;
  cold start with an empty database (must show the loading view, then the empty state).
