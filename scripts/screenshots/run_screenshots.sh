#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(pwd)"

echo "Waiting for emulator to boot..."
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
  sleep 3
done
adb shell input keyevent 82 >/dev/null 2>&1 || true

# Demo mode keeps the status bar clean and deterministic for store-ready captures.
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200 >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command network -e mobile hide >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false >/dev/null

# Disable animations.
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

# Suppress system ANR/crash dialogs so they never cover the app under test.
adb shell settings put global hide_error_dialogs 1

APK="$REPO_ROOT/app/build/outputs/apk/debug/app-debug.apk"
echo "Installing $APK"
adb install -r -t "$APK"

python3 "$SCRIPT_DIR/capture.py"
