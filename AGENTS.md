# clamit — Second Brain

Go backend + Kotlin Android second brain application.
Schedule tracking, task management (habits/tasks/quick tasks), Obsidian-style notes.

## Tech Stack

- **Backend:** Go (7/24 background service)
- **Frontend:** Kotlin, Android (native)
- **Database:** SQLite (structured data), Markdown files (notes only)
- **Auth:** none (local-first)
- **Sync:** TBD

## Critical Rules — Agent MUST Follow

### 1. Start Fresh — Always

Before making ANY change:

\`\`\`bash
git fetch origin
git status                      # confirm clean tree
git checkout main
git pull --rebase origin main   # main MUST be up to date
git log --oneline -5            # visually confirm freshness
\`\`\`

**Never** branch from stale \`main\`. A branch cut from stale main carries conflicts you did not write. If uncommitted work exists, stash it first.

### 2. Branch Naming

\`\`\`bash
git checkout -b feat/<short-slug>    # new feature
git checkout -b fix/<short-slug>     # bug fix
git checkout -b chore/<short-slug>   # maintenance, deps, tooling
git checkout -b docs/<short-slug>    # documentation
git checkout -b refactor/<short-slug># structure-only change
\`\`\`

Branch name states intent, not ticket number. A reader should know what the branch does without reading the diff.

### 3. Commit in Logical Units

One commit = one logical change. If the message needs "and", it is two commits.

\`\`\`bash
git add -p                 # stage selectively
git diff --staged          # READ before you commit
git commit -m "type(scope): imperative description"
\`\`\`

Types: \`feat\`, \`fix\`, \`docs\`, \`refactor\`, \`test\`, \`chore\`.
Scope examples: \`schedule\`, \`tasks\`, \`notes\`, \`api\`, \`db\`, \`android\`.

Body explains WHY, not what. The diff already shows what.

### 4. Never Edit Generated Files

- \`vendor/\`, \`android/.gradle/\`, \`android/build/\` — NEVER touch
- \`go.sum\` — only modified by \`go mod tidy\`

### 5. Tests Are Sacred

**Never** modify a test to make it pass. If a test fails:
1. Read the failure carefully
2. Fix the implementation, not the test
3. If the test expectation is wrong, verify manually FIRST, then ask

Run tests before and after every change:
\`\`\`bash
cd backend && go test ./...
\`\`\`

### 6. One PR = One Thing

If the PR title needs "and", split the PR. A reviewable PR:
- Does ONE thing
- Stays small (< 300 lines diff ideally)
- States what changed, why, and how it was verified
- Lists the test that proves it works

### 7. Never Force Push to Shared Branches

- \`main\` is protected — no force push, no direct push
- Feature branches: prefer \`git pull --rebase\` over force push
- If you must force push a feature branch, coordinate first

### 8. Lint Before Push

\`\`\`bash
cd backend && go vet ./...
cd backend && go fmt ./...
\`\`\`

Never send an LLM to do a linter's job. Use deterministic tooling.

### 9. Branch Divergence Check

Before opening a PR, check how far behind you are:

\`\`\`bash
git fetch origin
git rev-list --count main..HEAD   # commits ahead
git rev-list --count HEAD..main    # commits behind (should be 0 after rebase)
\`\`\`

If you are behind \`main\`, rebase: \`git rebase main\`.

### 10. Tool Reality Checks

After every API call, command execution, or file operation:
- Check the **actual output**, not what you expected
- A 200 HTTP status does NOT mean success if `ok: false`
- A non-zero exit code is a failure — do not ignore it
- Read error messages fully before deciding next action

### 11. Builds Only via GitHub Actions

**Never** build APK or Go binaries locally. All builds run on GitHub Actions:
- Every push/PR triggers CI (`.github/workflows/ci.yml`): lint → test → build
- Every **successful push to main** auto-publishes a **continuous release** with binaries + APK
  - Download: `https://github.com/rbb11dot/clamit/releases/download/continuous/...`
- Versioned releases: tag `v*` triggers `.github/workflows/release.yml`

To download the latest continuous build:
```bash
curl -LO https://github.com/rbb11dot/clamit/releases/download/continuous/clamit-linux-amd64.tar.gz
curl -LO https://github.com/rbb11dot/clamit/releases/download/continuous/clamit-android-debug.apk
```

To test a local code change, use `go run ./cmd/server` (not `go build`).
The CI build is the canonical build — if it fails on CI, your code is broken.

## Commands

```bash
# Backend — develop with these (builds ONLY on CI)
cd backend && go run ./cmd/server      # start dev server
cd backend && go test ./...            # run all tests
cd backend && go vet ./...             # static analysis
cd backend && go mod tidy              # clean dependencies

# Android — develop with these (builds ONLY on CI)
cd android && ./gradlew test           # run unit tests
cd android && ./gradlew lint           # Android lint

# Git
git fetch origin
git pull --rebase origin main
git status
git log --oneline -5

# Download CI builds
gh run download --name clamit-backend
gh run download --name clamit-android-debug
```

## Project Structure

\`\`\`
clamit/
├── AGENTS.md              # this file
├── backend/               # Go backend
│   ├── cmd/server/        # entrypoint
│   ├── internal/
│   │   ├── api/           # HTTP handlers
│   │   ├── db/            # SQLite layer
│   │   ├── models/        # domain types
│   │   ├── scheduler/     # schedule engine
│   │   └── notes/         # markdown notes
│   ├── go.mod
│   └── go.sum
├── android/               # Kotlin Android app
│   └── app/src/main/java/com/clamit/
│       ├── ui/schedule/   # schedule screen
│       ├── ui/tasks/      # tasks screen (habits/tasks/quick)
│       ├── ui/notes/      # notes screen (Obsidian-style)
│       ├── data/          # repositories, database
│       └── service/       # background sync
├── agent_docs/            # progressive disclosure docs
├── scripts/               # validation and helper scripts
└── .github/               # GitHub config
    ├── CODEOWNERS
    ├── pull_request_template.md
    └── workflows/
\`\`\`

## Progressive Disclosure

Detailed documentation is in \`agent_docs/\`. Read the relevant file before starting work in that area:

- \`agent_docs/architecture.md\` — overall system architecture, data flow
- \`agent_docs/testing.md\` — testing strategy, how to run specific test suites
- \`agent_docs/conventions.md\` — code conventions, naming, patterns

These files are NOT loaded automatically. Read them when relevant.

## Agent Memory

- Use \`.agent-memory/\` to persist session state across conversations
- Before ending a session, write a summary of what was done and what the current state is
- Read \`.agent-memory/\` at the start of every session to pick up where you left off

## Security

- **Never** commit \`.env\` files, API keys, tokens, or certificates
- **Never** hardcode secrets in source code
- **Never** read or expose \`~/.ssh/\`, \`~/.config/gh/\`, or credential files
