# .agent-memory — Index

Memory layout for the clamit repo. Read this first, then the relevant file.

> **Note:** The `agent-memory` MCP middleware is NOT connected in this runtime
> (no MCP resources, `tool.memory.*` empty). Files below are hand-maintained
> per AGENTS.md ("Use `.agent-memory/` to persist session state"). Future
> sessions: read `local/current.shared.md` (bootstrap) + the section relevant
> to your task.

## Layout

| File | Purpose |
|------|---------|
| `local/current.shared.md` | Cross-session bootstrap: current state, quickstarts (incl. Maestro E2E setup on this machine) |
| `modules/maestro-e2e.md` | Module facts: Maestro CLI setup, commands, device/backend wiring, what works / what doesn't |
| `pitfalls.md` | Footguns with workarounds (Maestro + Termux + adb + Windows) |
| `sessions/` | End-of-session logs (UTC-date named) |

## Cheat sheet — Maestro E2E on this machine

- Phone (Nothing, serial `00153157O001730`) is the test device; no emulator (PC crashes), no Android Studio.
- Maestro CLI 2.7.0: `C:\workspace\maestro\bin\maestro.bat`; needs `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot` inline in harness shells.
- Flows: `C:\Users\alipc\maestro-flows\` (e.g. `smoke.yaml`).
- App under test: `com.clamit` (debug APK from continuous release).
- Backend MUST run on the phone via Termux (`clamit`, listens :8080) — app hardcodes `http://127.0.0.1:8080/` (ApiClient.kt).
- Test artifacts: `C:\Users\alipc\.maestro\tests\<ts>\`; `maestro hierarchy` output is cp1254 on Windows.

See `modules/maestro-e2e.md` for the full runbook.
