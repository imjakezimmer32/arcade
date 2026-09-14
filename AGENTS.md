# Arcade

Kotlin Jetpack Compose night-garden arcade. Cabinets: Snake, Mines, Breakout, Stacks, 2048, Pong, Invaders, Flit, Memory, Dodge, Checkers, Chess, Four, Flip, Hop, Echo, Rocks, Peck, Catch, Slide, Boxes.

## Layout

- `app/` — Android (Kotlin / Jetpack Compose). Unchanged primary app.
- `web/` — computer/browser version (Vite + TypeScript), same cabinets and night-garden look
- `app/src/main/java/app/snake/` — shell, theme, nav, games, scores
- Build output lives in `%LOCALAPPDATA%/SnakeBuild` (or `.local-build`)

## Android

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat installDebug
adb shell am start -n app.snake/.MainActivity
```

## Web (computer)

```bash
cd web
npm install
npm run dev
```

Open the printed local URL. Production build: `npm run build` → `web/dist`.
Scores store in the browser (`localStorage`).
