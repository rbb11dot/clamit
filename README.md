# clamit — Second Brain

Go backend + Kotlin Android second brain app.
Schedule tracking, task management (habits/tasks/quick tasks), Obsidian-style notes.

> **Status:** Early development. Everything changes.

## Quick Install

### Android

```bash
# Download the latest APK from the continuous release
curl -LO https://github.com/rbb11dot/clamit/releases/download/continuous/app-debug.apk
# Install (requires Android device or emulator)
adb install app-debug.apk
```

### Backend (Linux)

```bash
# Download & extract
curl -L https://github.com/rbb11dot/clamit/releases/download/continuous/clamit-linux-amd64.tar.gz | tar xz
# Run
./clamit-linux-amd64
```

### Backend (macOS)

```bash
# Apple Silicon
curl -L https://github.com/rbb11dot/clamit/releases/download/continuous/clamit-darwin-arm64.tar.gz | tar xz
# Intel
curl -L https://github.com/rbb11dot/clamit/releases/download/continuous/clamit-darwin-amd64.tar.gz | tar xz
```

### Backend (Windows PowerShell)

```powershell
curl.exe -LO https://github.com/rbb11dot/clamit/releases/download/continuous/clamit-windows-amd64.tar.gz
tar xzf clamit-windows-amd64.tar.gz
.\clamit-windows-amd64.exe
```

### All in One

```bash
# List available assets
gh release view continuous --json assets -q '.assets[].name'
```

## Build from Source

### Backend

```bash
cd backend
go run ./cmd/server
```

### Android

```bash
cd android
./gradlew assembleDebug
```

Builds are also available as CI artifacts: **Actions** → latest workflow → Artifacts.

## Tech Stack

| Component | Technology |
|---|---|
| Backend | Go (7/24 background service) |
| Frontend | Kotlin, Android (Jetpack Compose) |
| Database | SQLite (structured data), Markdown (notes) |
| CI/CD | GitHub Actions |

## Project Structure

```
clamit/
├── backend/          # Go HTTP server
├── android/          # Kotlin Android app
├── agent_docs/       # Architecture, testing, conventions
├── scripts/          # Pre-push validation helpers
├── AGENTS.md         # AI coding agent instructions
└── .github/          # CI, release workflows
```

## License

MIT
