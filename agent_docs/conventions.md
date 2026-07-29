# Code Conventions

> Read this before writing code. These conventions are enforced by linters
> where possible; this document covers what linters cannot check.

## Go

### Naming

- Package names: lowercase, one word, no underscores (`db`, `api`, not `database_layer`)
- File names: snake_case (`schedule_service.go`)
- Types: PascalCase (`ScheduleTemplate`)
- Exported functions: PascalCase (`NewEngine`)
- Unexported functions: camelCase (`parseDuration`)
- Interfaces: -er suffix where possible (`Storer`, `Scheduler`)
- Receiver names: 1-2 letters (`s *Schedule`, not `schedule *Schedule`)

### Error Handling

- Always check errors. Never use `_` to discard an error
- Wrap errors with context: `fmt.Errorf("create schedule: %w", err)`
- Domain errors are defined as `var Err...` in the model package

### Imports

- Standard library first, third-party second, internal packages third
- Groups separated by a blank line

### Comments

- Exported types and functions always have doc comments
- TODO comments include your GitHub handle: `// TODO(@user): fix this`
- No comment for unexported trivial functions

## Kotlin

### Naming

- Classes: PascalCase (`ScheduleViewModel`)
- Functions/properties: camelCase (`formatTime()`)
- Constants: UPPER_SNAKE_CASE (`const val MAX_HABITS = 20`)
- Composable functions: PascalCase (`ScheduleScreen`)
- Files: PascalCase matching the primary class (`ScheduleViewModel.kt`)

### State Management

- ViewModels own all UI state
- State is exposed as `StateFlow` from ViewModel
- Compose observes with `collectAsStateWithLifecycle()`
- No mutable state outside ViewModel

### Imports

- Always use explicit imports, no wildcard (`import ...*`)
- Order: Android/Compose → Kotlin stdlib → third-party → project files

## General

- No `print`/`println` in committed code — use proper logging
- Dead code is deleted, not commented out
- One logical change per commit (see AGENTS.md rule 3)
- Follow existing patterns in the codebase, don't introduce new styles
