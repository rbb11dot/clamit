# Shared State — Bootstrap

Last updated: 2026-07-31 (evening). Everything below verified in live sessions.

## Maestro E2E — verified working chain

1. **Device:** physical Android phone (Nothing, serial `00153157O001730`) over USB.
   Minimal ADB & Fastboot is the adb provider
   (`C:\Program Files (x86)\Minimal ADB and Fastboot\adb.exe`). **NOTE: user
   unplugged the phone — no ADB until they return.**
2. **Backend:** runs ON the phone in Termux. Start remotely from harness:
   ```bash
   adb shell monkey -p com.termux -c android.intent.category.LAUNCHER 1   # Termux to foreground
   adb shell input text "clamit" && adb shell input keyevent 66           # type + ENTER
   ```
   Verify: `adb exec-out screencap -p > termux.png` shows
   `clamit server starting on :8080 (db: ./clamit.db)`.
   App base URL: `BuildConfig.API_BASE_URL` (default `http://127.0.0.1:8080/`,
   override with `-PapiBaseUrl=...` in gradle).
3. **Run a flow** (harness shell predates setx → full path + inline JAVA_HOME):
   ```bash
   set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot" && "C:\workspace\maestro\bin\maestro.bat" test "C:\Users\alipc\maestro-flows\smoke.yaml"
   ```
4. **Schedule is feature-complete as of this session** (bug round closed):
   - Copy-on-write special days (day-owned blocks, template delete detaches
     with snapshot, toggles/status preserved)
   - FAB on home adds the new block to the current day
   - Remove-from-day on special days (confirm dialog; library block stays)
   - `PATCH /api/schedule/{date}/block/{bid}/auto` now registered
   - Zero-template save, Back handling, race guards, error banners,
     delete confirmations, time validation, proguard, configurable base URL

## Open decisions / next steps

- Remaining schedule FEATURES (not bugs): per-day block EDIT UI, persistent
  day-block drag&drop ordering (backend order endpoints exist).
- Notes / tasks / notifications: DEFERRED by user decision — implement later,
  docs (AGENTS.md, architecture.md) still describe them.
- PR #1 (https://github.com/rbb11dot/clamit/pull/1) open on
  `feat/m3-expressive-and-bugfixes`; main protected (needs 1 approving review).
- Latest green CI: run `30630141246` (7/7). APK ready at
  `C:/workspace/projeler/clamit/latest-apk/app-debug.apk` (md5 ea55792b…).
- Install on phone: `adb uninstall com.clamit` first (signature mismatch between
  CI debug builds), then `adb install`.
