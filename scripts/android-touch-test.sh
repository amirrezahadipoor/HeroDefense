#!/usr/bin/env bash
set -uo pipefail

adb logcat -c
# Suppress Android's first-launch immersive-mode education overlay so touch evidence
# captures the app itself; gameplay interaction remains entirely touch-driven.
adb shell settings put secure immersive_mode_confirmations confirmed
set +e
./scripts/gradle.sh :android:connectedDebugAndroidTest --stacktrace --info
status=$?
set -e
mkdir -p android/build/reports/androidTests/diagnostics
adb logcat -d > android/build/reports/androidTests/diagnostics/emulator-logcat.txt || true
exit "$status"
