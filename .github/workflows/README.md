# GitHub Actions workflows

This directory holds every CI/CD workflow for Elite Memo Pro. Each file has a
single responsibility, so it is easy to see what runs, when, and why.

## Index — which file does which job

| Workflow file | Name | Trigger | What it does |
|---|---|---|---|
| [`ci.yml`](./ci.yml) | CI | `push` / `pull_request` to `dev` and `main`, plus manual | Runs the unit tests and a debug build, then uploads the test reports. This is the build-verification gate. |
| [`android.yml`](./android.yml) | Build and Sign APK | manual (`workflow_dispatch`) | Builds the unsigned release APK, decodes the keystore from secrets, signs the APK, and uploads it as the `release-apk` artifact. |
| [`mirror.yml`](./mirror.yml) | Multi-Platform Smart Auto Mirroring | `push` of any branch or tag | Materializes every branch locally, then mirrors all branches and tags to GitLab and Codeberg (force-syncing when a normal push is rejected). |
| [`universal-pr-check.yml`](./universal-pr-check.yml) | Universal PR Check | `pull_request` (opened, synchronize, reopened) to any branch | Builds the debug app and runs unit tests on JDK 17, reacts with a thumbs up on success, and on failure posts the filtered error snippet plus the run link and blocks the merge. |

## In one line each

- **Tests / verification** → `ci.yml`
- **Release build + signing** → `android.yml`
- **Mirroring (push) to other hosts (GitLab, Codeberg)** → `mirror.yml`
- **Per-PR build/test gate with in-PR feedback** → `universal-pr-check.yml`

## Required repository secrets

| Secret | Used by | Purpose |
|---|---|---|
| `KEYSTORE_BASE64` | `android.yml` | Base64-encoded release keystore. |
| `KEYSTORE_PASSWORD` | `android.yml` | Keystore password. |
| `KEY_ALIAS` | `android.yml` | Key alias inside the keystore. |
| `KEY_PASSWORD` | `android.yml` | Key password. |
| `GITLAB_TOKEN` | `mirror.yml` | Push access to the GitLab mirror. |
| `CODEBERG_TOKEN` | `mirror.yml` | Push access to the Codeberg mirror. |

## Universal PR Check (`universal-pr-check.yml`)

A single, branch-agnostic gate that runs on every pull request, so reviewers get
an immediate, in-PR signal about whether a change builds and passes its tests.

**Purpose.** Give each pull request a fast, self-contained verdict without
touching any branch directly. The check runs purely on PR activity, reports its
result back onto the PR itself, and blocks the merge button when the build or
tests fail.

**Trigger.** `pull_request` events only, of type `opened`, `synchronize`, and
`reopened`, with `branches: ['*']` so it applies to PRs targeting any branch. It
deliberately does not run on plain `push`, so merges and direct pushes are not
re-checked here — this workflow exists purely to guard pull requests.

**Environment.** The job runs on `ubuntu-latest`, checks out the repository with
full history (`fetch-depth: 0`), sets up Temurin JDK 17, and configures Gradle
caching via `gradle/actions/setup-gradle`. The JDK is pinned to **17** on
purpose: the project compiles against `sourceCompatibility`/`targetCompatibility`
17 and must not be silently upgraded to a newer JDK. The Android SDK platform the
project builds against (`compileSdk`) is installed before the build. It then runs
`./gradlew assembleDebug testDebugUnitTest --no-daemon`.

**Permissions.** `contents: read`, `pull-requests: write`, and `issues: write` —
just enough to read the code and write feedback back onto the pull request.

**On success.** A thumbs up (`+1`) reaction is added to the pull request, giving
a one-glance confirmation that the change builds and passes unit tests.

**On failure.** The workflow filters the build output for error/exception lines,
posts a comment on the pull request containing that snippet and a direct link to
the failing Action run, and then fails the job with a non-zero exit code so the
merge button stays blocked until the problem is fixed.
