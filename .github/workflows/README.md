# Store screenshots workflow

`store-screenshots.yml` generates the store screenshots for Elite Memo Pro on a
headless Android emulator and uploads them as workflow artifacts.

## What it does

1. Checks out the app from `dev` and the automation scripts from the branch the
   workflow runs on (`scripts/screenshots/`).
2. Builds the debug APK with `./gradlew assembleDebug`.
3. Boots a headless Pixel 6 emulator (API 34, x86_64, google_apis) with KVM.
4. Installs the APK, seeds realistic notes through the real UI, and captures
   full-resolution screenshots of each feature: home grid and list, search,
   navigation drawer, note editor, labels sheet, color picker, checklist editor,
   drawing note, archive, trash, settings, and the reminder flow.
5. Uploads the screenshots and a short log as artifacts.

## Trigger

Manual only (`workflow_dispatch`). Start it from **Actions → Store Screenshots →
Run workflow** and choose the branch to run against. It does not run on push or
on pull requests.

## Where the screenshots are generated and stored

- **Run artifacts** (retained for the default 90 days):
  - `screenshots` — the full-resolution PNGs (1080x2400).
  - `notes-and-logs` — a README listing each screenshot, the run log, and debug
    UI dumps.
- **Store metadata** — the store-ready set is kept in the repository under
  `fastlane/metadata/android/en-US/images/phoneScreenshots/`.

## Usage notes

- Screenshots are captured in dark mode only.
- The status bar is normalised with Android demo mode (fixed 12:00 clock, full
  battery, full Wi-Fi and cellular) and animations are disabled for stable,
  consistent captures.
- The app is always checked out from `dev`; the scripts come from the branch the
  workflow is run on. Re-running the workflow regenerates every screenshot.
- No repository secrets are required, and a run takes roughly 20 minutes.
