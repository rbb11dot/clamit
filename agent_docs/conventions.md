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

### Anti-Patterns — Go

#### Error handling

❌ **Log and swallow** — `log.Println(err); return nil`
  Caller thinks everything is fine. Always wrap and return:
  ```go
  return fmt.Errorf("schedule.Create: %w", err)
  ```

❌ **sql.ErrNoRows treated as generic error**
  Define domain errors:
  ```go
  var ErrNotFound = errors.New("not found")
  // ...
  if errors.Is(err, sql.ErrNoRows) {
    return nil, ErrNotFound
  }
  ```

#### Context

❌ **DB functions without context parameter**
  Every database call must accept `ctx context.Context`:
  ```go
  func (r *Repo) GetSchedule(ctx context.Context, id uuid.UUID) (*Schedule, error)
  ```

#### SQLite

❌ **CGO-bound driver** — `mattn/go-sqlite3` doesn't work on Termux
  Always use `modernc.org/sqlite` (pure Go).

❌ **Opening new connection per request** — `sql.Open` in every handler
  One `*sql.DB` at package level, reused everywhere.

❌ **No transaction for writes** — multiple INSERT/UPDATE without BeginTx
  Wrap bulk writes in a transaction.

❌ **Concurrent write without retry** — SQLite returns `database is locked`
  Use backoff retry or serialize writes.

#### fsnotify

❌ **Watching files instead of directories** — #1 fsnotify mistake
  Editors atomically replace files (same name, new inode). Watch the parent
  directory and filter by filename.

❌ **Not draining the Errors channel** — watcher blocks silently
  Always read both channels in a `select`:
  ```go
  select {
  case event, ok := <-watcher.Events:
  case err, ok := <-watcher.Errors:
  }
  ```

❌ **No debounce** — 10 events for one Ctrl+S
  Aggregate with a 200–300ms debounce before acting.

❌ **Reading file immediately on Write event** — half-written content
  Add a small delay or check write completion before reading.

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
- DI via **Koin**: `viewModel { MyViewModel(get()) }` in a Koin module
- Compose observes with `collectAsStateWithLifecycle()`
- No mutable state outside ViewModel

### Imports

- Always use explicit imports, no wildcard (`import ...*`)
- Order: Android/Compose → Kotlin stdlib → third-party → project files

### Anti-Patterns — Kotlin & Compose

#### Coroutines

❌ **`GlobalScope.launch`** — outlives ViewModel, memory leak
  Always use `viewModelScope` or `lifecycleScope`.

❌ **`runBlocking` in production code** — blocks the thread
  Only in tests or `main()` entry point.

❌ **No error handling in Flow** — crash kills the collector
  ```kotlin
  .catch { emit(UiState.Error(it)) }
  ```

❌ **`callbackFlow` without `awaitClose`** — listener leaks
  Always unregister in `awaitClose { unregister() }`.

#### Jetpack Compose

❌ **`collectAsState()` instead of `collectAsStateWithLifecycle()`**
  First one ignores lifecycle, causes wasted recompositions off-screen.

❌ **God-Composable** — 400+ lines
  Break into small composables, each with a single responsibility.

❌ **State not hoisted** — all state in ViewModel, none in composable params
  Hoist to the lowest composable boundary:
  ```kotlin
  fun MyWidget(value: String, onValueChange: (String) -> Unit)
  ```

❌ **No `remember` on lambdas** — new object on every recompose
  ```kotlin
  val onClick = remember { { doThing() } }
  ```

❌ **LazyColumn without `key`** — item state scrambles on reorder
  ```kotlin
  items(data, key = { it.id }) { item -> ... }
  ```

❌ **Scaffold padding not applied** — content under system bars
  ```kotlin
  Scaffold { padding -> Box(Modifier.padding(padding)) { ... } }
  ```

❌ **`derivedStateOf` overuse** — optimizing before profiling
  Write it simply first, profile, then optimize only bottlenecks.

❌ **Unstable lambdas/comparisons** — passing `() -> Unit` without remember
  Triggers recomposition of every child. Wrap with `remember` or use event sealed class.

#### State Management

❌ **Single sealed class with too many states** — bloated `when` blocks
  Keep UI state small: `Loading | Content(data) | Error(msg)`.
  Separate navigation/events from UI state (use `SharedFlow` for one-shot events).

❌ **Mutable state exposed from ViewModel** — composition breaks
  Only expose `StateFlow`, never `MutableStateFlow`.

#### Koin

❌ **Everything `single`** — stateful objects become singletons
  Stateless/immutable → `single`; stateful → `factory`.

❌ **`get()` inside classes instead of constructor injection**
  Service locator pattern. Dependencies must come via constructor.

❌ **Context in singleton** — memory leak
  Use `androidContext()` or scope to Application, not Activity.

❌ **Missing `checkModules()` in tests** — runtime crash on missing binding
  ```kotlin
  @Test fun verifyModules() = checkModules { modules }
  ```

## General

- No `print`/`println` in committed code — use proper logging
- Dead code is deleted, not commented out
- One logical change per commit (see AGENTS.md rule 3)
- Follow existing patterns in the codebase, don't introduce new styles
