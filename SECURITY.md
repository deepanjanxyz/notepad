# Security Policy

Elite Memo Pro is a local-first Android notes app. This document explains how to
report a security issue and what to expect in return.

## Supported versions

Security fixes land on the [`main`](https://github.com/deepanjanxyz/notepad/tree/main)
branch and ship in the next tagged release. Only the latest release is supported;
older releases are not maintained.

| Version | Supported |
| :--- | :---: |
| Latest release | ✅ |
| Older releases | ❌ |

## Reporting a vulnerability

Please report suspected vulnerabilities **privately** — do not open a public
issue, pull request, or discussion for a security problem.

The preferred channel is GitHub's private vulnerability reporting:

1. Open the repository's [Security tab](https://github.com/deepanjanxyz/notepad/security).
2. Click **Report a vulnerability**.
3. Describe the issue in as much detail as you can (see below).

This opens a private advisory that only you and the maintainers can see, and
gives us a place to investigate, fix, and coordinate disclosure before anything
is made public.

If you are unable to use private reporting, contact the maintainer directly on
GitHub: **Deepanjan Biswas ([@deepanjanxyz](https://github.com/deepanjanxyz))**. 

### What to include

- A clear description of the vulnerability and its impact.
- Steps to reproduce it, or a proof of concept.
- The affected version (release tag / `versionName`) and the device and Android
  version you tested on.
- A suggested fix or mitigation, if you have one.

### What to expect

- An acknowledgement of your report within a few days.
- An assessment of the issue and, where warranted, a fix in a subsequent release.
- Credit in the release notes if you would like it.

## Scope

**In scope**

- The Android application in this repository, and the APKs published under
  [Releases](https://github.com/deepanjanxyz/notepad/releases).
- The release, signing, and CI/CD configuration under `.github/workflows/`.

**Out of scope**

- Vulnerabilities in third-party dependencies — please report those upstream.
  (We still welcome a heads-up if they affect this app.)
- Anything that requires a rooted or otherwise already-compromised device.
- Social-engineering or physical-access attacks.

## Security model — what to know

Elite Memo Pro is deliberately offline and local-first:

- The app does **not** request the `INTERNET` permission, so it cannot open a
  network connection or transmit data. See [PRIVACY.md](PRIVACY.md) for the full
  details.
- Notes, labels, and settings are stored in app-private storage (a Room database
  and DataStore) and are not encrypted at rest by the app itself.
- Release APKs are built and signed from CI with credentials held in GitHub
  Secrets; keystores are never committed to the repository.
