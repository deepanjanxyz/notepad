# Building Elite Memo Pro with Bazel

This project is built with **Bazel** instead of Gradle. The Gradle build
(`gradlew`, `*.gradle.kts`, `settings.gradle.kts`, `gradle/`) is gone — Bazel is
now the only build system.

## What you need

- **JDK 21** (the same version the app compiles against).
- **Android SDK** with platform **37** and build-tools, pointed at by the
  `ANDROID_HOME` environment variable. rules_android picks the SDK up from there.
- **Bazel** — use [Bazelisk](https://github.com/bazelbuild/bazelisk), which reads
  the version you want automatically. Any recent Bazel 7/8/9 works.

## Everyday commands

```bash
# Everything: build and test.
bazel build //...
bazel test //...

# The debug APK.
bazel build //app:app

# A release build — one APK per ABI plus a universal one.
bazel build //app:app --config=release

# Just one ABI.
bazel build //app:app --android_platforms=//:arm64_v8a
```

Bazel writes its outputs under `bazel-bin/`. The APK for `//app:app` is
`bazel-bin/app/app.apk`.

## How the modules map

| Gradle module | Bazel target | Rule |
|---|---|---|
| `:domain` | `//domain` | `kt_jvm_library` (plain Kotlin/JVM) |
| `:core` | `//core` | `android_library` (manifest-only today) |
| `:data` | `//data` | `kt_android_library` + Room via `kt_ksp_plugin` |
| `:features` | `//features` | `android_library` (manifest-only today) |
| `:app` | `//app:app_lib` and `//app:app` | `kt_android_library` + `android_binary` |

The app's unit tests are `//app:app_test` (a `kt_android_local_test`).

## Where the version lives

Gradle kept `versionCode`/`versionName` in `app/build.gradle.kts`. They now live
in **`version.bzl`** at the repo root — one place, read by both `//app:app` and
the release automation.

```starlark
VERSION_CODE = 20
VERSION_NAME = "1.0.20"
```

## Configuration files

- `MODULE.bazel` — the rules, the Android SDK wiring, and every Maven dependency
  (one `maven.install`). This replaces `build.gradle.kts` + `settings.gradle.kts`.
- `.bazelrc` — Java 21, and the `--config=release` ABI platforms.
- `BUILD.bazel` (root) — the Kotlin toolchain, the Compose compiler plugin, and
  the four ABI platforms.
- `version.bzl` — the app version.

## Notes and known gaps

A few things are genuinely different from the Gradle build. They are called out
here rather than hidden:

- **Compose** is enabled by attaching `//:jetpack_compose_compiler_plugin` to a
  target's `plugins`. There is no `compose = true` flag in Bazel.
- **Room** runs through KSP (`kt_ksp_plugin` in `data/BUILD.bazel`). The old
  Gradle build also *exported* Room schema JSON into `data/schemas`; that export
  is not wired up under Bazel yet (Bazel's sandbox fights writing into a source
  directory). No schema files were committed, so nothing is lost — it just needs
  a decision before the next migration is authored.
- **Release signing** has no first-class attribute on `android_binary`, so the
  APKs are signed after the build with `apksigner`, exactly as the old workflow
  already did.
- **ABI splits** are one build per `--android_platforms` value (or all four at
  once with `--config=release`), rather than Gradle's single `splits {}` block.
- **Android Lint and Detekt** have no first-party Bazel rule. The `detekt.yml`
  config is still in `config/detekt/`, but the Gradle Detekt task is gone.
- **`compileSdk`** is implicit: it is whatever `android.jar` the SDK toolchain
  exposes (the highest installed API level). `minSdk`/`targetSdk` are set per
  binary in `manifest_values`.
