# GitHub Actions workflows

This directory holds every CI/CD workflow for Elite Memo Pro. Each file has a
single responsibility, so it is easy to see what runs, when, and why.

## Index — which file does which job

| Workflow file | Name | Trigger | What it does |
|---|---|---|---|
| [`ci.yml`](./ci.yml) | CI | `push` / `pull_request` to `dev` and `main`, plus manual | Runs the unit tests and a debug build, then uploads the test reports. This is the build-verification gate. |
| [`android.yml`](./android.yml) | Build and Sign APK | manual (`workflow_dispatch`) | Builds the unsigned release APK, decodes the keystore from secrets, signs the APK, and uploads it as the `release-apk` artifact. |
| [`mirror.yml`](./mirror.yml) | Multi-Platform Smart Auto Mirroring | `push` of any branch or tag | Materializes every branch locally, then mirrors all branches and tags to GitLab and Codeberg (force-syncing when a normal push is rejected). |
| [`universal-pr-check.yml`](./universal-pr-check.yml) | Universal PR Check | `pull_request` (opened, synchronize, reopened) to `dev` | Runs the build, unit tests, lint, detekt and a secret scan as parallel jobs on JDK 17, then posts a single status-table comment on the PR and blocks the merge if any check fails. |
| [`auto-release.yml`](./auto-release.yml) | Auto Release | `pull_request` / `push` to `main` | Guards on real application-code changes, commits the version bump straight onto the source PR branch when a `main` PR skipped it, and on merge to `main` publishes a GitHub release tagged `v<versionName>` with the signed, renamed APK and traced release notes. |

## In one line each

- **Tests / verification** → `ci.yml`
- **Release build + signing** → `android.yml`
- **Mirroring (push) to other hosts (GitLab, Codeberg)** → `mirror.yml`
- **Per-PR parallel gate with a unified status report** → `universal-pr-check.yml`
- **Version bump + GitHub release for `main`** → `auto-release.yml`

## Required repository secrets

| Secret | Used by | Purpose |
|---|---|---|
| `KEYSTORE_BASE64` | `android.yml`, `universal-pr-check.yml`, `auto-release.yml` | Base64-encoded release keystore. |
| `KEYSTORE_PASSWORD` | `android.yml`, `universal-pr-check.yml`, `auto-release.yml` | Release keystore password. |
| `KEY_ALIAS` | `android.yml`, `universal-pr-check.yml`, `auto-release.yml` | Release key alias. |
| `KEY_PASSWORD` | `android.yml`, `universal-pr-check.yml`, `auto-release.yml` | Release key password. |
| `GITLAB_TOKEN` | `mirror.yml` | Push access to the GitLab mirror. |
| `CODEBERG_TOKEN` | `mirror.yml` | Push access to the Codeberg mirror. |

## Universal PR Check (`universal-pr-check.yml`)

A branch-scoped gate that runs on every pull request targeting `dev`. All build,
test and inspection tasks run as **parallel jobs**, so the suite finishes in the
time of its slowest job rather than the sum of all of them, and the outcome is
reported back onto the PR as a single status table.

**Trigger.** `pull_request` events only, of type `opened`, `synchronize`, and
`reopened`, limited to pull requests whose base branch is `dev`. It deliberately
does not run on plain `push`.

**Parallel jobs.**

| Job | Command / action | Notes |
|---|---|---|
| Debug & Release build | `./gradlew assembleDebug assembleRelease --no-daemon --parallel` | Decodes the release keystore from `KEYSTORE_BASE64` and exposes `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` to Gradle so the release variant is signed, not just assembled. |
| Unit tests | `./gradlew testDebugUnitTest --no-daemon --parallel` | |
| Android lint | `./gradlew lintDebug --no-daemon` | Counts lint warnings so the report can flag non-blocking issues. |
| Static analysis (detekt) | `./gradlew detekt --no-daemon` | Runs only when detekt is configured in the project; skipped otherwise. |
| Secret scan | `gitleaks/gitleaks-action@v3` | Scans the checkout for hardcoded secrets. |

**Environment.** Every job runs on `ubuntu-latest`, checks out with full history
(`fetch-depth: 0`), sets up Temurin **JDK 17**, and configures Gradle caching via
`gradle/actions/setup-gradle`. The JDK is pinned to **17** on purpose: the project
compiles against `sourceCompatibility`/`targetCompatibility` 17 and must not be
silently upgraded to a newer JDK. The Android SDK platform the project builds
against (`compileSdk`) is installed before the Gradle build.

**Permissions.** `contents: read`, `pull-requests: write`, and `issues: write` —
just enough to read the code and write feedback back onto the pull request.

**Status report.** A final job waits for every parallel job and creates or
updates a single Markdown table comment on the PR. Each row uses a status
indicator:

| Indicator | Meaning |
|---|---|
| 🟢 | Passed — clean execution. |
| 🟡 | Warning — non-blocking warnings (e.g. lint suggestions). |
| 🔴 | Failed — blocking error. |
| ⚪ | Skipped / N/A — optional or unconfigured task. |

When every check passes, a thumbs up (`+1`) reaction is added to the PR. If any
check fails, its job fails and the merge button stays blocked until it is fixed.

## Auto Release (`auto-release.yml`)

A `main`-only release pipeline that refuses to act unless real application code
changed. It bumps the version straight onto the source PR branch when a `main` PR
forgot to, and publishes a GitHub Release when the change lands.

**Trigger.** `pull_request` events (`opened`, `synchronize`, `reopened`) whose
base branch is `main`, and `push` events to `main` (i.e. merges). The workflow
runs on **every** such event — path filtering is done inside the `guard` job, not
at the trigger, so a docs-only PR never leaves a required status check waiting
forever. Non-code changes simply skip the bot jobs (see `code_changed` below).

**Concurrency.** Two layers, both non-cancelling:

- Workflow level: `group: release-${{ github.ref }}`, so runs for the same ref
  never overlap.
- The `auto-bump` job additionally pins a strict global group `auto-bump-main`
  (`cancel-in-progress: false`), so bump runs never overlap.

**Guard (`guard` job).** Runs on both events and computes two facts used by the
later jobs:

- `code_changed` — a `git diff` against the base confirms that application code
  actually changed. Only paths under `app/src/main/`, or the root Gradle
  configuration files (`*.gradle.kts`, `gradle.properties`), count. Changes under
  `app/src/test/` and `app/src/androidTest/` (and docs/images) do not. If nothing
  matches, every bot step is skipped — no version check, no bump, no release.
- `version_updated` (pull requests only) — whether `versionCode` and
  `versionName` in `app/build.gradle.kts` differ from `main`.

**Auto-bump (`auto-bump` job).** On a `main` PR that changes code but leaves the
version untouched — and was not opened by `github-actions[bot]` — it:

1. Checks out the PR's own head branch (`ref: github.event.pull_request.head.ref`,
   authenticated with `GITHUB_TOKEN`).
2. Runs a **pre-flight verification** that extracts the app name
   (`rootProject.name` from `settings.gradle.kts`), `versionName` and
   `versionCode` from `app/build.gradle.kts` and fails immediately if any is
   missing or malformed (`versionName` must be strict `X.Y.Z`, `versionCode` a
   positive integer).
3. Increments `versionCode` by 1 and bumps the patch of `versionName`, commits
   `chore(release): bump version [skip ci]` and pushes
   `HEAD:${{ github.event.pull_request.head.ref }}` — a plain `git push`, never a
   force-push. If the file did not actually change, the job fails.

There is **no companion PR**: the code change and the version bump live on the
same branch, so merging the PR brings both into `main` atomically with no
out-of-sync merges and no cross-PR version collisions.

**Release (`release` job).** On a merge to `main` with code changes it runs a
strict pre-flight before doing anything else:

1. Reads the app name, `versionName` and `versionCode`.
2. `versionName` must match `^[0-9]+\.[0-9]+\.[0-9]+$`; otherwise the job fails
   with `exit 1`.
3. `versionCode` must be a positive integer (`^[1-9][0-9]*$`); if it is missing or
   malformed, the job fails with `exit 1`.
4. The `v<versionName>` tag must not already exist on the remote; if it does, the
   job fails hard (`exit 1`) rather than silently skipping.

It then resolves the release context, builds the signed release APK, and:

- **Renames** every produced APK to `<AppName>-v<VersionName>-<VersionCode>.apk`
  (e.g. `EliteMemoPro-v2.1.0-11.apk`) — never a generic `app-release.apk`.
- **Verifies signatures** with the Android SDK `apksigner verify --verbose`,
  failing the job if any APK is unsigned or has an invalid signature.
- **Appends SHA-256 checksums** for the renamed APKs into the release notes.

Finally it publishes the release with the native GitHub CLI
(`gh release create "v<versionName>" … --notes-file /tmp/release-notes.md`,
authenticated with `GH_TOKEN`) — no third-party release action. The release notes
prefer the merged PR's title and body; if the tip commit is the bot's bump commit,
it walks `git log` back to the last human commit and uses that contributor's
PR/commit context instead.

**Permissions.** Least privilege: the workflow default is `contents: read`; only
the `auto-bump` job and the `release` job are elevated to `contents: write`.

## Store screenshots (`store-screenshots.yml`)

Generates the store screenshots for Elite Memo Pro on a headless Android
emulator and uploads them as workflow artifacts.

**Trigger.** Manual only (`workflow_dispatch`) — start it from **Actions → Store
Screenshots → Run workflow** and choose the branch to run against. It never runs
on `push` or `pull_request`.

**What it does.**

1. Checks out the app from `dev` and the automation scripts from the branch the
   workflow runs on (`scripts/screenshots/`).
2. Builds the debug APK with `./gradlew assembleDebug`.
3. Boots a headless Pixel 6 emulator (API 34, x86_64, google_apis) with KVM.
4. Installs the APK, seeds realistic notes through the real UI, and captures a
   full-resolution screenshot of each feature.
5. Uploads the screenshots and a short log as artifacts.

**Where the screenshots are generated and stored.**

| Location | Contents | Notes |
|---|---|---|
| Run artifact `screenshots` | Full-resolution PNGs (1080x2400) | Uploaded by the workflow; retained for the default 90 days. |
| Run artifact `notes-and-logs` | README listing each screenshot, the run log, and debug UI dumps | From the same run. |
| `fastlane/metadata/android/en-US/images/phoneScreenshots/` | The store-ready screenshot set | Committed in the repository. |

**Screenshots captured.**

| File | Screen |
|---|---|
| `01-home-grid-dark.png` | Home, two-column grid layout |
| `02-home-list-dark.png` | Home, single-column list layout |
| `03-search-results-dark.png` | Search results |
| `04-navigation-drawer-dark.png` | Navigation drawer |
| `05-note-editor-dark.png` | Note editor |
| `06-labels-sheet-dark.png` | Labels bottom sheet |
| `07-color-picker-dark.png` | Color picker |
| `08-checklist-editor-dark.png` | Checklist editor |
| `09-drawing-note-dark.png` | Drawing note |
| `10-archive-dark.png` | Archive |
| `11-trash-dark.png` | Trash |
| `12-settings-dark.png` | Settings |
| `13-reminder-dialog-dark.png` | Reminder dialog |
| `14-reminder-applied-dark.png` | Reminder applied |

**Usage notes.**

- Screenshots are captured in dark mode only.
- The status bar is normalised with Android demo mode (fixed 12:00 clock, full
  battery, full Wi-Fi and cellular) and animations are disabled for stable,
  consistent captures.
- The app is always checked out from `dev`; the scripts come from the branch the
  workflow is run on. Re-running the workflow regenerates every screenshot.
- No repository secrets are required, and a run takes roughly 20 minutes.
