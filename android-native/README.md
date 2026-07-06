# Android native direct-port milestone

This folder contains the first installable Java Android APK milestone for the JCloisterZone direct-port project.

It is dependency-light because this Windows machine currently has Android SDK tools and Java, but no system Gradle or Maven. The build uses `aapt2`, `javac`, `d8`, `zipalign`, and `apksigner` directly.

## Build

```sh
./android-native/build.sh
```

Output:

```text
android-native/dist/JCloisterZone-Android-v0.1.0-debug.apk
```

## Current implementation

Implemented in Java:

- Android `MainActivity`.
- Custom board view with touch drag/pan.
- Pinch zoom.
- Long press rotates the current tile preview/hook.
- Visible Undo button.
- Raw TCP connection mode for remote Java engine testing.
- Minimal WebSocket client mode for desktop/client socket compatibility testing.
- Log panel for incoming/outgoing protocol messages.

## Important limitation

This APK is not yet complete gameplay parity. The upstream Java game engine is still being assessed for Android-compatible extraction. The app exposes the mobile controls and network connection plumbing needed for the port, but full tile rules/scoring/local engine integration remains tracked in `docs/porting-notes.md`.
