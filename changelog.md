# Changelog

## v1.1.0 (versionCode 34)

### Rich-Text Formatting & In-Note Links
- **Inline and block styles**: Format notes with **Bold**, *Italic*, Underline, Strikethrough, **Heading 1 (H1)**, **Heading 2 (H2)**, and **Clear formatting** from the editor formatting bar.
- **List & checklist support**: Apply inline formatting and links inside individual checklist, bulleted, and numbered list items without converting the list back to plain text.
- **In-note hyperlinks**: Insert, edit, open, and remove `https://`, `http://`, `mailto:`, and `tel:` links via the link dialog and cursor-aware active link pill, with safe URL validation.
- **Consistent rendering & export**: Styled text and links render cleanly in the editor, home/archive cards, PDF exports, and clickable PDF annotations, while raw style markers are stripped from cards, shortcuts, search, and exports without database schema changes.
- **Search & Markdown interoperability**: Search matches link URLs as well as note text, and Markdown export/import converts non-code-block links cleanly using standard `[label](url)` syntax.

### Bulk Note Actions & Selection UI
- **Bulk recolour & re-label**: Change the colour or manage labels (with tri-state checkboxes and inline new-label creation) across multiple selected notes from both the Home and Archive screens.
- **Streamlined Home selection bar**: Kept **Select all** and **Pin** directly on the selection bar while organizing **Archive**, **Change color**, **Labels**, **Export as Markdown**, and **Delete** into the overflow (`⋮`) menu.
- **Redesigned Markdown export sheet**: Updated the multi-note Markdown export bottom sheet with clearer layout and typography for exporting separate files (`.zip`) or one combined `.md` document.
- **Localized "New" badges**: Added compact **New** indicators (localized in English, Bengali, and Hindi) beside newly added options.

### Touch Gestures, Custom Order & Animations
- **Drag-and-drop note reordering**: Long-press and drag notes on the home screen to arrange them freely within their section (**Pinned** or **Others**) with smooth finger tracking and slot transitions. Automatically switches to and persists a **Custom** sort option across restarts.
- **Long-press swipe-to-archive**: Long-press and swipe left to reveal a smooth finger-tracked green archive indicator, moving the note to Archive on release with Undo support.
- **Drop-into-bin delete animation**: Replaced the abrupt home-screen delete behavior with an animation that drops deleted note cards into a trash bin icon before starting the Undo countdown snackbar.
