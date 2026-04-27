# MovieNight Copilot Instructions

## Build and test commands

- Frontend production build: `cd frontend && npm run build`
- Full packaged application build: `cd backend && mvn clean package`
- Full backend test suite: `cd backend && mvn test`
- Single backend test class: `cd backend && mvn -Dtest=StreamControllerTest test`
- Single backend test method: `cd backend && mvn -Dtest=StreamControllerTest#someTestMethod test`
- There is currently no dedicated lint script in `frontend/package.json` or `backend/pom.xml`.

`backend/pom.xml` is the top-level build entrypoint: its Maven build installs Node, runs `npm ci`, runs the frontend build, and then packages the Spring Boot jar. The Vite build writes into `backend/src/main/resources/static/frontend`, so changes to frontend build output affect the packaged backend immediately.

## High-level architecture

MovieNight is a two-part app: a React/Vite frontend in `frontend/` and a Spring Boot backend in `backend/`. The backend serves both the REST API and the built SPA. `SpaForwardController` forwards `/`, `/admin`, and `/admin/**` to the frontend entrypoint, while `frontend/src/App.jsx` keeps routing intentionally small: browse mode at `/` and admin mode at `/admin`.

The main product flow is mirrored across three media families:

- movies -> genres
- episodes -> series
- music videos -> artists

Each family has the same general structure: JPA model + repository, service-layer business logic, REST controller, and matching frontend list/form components. The frontend keeps most orchestration in `frontend/src/pages/MediaLibraryPage.jsx`: tab state, fetches, CRUD actions, thumbnail upload/delete flows, and error handling all live there rather than being spread across many hooks.

SQLite stores media metadata, tags, relationships, and watch history. Actual video files are not uploaded into the app; admin users point the app at files already present on the server filesystem, using `/api/files` to browse directories. Thumbnail images are also filesystem-backed under `movienight.data-dir`.

Streaming is separate from CRUD. Frontend cards play via `/api/stream/{encodedId}`. `MediaService` resolves encoded IDs with prefixes `M`, `E`, and `V` to file paths and updates the media item's `lastWatchedDate`; `StreamController` serves the file, handles HTTP range requests, and can also generate VLC playlist files.

Security is split by intent rather than by separate apps. Read-only browsing is public for `GET /api/**` except `/api/files`. Admin UI routes plus all `POST`/`PUT`/`DELETE` API routes require HTTP Basic auth, and localhost-only admin access is enabled by default. Errors are normalized through `GlobalExceptionHandler`, and every request gets an `X-Correlation-Id` from `CorrelationIdFilter`.

## Key conventions

- Preserve the symmetry between the three media families. Features added to one stack often need corresponding backend and frontend changes for the other two.
- Do not persist UI convenience flags directly. Fields such as `hasThumbnail`, `movieCount`, `episodeCount`, `videoCount`, and `watchedRecently` are transient/read-only JSON fields populated in services before responses are returned.
- Thumbnails are managed as a second step after the main entity save. Frontend forms pass `_thumbnail` and `_clearThumbnail`; `MediaLibraryPage.jsx` strips those helper fields from the JSON payload, saves the entity first, then calls the separate thumbnail endpoint.
- Thumbnail support only activates when `movienight.data-dir` is configured **and already exists**. `ThumbnailService` creates per-entity subdirectories under that base path and validates uploads as JPG/PNG within 26x26 to 2000x2000 pixels.
- Tag handling is normalized in model setters. Tags are trimmed, lowercased, de-duplicated, and stored as element collections; backend search uses case-insensitive `Specification`s and joins the tag collection with `query.distinct(true)` when filtering by tag.
- Category-like names are not normalized uniformly across all models. `Genre` lowercases names in `setName()`, while `Series` and `Artist` currently preserve case. Check the target model before changing naming behavior.
- Frontend media cards across all list components determine thumbnail object fit at runtime from `naturalWidth`/`naturalHeight`, using `object-fill` for landscape thumbnails and `object-contain` otherwise. Keep that behavior aligned when changing card rendering.
- The file browser operates on server-local filesystem paths, not client paths, and intentionally skips dotfiles and symlinks.
