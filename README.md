# clamit — Second Brain

Go backend + Kotlin Android app. Schedule, tasks, notes.
Backend runs on Termux; app connects over localhost.

## Quick Install

```bash
curl -fsSL https://raw.githubusercontent.com/rbb11dot/clamit/main/install.sh | bash
```

Runs the backend binary on Termux (Linux/macOS/Windows also supported).
Android APK automatically downloaded on Termux.

## Usage

```bash
clamit                          # start backend (default :8080)
curl http://localhost:8080/health  # verify it's running
```

## Build from Source

```bash
cd backend && go run ./cmd/server
cd android && ./gradlew assembleDebug
```

## Downloads

Latest builds: https://github.com/rbb11dot/clamit/releases/tag/continuous
