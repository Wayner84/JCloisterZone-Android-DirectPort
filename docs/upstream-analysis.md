# Upstream analysis

- User-provided repo: `farin/JCloisterZone-Client`, cloned locally under `reference/JCloisterZone-Client`.
- The client repo is Electron/Nuxt/Vue/JavaScript, not Java. It downloads/uses an external `Engine.jar` for game logic and can connect to a remote engine via socket config.
- Main Java source is in `farin/JCloisterZone`, cloned locally under `reference/JCloisterZone`. It is Maven-based and contains the Java engine/platform source.
- Therefore the Android direct port should use the Java engine/platform source as the core and the client repo as UI/network behaviour reference.
