# Claude Code task: JCloisterZone Android direct-port APK

Workspace: `W:/04_Software_Projects/Apps/JCloisterZone-Android-DirectPort`

You may edit only this workspace. Do not modify any sibling folders. Treat these local reference clones as read-only:

- `reference/JCloisterZone-Client` — upstream user-provided Electron/Vue client: https://github.com/farin/JCloisterZone-Client
- `reference/JCloisterZone` — upstream Java engine/platform: https://github.com/farin/JCloisterZone

## User goal

Create an Android APK project for a first release that is a direct port of JCloisterZone. Preserve original functionality exactly as much as practical, including ability to connect to other games. Keep Java as the implementation language. Do not convert the core to Kotlin.

Required first-release mobile controls:

- long press rotates the current tile
- Undo is a visible Android button
- pinch gestures zoom in/out of the board
- touch drag pans/navigates the board

Future features will be added later; do not redesign gameplay for this first release.

## Important discovery

The provided `JCloisterZone-Client` repo is Electron/Nuxt/Vue/JavaScript and uses/downloads `Engine.jar`. The Java source is in `reference/JCloisterZone`.

## Preferred implementation direction

Create a normal Android build if feasible, Java language, with Gradle wrapper if possible. Use Java source from `reference/JCloisterZone` where Android-compatible. If full engine parity cannot be completed in one pass due to desktop/server dependencies, isolate the incompatible parts and write `docs/porting-notes.md` with exact classes/packages and next steps. Do not fake gameplay or network compatibility.

## Deliverables

1. Android project files capable of building a debug APK locally and in GitHub Actions, or a precise blocker if tooling/dependencies prevent this.
2. Java Android UI entry point with board surface/view and touch handling wired for long-press rotate, undo button, pinch zoom, and pan.
3. Ported/integrated game/client networking architecture preserving ability to connect to other games where feasible.
4. README updated with build instructions, current port status, and honest limitations.
5. GitHub Actions workflow to build the debug APK artifact.

## Verification commands to aim for

From the workspace root:

- `./gradlew.bat assembleDebug` on Windows/Git Bash if using Gradle wrapper, or the actual build command you create.
- `git status --short`

Do not claim APK built unless a real APK exists.

## Reporting requirements

At the end, report changed/created files, exact commands run and outcomes, APK path if produced, and blockers/limitations.
