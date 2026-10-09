# Pictoclip

A tiny clipboard hoarder for Android, dressed up like the Nintendo DS system menu / PictoChat.

- **A · Paste** – saves whatever is on your clipboard to the list (newest on top, duplicates move to the top).
- **Tap a clip** – selects it (blue corner brackets) and copies it back to your clipboard.
- **B · Delete** – deletes the selected clip. *Long-press* it to clear everything.
- **Share → Pictoclip** or **select text → Pictoclip** from any app saves text straight into the list.

Clips are stored on the device only (last 200). No permissions, no network, no dependencies beyond the Android framework.

## Build

```
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17+ and the Android SDK (platform 35). Every push also builds the APK in GitHub Actions
(download it from the run's *Artifacts*).

## Credits

Font: [DotGothic16](https://github.com/fontworks-fonts/DotGothic16), SIL Open Font License 1.1 (see `FONT-LICENSE-OFL.txt`).
Not affiliated with Nintendo.
