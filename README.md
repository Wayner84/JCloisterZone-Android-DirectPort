# JCloisterZone Android Direct Port

Android/APK port workspace for JCloisterZone.

First release goal: direct mobile port of the existing JCloisterZone behaviour, preserving original functionality including connection to other games/engines.

Required mobile controls:

- Long press: rotate tile
- Undo: visible Android button
- Pinch: zoom in/out
- Drag: pan/navigate board

Source references used locally:

- `reference/JCloisterZone-Client` (ignored): upstream Electron/Vue client from https://github.com/farin/JCloisterZone-Client
- `reference/JCloisterZone` (ignored): upstream Java engine/platform from https://github.com/farin/JCloisterZone

The reference folders are intentionally ignored rather than committed as nested Git repos. Ported/copied code must preserve upstream license headers and GPL licensing.
