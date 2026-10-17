# GitHub Actions workflows

This directory holds every CI/CD workflow for Elite Memo Pro. Each file has a
single responsibility, so it is easy to see what runs, when, and why.

## Index — which file does which job

| Workflow file | Name | Trigger | What it does |
|---|---|---|---|
| [`ci.yml`](./ci.yml) | CI | `push` to `dev` and `main`, `pull_request` to `main`, plus manual | Runs the unit tests and a debug build, then uploads the test reports. Pull requests into `dev` are left to `universal-pr-check.yml`, so the same build and tests are not run twice. |
| [`mirror.yml`](./mirror.yml) | Multi-Platform Smart Auto Mirroring | `push` of any branch or tag | Materializes every branch locally, then mirrors all branches and tags to GitLab and Codeberg as two separate jobs. It force-syncs only when a normal push is rejected as a divergence (non-fast-forward or an existing tag); any other failure (auth, network, hooks) is reported instead of force-pushing. The run stays green while either host accepts the push and turns red only when both fail, and any failure opens (or comments on) a `bug` issue naming the host that failed; once both hosts are syncing again it closes that issue, and it only ever touches the issue it opened itself. |
| [`universal-pr-check.yml`](./universal-pr-check.yml) | Universal PR Check | `pull_request` (opened, synchronize, reopened) to `dev` | Runs the build, unit tests, lint, detekt and a secret scan as parallel jobs on JDK 21, then posts a single status-table comment on the PR and blocks the merge if any check fails. |
| [`version-guard.yml`](./version-guard.yml) | Version Guard | `pull_request` (opened, synchronize, reopened) to `main`, plus `workflow_dispatch` | Requires a version bump only when shipping files change (a deep diff check ignores docs, images, comments and whitespace-only edits); otherwise it posts one sticky comment asking for a bump. |
| [`version-bump.yml`](./version-bump.yml) | Version Bump | `issue_comment` (created) | Turns an authorised `/bump-version` comment on a `main` pull request into a version bump on that branch, then re-fires the guard. Any other comment does nothing. |
| [`auto-release.yml`](./auto-release.yml) | Auto Release | `push` to `main` | On a merge that changes a shipping file, publishes a GitHub release tagged `v<versionName>` with the signed, renamed APKs and traced release notes. |
| [`store-screenshots.yml`](./store-screenshots.yml) | Store Screenshots | manual (`workflow_dispatch`) | Builds a debug APK and captures the store screenshots on a headless Pixel 6 emulator, then uploads them as artifacts. |
| [`pr-review-commands.yml`](./pr-review-commands.yml) | PR Review Commands | `issue_comment` (created) | Turns `/review`, `/test`, `/force-review`, `/add-approve-user` and `/remove-approve-user` comments on `main` pull requests into an automated review or an allowlist change, after checking the commenter against `.github/pr-reviewers.yml`. |

## In one line each

- **Tests / verification on push, and on pull requests into `main`** → `ci.yml`
- **Mirroring (push) to other hosts (GitLab, Codeberg)** → `mirror.yml`
- **Per-PR parallel gate with a unified status report** → `universal-pr-check.yml`
- **Is a version bump required?** → `version-guard.yml`
- **`/bump-version` command** → `version-bump.yml`
- **GitHub release for `main`** → `auto-release.yml`
- **Store screenshots on an emulator** → `store-screenshots.yml`
- **Slash-command reviews and reviewer allowlist** → `pr-review-commands.yml`

## Required repository secrets

| Secret | Used by | Purpose |
|---|---|---|
| `KEYSTORE_BASE64` | `universal-pr-check.yml`, `auto-release.yml` | Base64-encoded release keystore. |
| `KEYSTORE_PASSWORD` | `universal-pr-check.yml`, `auto-release.yml` | Release keystore password. |
| `KEY_ALIAS` | `universal-pr-check.yml`, `auto-release.yml` | Release key alias. |
| `KEY_PASSWORD` | `universal-pr-check.yml`, `auto-release.yml` | Release key password. |
| `GITLAB_TOKEN` | `mirror.yml` | Push access to the GitLab mirror. |
| `CODEBERG_TOKEN` | `mirror.yml` | Push access to the Codeberg mirror. |
| `COPILOT_TOKEN` | `auto-release.yml` | Optional. Enables the Copilot summary in the release notes. |

## Universal PR Check (`universal-pr-check.yml`)

A branch-scoped gate that runs on every pull request targeting `dev`. All build,
test and inspection tasks run as **parallel jobs**, so the suite finishes in the
time of its slowest job rather than the sum of all of them, and the outcome is
reported back onto the PR as a single status table.

Runs for the same pull request share a concurrency group with
`cancel-in-progress: true`, so a new push cancels the superseded run instead of
leaving two six-job matrices racing over the same pull request.

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
(`fetch-depth: 0`), sets up Temurin **JDK 21**, and configures Gradle caching via
`gradle/actions/setup-gradle`. The JDK matches the project: every module compiles
against `sourceCompatibility`/`targetCompatibility` `JavaVersion.VERSION_21`. The
Android SDK platform the project builds against (`compileSdk`) is installed
before the Gradle build.

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

## Version Guard (`version-guard.yml`)

The `main`-only gate that keeps the version honest. It runs on every pull
request into `main` — path filtering is done inside the `guard` job, not at the
trigger, so a docs-only PR never leaves a required status check waiting forever.

**Guard (`guard` job).** Computes the single fact the other jobs need:

- `code_changed` — a **deep check** of the real diff against the base. A file
  counts only when it ships in the APK or affects how it is built: anything under
  `app/`, plus `*.gradle.kts`, `*.gradle`, `gradle.properties`,
  `settings.gradle.kts`, `gradle/libs.versions.toml` and `gradle/wrapper/*`. Even
  then an edit is ignored when it only moves whitespace or blank lines, or touches
  comment lines. A reformat or a comment tweak therefore does not cut a release,
  while a one-line behaviour change does.

The same `guard` job also lives in `auto-release.yml`, where it answers the same
question for a merge. The two copies are kept byte-identical.

**Version guard (`version-guard` job).** Consumes `needs.guard.outputs.code_changed`:

- When a **shipping** file changed, it requires the pull request to raise both
  `versionCode` (above the latest release) and `versionName` (a greater strict
  `X.Y.Z`), with a `versionCode` no other open pull request claims. Otherwise it
  posts one sticky comment (marker `<!-- version-guard -->`) and fails.
- When **no shipping file** changed — a documentation-only pull request touching
  `README`, `*.md`, `*.txt`, images or any other non-shipping path — the bump
  requirement is skipped: the job logs a notice and passes, so documentation can
  be merged without touching the version.

**Trigger.** `pull_request` events (`opened`, `synchronize`, `reopened`) whose base
branch is `main`, plus a `workflow_dispatch` re-check that takes the pull request
number as its `pr` input (see Version Bump below).

**Concurrency.** `group: version-guard-<pull request number>`,
`cancel-in-progress: false`.

## Version Bump (`version-bump.yml`)

The one-step fix the guard's warning offers. Commenting `/bump-version` on a
`main` pull request — from a person with write access, on a branch of this
repository rather than a fork — raises `versionCode` and `versionName` on that
branch (always above the latest release) and pushes the change. Any other
comment, or a comment from someone without write access, does nothing.

GitHub comments cannot carry a real button, so this command is the closest
equivalent; the guard's warning spells it out.

**Why it re-fires the guard.** A commit pushed with the built-in `GITHUB_TOKEN`
does **not** start workflow runs, so the guard would never see the new version on
its own. `workflow_dispatch` is one of the two events exempt from that rule, so
after a successful bump this workflow asks `version-guard.yml` to run again for
that pull request. Without it, the guard's check would stay red on an
already-bumped pull request.

## Auto Release (`auto-release.yml`)

Publishes the release when a merge to `main` actually changes application code.

**Trigger.** `push` events to `main` (i.e. merges). It runs on **every** such
push — path filtering is done inside the `guard` job, not at the trigger.

**Release (`release` job).** On a merge to `main` with code changes it:

1. Reads the app name and takes `versionName`/`versionCode` straight from
   `app/build.gradle.kts`. The version guard already forced the bump at
   pull-request time, so the committed values are the single source of truth and
   the release job never rewrites the version.
2. Validates them — `versionName` must match `^[0-9]+\.[0-9]+\.[0-9]+$` and
   `versionCode` must be a positive integer (`^[1-9][0-9]*$`).
3. Confirms the new version is newer than the latest `v*` release and that the
   `v<versionName>` tag does not already exist on the remote, failing hard
   (`exit 1`) rather than silently skipping.

It then resolves the release context, builds the signed release APK, and:

- **Renames** every produced APK to `<AppName>-v<VersionName>-<VersionCode>-<abi>.apk`
  (e.g. `EliteMemoPro-v1.0.12-12-arm64-v8a.apk`) — never a generic
  `app-release.apk`.
- **Verifies signatures** with the Android SDK `apksigner verify --verbose`,
  failing the job if any APK is unsigned or has an invalid signature.
- **Appends SHA-256 checksums** for the renamed APKs into the release notes.

Finally it publishes the release with the native GitHub CLI
(`gh release create "v<versionName>" … --notes-file /tmp/release-notes.md`,
authenticated with `GH_TOKEN`) — no third-party release action. The release notes
are built from the actual diff between the previous tag and the merge, with an
optional Copilot summary when `COPILOT_TOKEN` is configured.

**Permissions.** Least privilege: the workflow default is `contents: read`; only
the `release` job is elevated to `contents: write`.

**Concurrency.** `group: release-${{ github.ref }}` with
`cancel-in-progress: false`, so runs for the same ref never overlap.

## Mirror (`mirror.yml`)

Mirrors every branch and tag to GitLab and Codeberg on each push, as two
independent jobs, so the run page shows a green tick for the host that accepted
the push and a red cross for the one that did not.

Each host's push step is `continue-on-error`, so one host failing never fails the
run on its own. A final `report` job reads both results, writes a ✅/❌ table into
the run summary, and opens (or comments on) a single `bug` issue naming the host
that failed, with the ref, the commit, the run link and the last 200 lines of the
log. The run is green when at least one host accepted the push, and red only when
both failed.

It only ever touches the issue it opened itself — the same title, authored by
this bot — so it never comments on anything a person filed. Once both hosts are
syncing again it closes that issue with a short note, and because a closed issue
is not found by the lookup, a problem that comes back after being fixed opens a
fresh issue instead of commenting on the old one.

A force-push happens only when the remote refused a ref update because it
genuinely diverged (a non-fast-forward or an existing tag). An authentication,
network or pre-receive-hook failure is reported and never triggers a force-push,
which could rewrite or delete branches and tags on the mirror.

## Store screenshots (`store-screenshots.yml`)

Generates the store screenshots for Elite Memo Pro on a headless Android
emulator and uploads them as workflow artifacts.

**Trigger.** Manual only (`workflow_dispatch`) — start it from **Actions → Store
Screenshots → Run workflow** and choose the branch to run against. It never runs
on `push` or `pull_request`.

**What it does.**

1. Checks out the app from `dev` and the screenshot automation scripts from the
   `ci/screenshots` branch into `.ci/`.
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
- The app is always checked out from `dev`; the scripts come from the
  `ci/screenshots` branch. Re-running the workflow regenerates every screenshot.
- No repository secrets are required, and a run takes roughly 20 minutes.
