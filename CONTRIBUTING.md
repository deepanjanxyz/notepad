# Contributing to Elite Memo Pro

Thanks for taking the time to contribute. This document covers how work lands in
this repository and what a pull request has to satisfy.

## The one pull request target we accept

Elite Memo Pro ships from `main`, and the only pull request that may target
`main` is the release pull request **from `dev`**.

| Head branch | Base branch | Result |
| :--- | :--- | :--- |
| `dev` | `main` | Accepted — this is the release pipeline |
| anything else | anything else | Closed automatically |

Any pull request that is not `dev` → `main` is closed by the
[PR Manager](.github/workflows/pr-manager.yml) workflow, which leaves a comment
explaining why. This is not a judgement on the change; it is how the repository
keeps `main` release-only.

So the workflow is:

1. Land your work on `dev`.
2. Once `dev` is ready to ship, open a pull request **from `dev` into `main`**.
3. The full check suite runs on that pull request (see below).
4. When it is green and reviewed, the merge produces a release automatically.

## What the check suite runs

A valid pull request runs these in parallel:

- **Debug & Release build** — `./gradlew assembleDebug` (and
  `assembleRelease` when the signing secrets are available).
- **Unit tests** — `./gradlew testDebugUnitTest`.
- **Android lint** — `./gradlew lintDebug`, with a warning count reported.
- **Static analysis** — `./gradlew detekt`, when detekt is configured.
- **Secret scan** — gitleaks.

The suite posts a single summary comment on the pull request and updates it in
place on each push.

## Building locally

You need JDK 17 and the Android SDK (platform 37, build-tools 36.0.0).

```bash
./gradlew assembleDebug          # build the debug APK
./gradlew testDebugUnitTest      # run the unit tests
./gradlew lintDebug              # run Android lint
```

Release builds are signed with a keystore supplied through CI secrets, so a
local `assembleRelease` will not be signed unless you provide your own.

## Releases

Releases are cut automatically. When a pull request from `dev` into `main`
merges, the release workflow reads the latest `v*` tag, derives the next
version, builds and signs the APKs, and publishes the release with notes drawn
from the actual diff. Contributors do not need to bump `versionCode` or
`versionName`.

## Reporting bugs and requesting features

Use the issue templates. For anything security-related, please open a private
report rather than a public issue.
