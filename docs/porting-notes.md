# JCloisterZone Android porting notes

## Status

A real debug APK is buildable from `android-native/`, but the complete JCloisterZone Java engine has not yet been fully embedded into Android.

## Why this is not a one-step Java copy

- The user-provided `JCloisterZone-Client` repository is Electron/Nuxt/Vue JavaScript. It is a desktop client that downloads/uses `Engine.jar` and can connect to a remote engine.
- The Java code lives in `reference/JCloisterZone`, a Maven project with server/engine/platform classes.
- Android cannot run the desktop Electron/Vue client directly.
- This first milestone keeps the Android code Java-only and creates the native mobile shell, touch controls, and network connection plumbing without pretending full engine parity is already complete.

## Implemented Android controls

- Long press rotates the current tile preview/hook.
- Undo is a visible button.
- Pinch zooms the board.
- Drag pans the board.

## Implemented network compatibility work

- Raw TCP client for testing against a remote Java engine/socket bridge.
- Minimal RFC6455 text WebSocket client for compatibility experiments with desktop/client WebSocket endpoints.

## Next engine-port steps

1. Extract Android-compatible core packages from `reference/JCloisterZone/src/main/java/com/jcloisterzone` into an Android library/module.
2. Separate pure model/rules classes from desktop/server startup and filesystem/config assumptions.
3. Replace Maven-only dependency resolution with vendored Android-compatible jars or a Gradle project once Gradle/Maven tooling is available.
4. Add protocol-level tests using sample messages from `reference/JCloisterZone-Client/src`.
5. Replace the current board preview renderer with real tile artwork and state rendering from the engine model.
6. Validate online game connection against a running desktop engine/client on the LAN.

## Non-negotiable parity rule

Do not mark this project as a full direct port until local gameplay, scoring, expansions, save/load/protocol behaviour, and online connection have been tested against upstream behaviour.
