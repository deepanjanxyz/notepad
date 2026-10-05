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
| [`auto-release.yml`](./auto-release.yml) | Auto Release | `pull_request` / `push` to `main` | Guards on real application-code changes, auto-bumps the version with a companion PR when a `main` PR skipped it, and on merge to `main` publishes a GitHub release tagged `v<versionName>` with the signed APK and traced release notes. |

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
changed. It bumps the version when a `main` PR forgot to, and publishes a GitHub
Release when the change lands.

**Trigger.** `pull_request` events (`opened`, `synchronize`, `reopened`) whose
base branch is `main`, and `push` events to `main` (i.e. merges). The workflow
runs on **every** such event — path filtering is done inside the `guard` job, not
at the trigger, so a docs-only PR never leaves a required status check waiting
forever. Non-code changes simply skip the bot jobs (see `code_changed` below).

**Concurrency.** Two layers, both non-cancelling:

- Workflow level: `group: release-${{ github.ref }}`, so runs for the same ref
  never overlap.
- The `auto-bump` job additionally pins a strict global group `auto-bump-main`
  (`cancel-in-progress: false`), so two different PRs targeting `main` can never
  race each other into creating conflicting bump PRs.

**Guard (`guard` job).** Runs on both events and computes two facts used by the
later jobs:

- `code_changed` — a `git diff` against the base confirms that application code
  actually changed. Only paths under `app/src/main/`, or the root Gradle
  configuration files (`*.gradle.kts`, `gradle.properties`), count. Changes under
  `app/src/test/` and `app/src/androidTest/` (and docs/images) do not. If nothing
  matches, every bot step is skipped — no version check, no bot PR, no release.
- `version_updated` (pull requests only) — whether `versionCode` and
  `versionName` in `app/build.gradle.kts` differ from `main`.

**Auto-bump (`auto-bump` job).** On a `main` PR that changes code but leaves the
version untouched — and was not opened by `github-actions[bot]` — it first
validates that `versionName` is strict semantic versioning
(`^[0-9]+\.[0-9]+\.[0-9]+$`). If it is not (e.g. an alpha/beta suffix), the
arithmetic bump is skipped with a warning instead of risking a malformed version.
Otherwise it creates a **per-PR** branch `auto/bump-version-pr-<PR number>`,
increments `versionCode` by 1, bumps the patch component of `versionName`, and
opens a companion PR to `main` titled `chore(release): bump version code [skip ci]`.
Because the branch is unique to the source PR and is pushed with a plain
`git push` (never a force-push), parallel PRs cannot collide or corrupt history.
If a bump PR for this source PR is already open, it does nothing; the global
`auto-bump-main` lock still serialises runs.

**Release (`release` job).** On a merge to `main` with code changes it runs a
strict pre-flight before doing anything else:

1. `versionName` must match `^[0-9]+\.[0-9]+\.[0-9]+$`; otherwise the job fails
   with `exit 1`.
2. `versionCode` must be a positive integer (`^[1-9][0-9]*$`); if it is missing or
   malformed, the job fails with `exit 1`.
3. The `v<versionName>` tag must not already exist on the remote; if it does, the
   job fails hard (`exit 1`) rather than silently skipping.

Only after those pass does it resolve the release context, build the signed
release APK, and publish a GitHub Release using the native GitHub CLI
(`gh release create`) — tag `v<versionName>`, title `v<versionName>`, the notes
read from a generated notes file, and the signed APK attached as a release asset.
For the release notes it prefers the merged PR's title and body; if the tip commit
is the bot's bump commit, it walks `git log` back to the last human commit and
uses that contributor's PR/commit context instead.

**Permissions.** Least privilege: the workflow default is `contents: read`. Only
the `auto-bump` job is elevated to `contents: write` + `pull-requests: write`, and
only the `release` job to `contents: write`.
