# clamit — Second Brain

Go backend + Kotlin Android second brain application.
Schedule tracking, task management (habits/tasks/quick tasks), Obsidian-style notes.

**Architecture:** Android app + Go backend running locally on the same device via [Termux](https://termux.dev/).

> **Status:** Early development. Everything changes.

## Quick Install

### 1. Android App

```bash
curl -LO https://github.com/rbb11dot/clamit/releases/download/continuous/app-debug.apk
adb install app-debug.apk
```

### 2. Backend (Termux — Android)

Open Termux on your phone and run:

```bash
# Download
curl -LO https://github.com/rbb11dot/clamit/releases/download/continuous/clamit-linux-arm64.tar.gz
# Extract
tar xzf clamit-linux-arm64.tar.gz
# Make executable
chmod +x clamit-linux-arm64
# Run
./clamit-linux-arm64
```

### Backend (other platforms)

```bash
# Linux (x86_64)
curl -L https://github.com/rbb11dot/clamit/releases/download/continuous/clamit-linux-amd64.tar.gz | tar xz

# macOS (Apple Silicon)
curl -L https://github.com/rbb11dot/clamit/releases/download/continuous/clamit-darwin-arm64.tar.gz | tar xz

# macOS (Intel)
curl -L https://github.com/rbb11dot/clamit/releases/download/continuous/clamit-darwin-amd64.tar.gz | tar xz

# Windows (PowerShell)
curl.exe -LO https://github.com/rbb11dot/clamit/releases/download/continuous/clamit-windows-amd64.tar.gz
tar xzf clamit-windows-amd64.tar.gz
```

### All Assets

```bash
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

## Tech Stack

| Component | Technology | Runs on |
|---|---|---|
| Backend | Go | Termux (Android) / localhost |
| App | Kotlin, Jetpack Compose | Android |
| Database | SQLite + Markdown | Device-local |
| CI/CD | GitHub Actions | Cloud |

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
