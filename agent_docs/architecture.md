# clamit Architecture

> Read this before making architectural decisions or adding new features.

## Overview

clamit is a local-first second brain app. The Go backend runs as a 7/24 service
inside [Termux](https://termux.dev/) on the same Android device.
The Android app connects to it over HTTP on localhost.

## Data Flow

```
┌──────────────┐     HTTP/REST     ┌──────────────┐
│  Android App │ ◄──────────────► │  Go Backend  │
│  (Kotlin)    │                   │  (Termux)    │
└──────────────┘                   └──────┬───────┘
       same device, localhost             │
                              ┌───────────┴───────────┐
                              │                       │
                         ┌────────┐           ┌────────────┐
                         │ SQLite │           │  Markdown  │
                         │ (data) │           │  (notes)   │
                         └────────┘           └────────────┘
```

- **SQLite** stores structured data: schedule routines, tasks, habits, settings
- **Markdown files** store notes (Obsidian-compatible format)
- The backend owns all data access; Android is a thin client

## Key Decisions

| Decision | Rationale |
|---|---|
| Go over Node/Python | Single binary, near-zero memory, starts in ms, runs forever on Termux |
| Local-first, no cloud | Privacy, offline-by-default, all data on device |
| SQLite + Markdown | Zero ops, no server to manage, notes are plain files |
| REST over gRPC | Simpler tooling, localhost latency is irrelevant |
| Termux over separate server | Both app and backend on same phone, no network dependency |

## Backend Layers

```
cmd/server/          — entrypoint, config, server lifecycle
internal/
  api/               — HTTP handlers, request/response types
  db/                — SQLite migrations, queries, repository
  models/            — domain types shared across packages
  scheduler/         — schedule engine, routine evaluation
  notes/             — markdown file operations, frontmatter parsing
```

## Android Layers

```
ui/schedule/   — daily routine timeline (TimeTune-like)
ui/tasks/      — habits list, task list, quick task input
ui/notes/      — markdown note viewer/editor (Obsidian-like)
data/          — repositories, local cache, API client
service/       — background sync, notification scheduling
```

## Future Considerations

- Eventually: Web UI (HTMX + Go templates) for desktop/tablet access
- Eventually: Sync protocol between devices (optional, offline-first always)
- Never: User accounts, cloud dependency, proprietary formats
