# Building Elite Memo Pro with Bazel

Gradle is gone — Bazel builds the whole project now.

You need **JDK 21**, an **Android SDK** (platform 37, build-tools) with
`ANDROID_HOME` set, and [Bazelisk](https://github.com/bazelbuild/bazelisk).

```bash
bazel build //...
bazel test //...

# Debug APK -> bazel-bin/app/app.apk
bazel build //app:app

# Release build: one APK per ABI plus a universal one
bazel build //app:app --config=release
```

| Module | Target | Rule |
|---|---|---|
| `:domain` | `//domain` | `kt_jvm_library` |
| `:core` | `//core` | `android_library` |
| `:data` | `//data` | `kt_android_library` (Room via KSP) |
| `:features` | `//features` | `android_library` |
| `:app` | `//app:app` | `android_binary` |

The app version lives in `version.bzl`. Dependencies are in `MODULE.bazel`.
