# Contributing to Elite Memo Pro

Thanks for taking the time to improve Elite Memo Pro. This document explains how
changes are proposed, reviewed, and released, so your contribution lands quickly.

## Where to open a pull request

The project uses two long-lived branches:

- **`dev`** — the integration branch. **All feature, fix, and documentation pull
  requests target `dev`.** Every PR opened against `dev` runs the
  [Universal PR Check](#what-ci-runs) automatically.
- **`main`** — the release branch. It only receives changes that have already
  been verified on `dev`, promoted through a `dev` → `main` pull request.

Do not open a pull request directly against `main` for ordinary work. Changes
flow `dev` → `main`, and the release pipeline handles versioning and publishing
from there.

## Getting set up

### Prerequisites

- **JDK 21** (Temurin recommended).
- **Android SDK** with platform **37** and build-tools **36.0.0**.
- The Gradle wrapper is bundled — you do not need a separate Gradle install.

### Build and test

```bash
git clone https://github.com/deepanjanxyz/notepad.git
cd notepad

./gradlew assembleDebug     # build the debug APK
./gradlew test              # run the JVM unit tests
./gradlew installDebug      # install on a connected device or emulator
```

Release builds require signing secrets and are **not** needed for day-to-day
development; debug builds work without them. See the *Release signing* section of
the [README](README.md) if you do need to build a release variant locally.

## Making a change

1. Fork the repository (or branch off `dev` if you have write access).
2. Create a focused branch off `dev`, e.g. `fix/reminder-notification` or
   `feature/label-rename`.
3. Make your change and make sure `./gradlew test` passes locally.
4. Open a pull request **against `dev`**, with a clear title and a short
   description of what changed and why.
5. The Universal PR Check runs automatically and posts a status table on the PR.
   A pull request is ready to merge once every check is green and a maintainer
   has approved it.

## What CI runs

Every pull request against `dev` runs [`universal-pr-check.yml`](.github/workflows/universal-pr-check.yml)
as parallel jobs, reported back as a single status table:

| Check | What it does |
| :--- | :--- |
| Debug & Release build | Assembles the debug and release variants. |
| Unit tests | Runs `./gradlew testDebugUnitTest`. |
| Android lint | Runs `./gradlew lintDebug`. |
| Static analysis (detekt) | Runs only when detekt is configured. |
| Secret scan | Scans the checkout for hardcoded secrets with gitleaks. |

A plain `push` to `dev` or `main` also runs [`ci.yml`](.github/workflows/ci.yml)
(unit tests plus a debug build). Please keep both green.

## Conventions

- **Keep the domain module Android-free.** `:domain` is pure Kotlin/JVM and must
  not gain Android dependencies.
- **One use case per operation.** Prefer a single, focused use case under
  `domain/usecase/` over a large multi-purpose one.
- **Never commit secrets.** `app/keystore.jks`, `.env`, and `local.properties`
  are git-ignored — keep it that way.
- **Match the existing style.** The codebase uses Kotlin with Jetpack Compose and
  Material 3; follow the surrounding structure and naming.
- **Keep pull requests focused.** One logical change per PR is easier to review
  and to revert.

## Commit messages

Write a short, imperative subject line. Where it helps, follow the existing
convention of a type prefix, for example:

```
fix: restore reminder notification after reboot
docs: add security policy
chore(release): bump version [skip ci]
```

## Reporting bugs and requesting features

Use the issue templates under **New issue**. For anything security-related,
follow [SECURITY.md](SECURITY.md) instead of opening a public issue.

## For maintainers

Review on `main` pull requests can be driven with slash commands:
`/review`, `/test`, `/force-review`, `/add-reviewer <user>`, and
`/remove-approve-user <user>`.
