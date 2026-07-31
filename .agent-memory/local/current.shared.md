# Shared State — Bootstrap

Last updated: 2026-07-31. Everything below verified in a live session.

## Maestro E2E — verified working chain

1. **Device:** physical Android phone (Nothing, serial `00153157O001730`) over USB.
   `adb devices` shows `device`. Minimal ADB & Fastboot is the adb provider
   (`C:\Program Files (x86)\Minimal ADB and Fastboot\adb.exe`), fine for Maestro.
2. **Backend:** runs ON the phone in Termux. Start remotely from harness:
   ```bash
   adb shell monkey -p com.termux -c android.intent.category.LAUNCHER 1   # Termux to foreground
   adb shell input text "clamit" && adb shell input keyevent 66           # type + ENTER
   ```
   Verify: `adb exec-out screencap -p > termux.png` shows
   `clamit server starting on :8080 (db: ./clamit.db)`.
   App connects to `http://127.0.0.1:8080/` (hardcoded in `android/app/src/main/java/com/clamit/data/api/ApiClient.kt`).
3. **Run a flow** (harness shell predates setx → full path + inline JAVA_HOME):
   ```bash
   set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot" && "C:\workspace\maestro\bin\maestro.bat" test "C:\Users\alipc\maestro-flows\smoke.yaml"
   ```
4. **Current app state:** schedule screen shows empty state
   `"Bu gün için zaman bloğu yok."` / `"Butonuna basarak blok ekleyin."`
   (backend up, no schedule data). These texts are stable selectors.

## Open decisions / next steps

- Not decided: whether to wire Maestro MCP into the user's own Claude Code
  (`claude mcp add -s project maestro -- maestro mcp`) — optional, for the
  user's personal terminal; harness drives via CLI.
- Not done: real data flows (add schedule block → assert → delete). Backend DB
  is empty (`./clamit.db` in Termux).
