# GitHub Actions workflows

This directory holds every CI/CD workflow for Elite Memo Pro. Each file has a
single responsibility, so it is easy to see what runs, when, and why.

## Index — which file does which job

| Workflow file | Name | Trigger | What it does |
|---|---|---|---|
| [`ci.yml`](./ci.yml) | CI | `push` / `pull_request` to `dev` and `main`, plus manual | Runs the unit tests and a debug build, then uploads the test reports. This is the build-verification gate. |
| [`android.yml`](./android.yml) | Build and Sign APK | manual (`workflow_dispatch`) | Builds the unsigned release APK, decodes the keystore from secrets, signs the APK, and uploads it as the `release-apk` artifact. |
| [`mirror.yml`](./mirror.yml) | Multi-Platform Smart Auto Mirroring | `push` of any branch or tag | Materializes every branch locally, then mirrors all branches and tags to GitLab and Codeberg (force-syncing when a normal push is rejected). |

## In one line each

- **Tests / verification** → `ci.yml`
- **Release build + signing** → `android.yml`
- **Mirroring (push) to other hosts (GitLab, Codeberg)** → `mirror.yml`

## Required repository secrets

| Secret | Used by | Purpose |
|---|---|---|
| `KEYSTORE_BASE64` | `android.yml` | Base64-encoded release keystore. |
| `KEYSTORE_PASSWORD` | `android.yml` | Keystore password. |
| `KEY_ALIAS` | `android.yml` | Key alias inside the keystore. |
| `KEY_PASSWORD` | `android.yml` | Key password. |
| `GITLAB_TOKEN` | `mirror.yml` | Push access to the GitLab mirror. |
| `CODEBERG_TOKEN` | `mirror.yml` | Push access to the Codeberg mirror. |
