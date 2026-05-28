# MovieNight Copilot Instructions

## Architecture Overview

MovieNight is a self-hosted media library browser. The backend is a **Java 25 HTTP server with no framework** (uses `com.sun.net.httpserver` directly) backed by SQLite. The frontend is a **React/TypeScript SPA** built with Vite. During development they run separately; for production the frontend is built into backend static resources and served from the same process.

```
frontend/           React + TypeScript + Tailwind (Vite)
backend/            Java 25 + SQLite + custom HTTP server (Maven)
  └─ src/main/resources/static/frontend/   (frontend build output)
```

## Build & Test Commands

**Backend:**
```bash
cd backend && mvn package -q          # build fat jar (outputs MovieNight-2.0-SNAPSHOT.jar)
cd backend && mvn test -q             # run all tests
cd backend && mvn test -q -Dtest=DatabaseTest          # run a single test class
cd backend && mvn test -q -Dtest=ApiIntegrationTest#testCreateGroup  # run a single test method
```

**Frontend:**
```bash
cd frontend && npm run build          # build and emit to backend/src/main/resources/static/frontend/
cd frontend && npm run dev            # dev server with proxy to localhost:8181
```

**Running the app:**
```bash
# With defaults (port 8080, current dir as data dir):
java -jar backend/target/MovieNight-2.0-SNAPSHOT.jar

# With a config file:
MOVIENIGHT_CONFIG_FILE=/path/to/config.properties java -jar ...
```

Config file is a Java `.properties` file. Recognized keys: `port`, `dataDir`, `pageSize`, `apiBasePath`, `rangeLimitMB`.

## Data Model

Two domain objects:

- **`MediaGroup`** — a named group (like a folder). Groups are hierarchical: `parentGroupId == null` means top-level. Groups can contain other groups and/or items.
- **`MediaItem`** — a single streamable file. Always belongs to one `MediaGroup` via `mediaGroupId`. Key fields:
  - `mediaFilePath` — **relative** to the configured `dataDir`, not absolute.
  - `tags` — always normalized on save: trimmed, lowercased, deduplicated.
  - `hasThumbnail` — computed at runtime from the filesystem; **not stored in the database**.
  - `lastWatchedDate` — nullable `LocalDate`.

Thumbnails are stored in `{dataDir}/thumbnails/` as `MediaItem_{id}.jpg` or `MediaGroup_{id}.jpg`.

## Backend Conventions

### Custom Router
The backend uses a hand-rolled `Router` (no Spring, no Javalin). Routes are registered in `ApiServer` in a deliberate order — **more specific routes must be registered before more general ones**. When a handler cannot handle a request it throws `new RuntimeException("ROUTE_NOT_MATCHED")` wrapped in an `IOException`; the router then tries the next matching route.

### Adding a New Endpoint
1. Create or extend a `Handler` class in `api/handler/` implementing `HttpHandler`.
2. Add `DTO` classes in `api/dto/` if needed.
3. Register the route in `ApiServer` in the correct order relative to existing routes.
4. Add CRUD methods to `Database.java` if the handler needs DB access (all SQL lives there).
5. Optionally wrap business logic in a `Service` class in `service/`.

### Error Handling
All exceptions are mapped to JSON error responses via `ExceptionMapper`. `Database.NotFoundException` → 404. Unhandled exceptions → 500.

### Database Access
All SQL is in `Database.java`. All write operations are synchronized on `lockObject`. Use the existing `open()`/`dispose()` lifecycle. Tests create a temp dir and a fresh DB per test class via `@BeforeAll`.

## Frontend Conventions

### Two UI Sections
- `/browse` — public-facing media browser (`src/browse/`)
- `/admin` — management UI (`src/admin/`)

Each section has its own `AppShell` component and nav. `App.tsx` owns the top-level route tree.

### API Layer
All API calls go through `apiFetch()` in `src/api/client.ts`. Never call `fetch()` directly. The API base path is read from `VITE_API_BASE_PATH` (defaults to `/MovieNight/`). During `npm run dev`, Vite proxies `/MovieNight/` to `localhost:8181`.

Module layout in `src/api/`:
- `client.ts` — `apiFetch`, `buildApiUrl`, `buildQueryString`
- `types.ts` — shared TypeScript interfaces mirroring backend DTOs
- `groups.ts`, `items.ts`, `thumbnails.ts` — typed API functions per resource

### Component Layers
- `src/components/ui/` — generic, reusable UI primitives (Button, Input, Card, Dialog, Toast, etc.)
- `src/components/shared/` — app-specific shared components (Thumbnail, TagPills, SearchBar, Breadcrumbs, etc.)
- `src/admin/features/` — complex admin-specific feature components (forms, delete dialogs)
- `src/admin/pages/` / `src/browse/pages/` — page-level components, one per route

### Forms
Forms use `react-hook-form` + `zod` for validation. Resolvers are wired via `@hookform/resolvers/zod`.

### Data Fetching
TanStack Query (`@tanstack/react-query`) is used for all server state. Follow the existing pattern of calling the typed API functions from `src/api/` inside `useQuery`/`useMutation` hooks.

### Theming
Light/dark theme is managed by `ThemeProvider` in `src/theme/`. CSS custom properties are defined in `tokens.css`. Tailwind classes use the standard `dark:` variant.
