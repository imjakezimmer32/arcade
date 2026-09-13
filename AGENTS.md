# Arcade

Kotlin Jetpack Compose night-garden arcade. Cabinets: Snake, Mines, Breakout, Stacks, 2048, Pong, Invaders, Flit, Memory, Dodge, Checkers, Chess, Four, Flip, Hop, Echo, Rocks, Peck, Catch, Slide, Boxes. Install with `gradlew installDebug`.

## Layout

- `app/src/main/java/app/snake/` — shell, theme, nav
- `games/` — each cabinet's engine, ViewModel, screen
- `scores/` — write-in high scores
- Build output lives in `%LOCALAPPDATA%/SnakeBuild`

## Device

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat installDebug
adb shell am start -n app.snake/.MainActivity
```

## Linux / Cloud Agent

The Cloud Agent environment provisions the Android SDK (platform-tools,
`platforms;android-35`, `build-tools;35.0.0`) and warms the build via
`bash .cursor/install.sh`. Build and lint with the POSIX wrapper:

```bash
./gradlew :app:assembleDebug   # APK -> .local-build/app/outputs/apk/debug/app-debug.apk
./gradlew :app:lintDebug
```

`installDebug` and launching the app require a device/emulator. The Android
emulator does **not** run in the Cloud Agent VM: x86_64 images need hardware
acceleration, but nested KVM crashes the host (`kernel BUG ... kvm_spurious_fault`
on vCPU create), and the emulator cannot run ARM images on an x86 host. Use a
local machine or physical device for on-device testing.
