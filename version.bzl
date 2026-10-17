"""Single source of truth for the app version.

The version used to live in `app/build.gradle.kts`. Now that the build runs on
Bazel we keep it here instead, so the BUILD file and the release automation can
read exactly one place.
"""

VERSION_CODE = 20
VERSION_NAME = "1.0.20"
