# Testing Strategy

> Read this before writing or running tests.

## Backend (Go)

**Run all tests:**
```bash
cd backend && go test ./...
```

**Run with coverage:**
```bash
cd backend && go test -coverprofile=coverage.out ./... && go tool cover -html=coverage.out
```

**Run a specific package:**
```bash
cd backend && go test ./internal/db/...
```

**Run a specific test:**
```bash
cd backend && go test -run TestCreateSchedule ./internal/db/...
```

### Conventions

- Test files live next to the code they test: `db/schedule.go` → `db/schedule_test.go`
- Use standard `testing` package (no third-party test frameworks)
- Table-driven tests for multiple cases
- SQLite tests use `:memory:` database
- Integration tests that need a running server are in `cmd/server/server_test.go`

### Coverage Goals

- `internal/db/`: 90%+
- `internal/api/`: 80%+ (handler tests with httptest)
- `internal/scheduler/`: 90%+
- `internal/notes/`: 80%+

## Android (Kotlin)

**Run unit tests:**
```bash
cd android && ./gradlew test
```

**Run instrumented tests:**
```bash
cd android && ./gradlew connectedAndroidTest
```

### Conventions

- Unit tests: `src/test/java/`
- Instrumented tests: `src/androidTest/java/`
- ViewModel tests use Turbine for Flow testing
- Repository tests use fake data sources, not mocks

## CI

CI runs all backend and Android tests on every PR.
A PR that fails tests cannot be merged (branch protection).
