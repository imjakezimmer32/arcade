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
