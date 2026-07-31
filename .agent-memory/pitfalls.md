# Pitfalls

<!-- @id: maestro-pitfalls -->

Footguns hit while setting up Maestro E2E (2026-07-31). Each: symptom → cause → fix.

- **`ERROR: JAVA_HOME is set to an invalid directory: C:\Program Files\Eclipse`**
  Cause: JAVA_HOME env var pointed at a non-JDK dir (left by an old Eclipse install);
  `echo $JAVA_HOME` in PowerShell does NOT read env vars (that's `$env:JAVA_HOME`), so it looked unset.
  Fix: `$javaHome = Split-Path (Split-Path (Get-Command java).Source); [Environment]::SetEnvironmentVariable("JAVA_HOME", $javaHome, "User")`, then NEW terminal.

- **`Parsing Failed at smoke.yaml:4:16`**
  Cause: YAML double-quoted string contained `\.` (invalid YAML escape).
  Fix: use single quotes for regex escapes — `assertVisible: 'Bu gün için zaman bloğu yok\.'`.

- **First `takeScreenshot` showed the launcher while the app was displayed**
  Cause: screenshot timing raced app first frame (not a crash — logcat had no FATAL;
  `Displayed com.clamit/.MainActivity` present).
  Fix: verify with `adb shell dumpsys window | grep mCurrentFocus` or `maestro hierarchy`,
  and use `adb exec-out screencap -p` for ground-truth captures.

- **`am start -n com.termux/.app.TermuxActivity` → "Error type 3 ... does not exist"**
  Cause: missing `--user 0` in this shell context.
  Fix: add `--user 0`, or use `adb shell monkey -p com.termux -c android.intent.category.LAUNCHER 1`.

- **App shows `Hata: Failed to connect to /127.0.0.1:8080`**
  Cause: backend not running on the phone; app base URL is hardcoded `http://127.0.0.1:8080/`
  (`android/.../data/api/ApiClient.kt`) — that's the PHONE's localhost, not the PC's.
  Fix: run `clamit` in Termux on the phone (see `modules/maestro-e2e.md`).

- **`maestro hierarchy` JSON fails `json.loads` with UnicodeDecodeError**
  Cause: Windows console output is cp1254, not UTF-8.
  Fix: `raw = open(path,'rb').read(); json.loads(raw.decode('cp1254'))`.

- **`maestro` not found in harness shell**
  Cause: harness Git-Bash process predates `setx PATH` (env read at process start).
  Fix: full path `C:\workspace\maestro\bin\maestro.bat` + inline `set "JAVA_HOME=..."`.

- **Compose app has no resource-ids** (video lesson confirmed)
  Flutter `Key()` / Compose ids are NOT accessibility ids for Maestro; text-based
  selectors are the reliable path. `assertVisible`/`tapOn` are exact regex matches —
  `"Mekanlar"` does not match `"Mekanlar!"`.
