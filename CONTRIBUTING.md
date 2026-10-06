# Contributing to Elite Memo Pro

Thanks for taking the time to contribute. This document covers how work lands in
this repository and what a pull request has to satisfy.

## The one pull request target we accept

Elite Memo Pro ships from `main`, and the only pull request that may target
`main` is the release pull request **from `dev`**.

| Head branch | Base branch | Result |
| :--- | :--- | :--- |
| `dev` | `main` | Accepted â€” this is the release pipeline |
| anything else | anything else | Closed automatically |

Any pull request that is not `dev` â†’ `main` is closed by the
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

- **Debug & Release build** â€” `./gradlew assembleDebug` (and
  `assembleRelease` when the signing secrets are available).
- **Unit tests** â€” `./gradlew testDebugUnitTest`.
- **Android lint** â€” `./gradlew lintDebug`, with a warning count reported.
- **Static analysis** â€” `./gradlew detekt`, when detekt is configured.
- **Secret scan** â€” gitleaks.

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
merges, the release workflow reads the latest `vê`ˆÑ…œ°‘•É¥Ù•ÌÑ¡”¹•áĞ)Ù•ÉÍ¥½¸°‰Õ¥±‘Ì…¹Í¥¹ÌÑ¡”A-Ì°…¹ÁÕ‰±¥Í¡•ÌÑ¡”É•±•…Í”İ¥Ñ ¹½Ñ•Ì‘É…İ¸)™É½´Ñ¡”…ÑÕ…°‘¥™˜¸½¹ÑÉ¥‰ÕÑ½ÉÌ‘¼¹½Ğ¹••Ñ¼‰ÕµÀÙ•ÉÍ¥½¹½‘•€½È)Ù•ÉÍ¥½¹9…µ•€¸((ŒŒI•Á½ÉÑ¥¹œ‰ÕÌ…¹É•ÅÕ•ÍÑ¥¹œ™•…ÑÕÉ•Ì()UÍ”Ñ¡”¥ÍÍÕ”Ñ•µÁ±…Ñ•Ì¸½È…¹åÑ¡¥¹œÍ•ÕÉ¥ÑäµÉ•±…Ñ•°Á±•…Í”½Á•¸„ÁÉ¥Ù…Ñ”)É•Á½ÉĞÉ…Ñ¡•ÈÑ¡…¸„ÁÕ‰±¥Œ¥ÍÍÕ”¸(