#!/usr/bin/env bash
#
# Cloud Agent install script for the Arcade Android app.
#
# Idempotent bootstrap that provisions the Android SDK build toolchain and
# warms the Gradle/Kotlin caches by producing the debug APK. Safe to re-run:
# already-installed SDK packages are skipped and generated files are rewritten
# deterministically.
set -euo pipefail

# ---- Configuration ---------------------------------------------------------
ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
CMDLINE_TOOLS_VERSION="commandlinetools-linux-15859902_latest.zip"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/${CMDLINE_TOOLS_VERSION}"
PLATFORM="platforms;android-35"
BUILD_TOOLS="build-tools;35.0.0"

export ANDROID_HOME
export ANDROID_SDK_ROOT="$ANDROID_HOME"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

echo "==> Using ANDROID_HOME=$ANDROID_HOME"

# ---- 1. Android command-line tools ----------------------------------------
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
if [ ! -x "$SDKMANAGER" ]; then
  echo "==> Installing Android command-line tools"
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  tmp_zip="$(mktemp --suffix=.zip)"
  tmp_dir="$(mktemp -d)"
  curl -fSL -o "$tmp_zip" "$CMDLINE_TOOLS_URL"
  unzip -q "$tmp_zip" -d "$tmp_dir"
  rm -rf "$ANDROID_HOME/cmdline-tools/latest"
  mv "$tmp_dir/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
  rm -rf "$tmp_zip" "$tmp_dir"
else
  echo "==> Android command-line tools already present"
fi

# ---- 2. SDK packages + licenses -------------------------------------------
echo "==> Accepting SDK licenses"
yes | "$SDKMANAGER" --licenses >/dev/null 2>&1 || true

echo "==> Installing SDK packages (idempotent)"
"$SDKMANAGER" "platform-tools" "$PLATFORM" "$BUILD_TOOLS"

# ---- 3. Point Gradle at the SDK -------------------------------------------
echo "==> Writing local.properties"
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > "$REPO_ROOT/local.properties"

# ---- 4. Warm caches + verify the toolchain --------------------------------
echo "==> Building debug APK (warms caches and verifies the toolchain)"
./gradlew :app:assembleDebug --no-daemon

echo "==> Install complete. Debug APK:"
find "$REPO_ROOT/.local-build" -name 'app-debug.apk' -print 2>/dev/null || true
