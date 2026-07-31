# Module: maestro-e2e

Maestro mobile UI testing on this machine, against the clamit Android app.
All facts verified 2026-07-31 in a live session (see `sessions/2026-07-31.md`).

## Environment (the "what works" map)

| Piece | Working setup | Gotcha |
|-------|---------------|--------|
| Test device | Physical Android phone (Nothing, serial `00153157O001730`), USB | Emulator NOT usable — PC shuts down (i3-12100F, no iGPU). Physical device needs only ADB, no Android Studio |
| adb | Minimal ADB & Fastboot (`C:\Program Files (x86)\Minimal ADB and Fastboot\adb.exe`), on PATH | Maestro finds adb via PATH; minimal adb is sufficient |
| Maestro CLI | 2.7.0 at `C:\workspace\maestro\bin\maestro.bat` (user PATH entry: `C:\workspace/maestro/bin`) | NOT `C:\maestro`. Harness Git-Bash shell predates `setx` → use full path + inline JAVA_HOME |
| Java | Temurin 17 at `C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot` | Maestro launcher hard-fails with "JAVA_HOME is set to an invalid directory" if JAVA_HOME is wrong |
| App under test | `com.clamit`, debug APK from continuous release (`https://github.com/rbb11dot/clamit/releases/download/continuous/app-debug.apk`), installed via `adb install -r` | — |
| Backend | Go server runs ON THE PHONE in Termux (`clamit`, `:8080`, `./clamit.db`) | App hardcodes `http://127.0.0.1:8080/` in `ApiClient.kt` — backend on the PC is NOT reachable from the phone |
| Flow files | `C:\Users\alipc\maestro-flows\` (`smoke.yaml`) | — |
| Test artifacts | `C:\Users\alipc\.maestro\tests\<ts>\<flow>\takeScreenshot\*.png`, plus `maestro.log`, `commands.json`, `logs/device-logcat.txt` | Screenshots land in `.maestro\tests`, NOT the flow dir |

## Verified commands

```bash
# boot backend on phone (from harness)
adb shell monkey -p com.termux -c android.intent.category.LAUNCHER 1
adb shell input text "clamit" && adb shell input keyevent 66

# verify app foreground / backend connect
adb shell dumpsys window | grep mCurrentFocus
adb exec-out screencap -p > "$TEMP/shot.png"

# run flow
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot" && "C:\workspace\maestro\bin\maestro.bat" test "C:\Users\alipc\maestro-flows\smoke.yaml"

# dump view hierarchy (cp1254-encoded JSON on Windows!)
set "JAVA_HOME=..." && "C:\workspace\maestro\bin\maestro.bat" hierarchy > h.json
python -c "import json; d=json.loads(open('h.json','rb').read().decode('cp1254'))"
```

## UI state today

- Schedule screen, backend up, DB empty:
  `"Bu gün için zaman bloğu yok."` + `"Butonuna basarak blok ekleyin."` + date/`Cuma`.
- Compose app: app content has NO resource-ids in the hierarchy → text-based
  selectors only (`assertVisible`/`tapOn` by visible text).

## Known non-goals

- No emulator on this machine (PC shutdown), no iOS (Windows).
- No MCP wiring yet — harness drives Maestro via CLI. Optional for user's own
  Claude Code: `claude mcp add -s project maestro -- maestro mcp`.
