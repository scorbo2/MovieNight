## Plan: Separate Browse and Admin UI

Build a modern React-based frontend around the existing REST contract (contained within the Java/Maven project in
`backend/`), with two clearly separated experiences: a public browse app for discovery and a private-by-convention admin
app for full CRUD and thumbnail management. The recommended implementation is a small frontend workspace with two
route-level applications (`browse` and `admin`) sharing a common design system, API client, state/query utilities, and
theme infrastructure. React + Tailwind is a strong fit here because it supports fast UI iteration, responsive layouts,
and token-driven theming without forcing hard-coded colors into components.

The backend API already provides the core capabilities this UI needs:
- group browse/search and item browse/search
- group and item detail retrieval
- group and item create/update/delete for admin workflows
- media streaming and playlist generation for the browse experience
- thumbnail fetch/create/replace/delete endpoints for both groups and items

That means the UI plan should optimize for clean separation of concerns, rapid iteration, and future extensibility rather than heavy framework complexity.

## Recommended Frontend Structure

Use one frontend codebase with shared packages rather than two completely separate repos.

Suggested structure:

```text
frontend/
  apps/
	browse/
	admin/
  packages/
	ui/
	theme/
	api-client/
	media-domain/
```

### Why this structure works well

1. `browse` and `admin` stay visibly separate in navigation, layout, and intent.
2. Shared packages prevent duplicated form controls, cards, tables, dialogs, and API code.
3. A central `theme` package makes light/dark and future themes easy to manage consistently.
4. A shared `api-client` package keeps endpoint contracts aligned with the Java backend and reduces breakage.

If you want an even simpler first iteration, this can also be implemented as a single React app with two route groups:
- `/browse/*`
- `/admin/*`

That said, two app shells in one workspace is the cleaner long-term shape.

## High-Level UX Direction

### Browse UI

The browse experience should feel visual, lightweight, and media-oriented:

- prominent search/filter controls: media items can be searched for by title, description, or tags, and groups can be
  searched for by title or description
- tags are read-only in the browse UI but should be visible as metadata on each media item card and detail page. Tag
  pills should be clickable to trigger a search for that tag across all media items.
- card/grid presentation for groups and items
- optional list/table mode for denser navigation
- strong thumbnail usage where available
- clear breadcrumb navigation through nested groups
- polished empty states, skeleton loading states, and large click targets
- each media item detail page has a "Watch now" action that shows an inline HTML5 video player using the backend's
  streaming endpoint
- each media item detail page also has a "Watch in VLC" action that invokes the backend's playlist endpoint to download
  an `m3u` file for that media item
- when browsing a media group, the group itself should also have a "Watch all in VLC" action that uses the playlist
  endpoint to download an `m3u` playlist for all media items (direct children only) in that group

### Admin UI

The admin experience should feel efficient and operational:
- denser data presentation
- quick create/edit actions
- inline status feedback for saves/deletes/uploads
- ability to add/edit metadata and tags on media items
- bulk-friendly navigation patterns, even if bulk actions are deferred
- predictable forms with validation and confirmation dialogs
- thumbnail upload/replace/remove controls attached directly to edit screens
- no media streaming or playlist generation in the admin UI!

## Proposed Component Architecture

Use a layered component architecture so low-level UI remains reusable while app-specific screens stay thin.

### 1. App Shell Layer

Responsible for layout, navigation, and top-level providers.

Shared shell concerns:
- router provider
- query/data provider
- theme provider
- global toast/notification provider
- modal/dialog host

Browse shell components:
- `BrowseAppShell`
- `BrowseHeader`
- `BrowseSidebar` or collapsible filter drawer
- `Breadcrumbs`
- `ThemeSwitcher`

Admin shell components:
- `AdminAppShell`
- `AdminTopBar`
- `AdminNav`
- `CommandBar`
- `ThemeSwitcher`

### 2. Page Layer

These components represent route-level screens and own page composition.

Browse pages:
- `BrowseHomePage`
- `GroupBrowsePage`
- `GroupDetailPage`
- `ItemDetailPage`
- `SearchResultsPage`

Admin pages:
- `AdminDashboardPage`
- `AdminGroupsPage`
- `AdminGroupEditPage`
- `AdminItemsPage`
- `AdminItemEditPage`
- optional `AdminThumbnailManagerPage` if thumbnail operations become large enough to justify a dedicated screen

### 3. Feature Layer

These are reusable domain-focused modules composed into pages.

Group features:
- `GroupTree`
- `GroupCardGrid`
- `GroupListTable`
- `GroupFilters`
- `GroupForm`
- `GroupDeleteDialog`
- `GroupThumbnailPanel`

Item features:
- `ItemCardGrid`
- `ItemListTable`
- `ItemFilters`
- `ItemForm`
- `ItemDeleteDialog`
- `ItemThumbnailPanel`
- `TagEditor`

Shared media/domain features:
- `Thumbnail`
- `MediaMetaPanel`
- `PaginationControls`
- `SearchBar`
- `EmptyState`
- `ErrorState`
- `ConfirmDialog`

### 4. Primitive UI Layer

These are design-system components that should not know about groups/items specifically.

Examples:
- `Button`
- `IconButton`
- `Input`
- `Textarea`
- `Select`
- `Checkbox`
- `Switch`
- `Tabs`
- `Dialog`
- `Drawer`
- `Popover`
- `Card`
- `Badge`
- `Table`
- `Pagination`
- `Toast`
- `Skeleton`

These should live in the shared `ui` package and be styled only through semantic theme tokens.

## Data and State Architecture

Use a small but modern client data layer.

Recommended choices:
- React Router for routing
- TanStack Query for server-state fetching, caching, invalidation, and optimistic refresh
- React Hook Form + Zod for admin form handling and validation
- Tailwind CSS for styling
- optional Zustand only if a small amount of cross-page client state emerges beyond what React and query state handle cleanly

### API client shape

Create a typed API wrapper around the backend endpoints:

- `listGroups(params)`
- `getGroup(id)`
- `createGroup(payload)`
- `updateGroup(id, payload)`
- `deleteGroup(id)`
- `listGroupItems(groupId, params)`
- `getItem(id)`
- `createItem(groupId, payload)`
- `updateItem(id, payload)`
- `deleteItem(id)`
- `getGroupThumbnailUrl(id)`
- `getItemThumbnailUrl(id)`
- `uploadGroupThumbnail(id, file)`
- `uploadItemThumbnail(id, file)`
- `replaceGroupThumbnail(id, file)`
- `replaceItemThumbnail(id, file)`
- `deleteGroupThumbnail(id)`
- `deleteItemThumbnail(id)`

### Query strategy

Suggested query keys:
- `['groups', filters, page]`
- `['group', groupId]`
- `['group-items', groupId, filters, page]`
- `['item', itemId]`
- `['thumbnail', 'group', groupId]`
- `['thumbnail', 'item', itemId]`

This makes invalidation straightforward after create/update/delete/thumbnail operations.

## Browse UI Scope

### Core browse routes

1. Home page with top-level media groups.
2. Group page showing:
   - group metadata
   - child groups
   - items within the selected group
   - filters and pagination
3. Item detail page showing:
   - title, description, tags, last watched date, media path
   - thumbnail if present
   - inline HTML5 video player if "Watch now" is clicked
   - optional related items from the same group

### Browse interaction model

- top-level groups load via `GET /MovieNight/media-groups?topLevelOnly=true`
- child groups load via `GET /MovieNight/media-groups?parentGroupId={id}`
- items in a group load via `GET /MovieNight/media-groups/{groupId}/items`
- item details load via `GET /MovieNight/media-items/{itemId}`
- thumbnail images load directly from the thumbnail endpoints when `hasThumbnail` is true

### Browse UI priorities

1. Smooth skeleton loading states
2. Attractive responsive card layouts
3. Persistent filters in URL query params
4. Graceful fallback art/placeholders when thumbnails are missing
5. Mobile-friendly navigation and filter drawers

## Admin UI Scope

### Core admin routes

1. Dashboard page with quick counts and shortcuts.
2. Groups management page with list/search/filter/pagination.
3. Group create/edit page with parent selection and metadata form.
4. Group detail or edit sidebar showing child groups, contained items, and thumbnail tools.
5. Items management page scoped to a selected group.
6. Item create/edit page with tags, last watched date, file path, and thumbnail controls.

### Admin interaction model

Group management:

- create via `POST /MovieNight/media-groups`
- edit via `PUT /MovieNight/media-groups/{groupId}`
- delete via `DELETE /MovieNight/media-groups/{groupId}`

Item management:

- create via `POST /MovieNight/media-groups/{groupId}/items`
- edit via `PUT /MovieNight/media-items/{itemId}`
- delete via `DELETE /MovieNight/media-items/{itemId}`

Thumbnail management:

- preview via `GET /MovieNight/thumbnails/media-groups/{groupId}` or `GET /MovieNight/thumbnails/media-items/{itemId}`
- upload new via `POST`
- replace via `PUT`
- remove via `DELETE`

Streaming and playlist generation:

- Streaming vie `GET /MovieNight/stream/{itemId}`
- Playlist generation (single item) `GET /MovieNight/playlist/media-item/{itemId}`
- Playlist generation (group) `GET /MovieNight/playlist/media-group/{groupId}`
- Playlist generation (arbitrary list) `POST /MovieNight/playlist/media-item` - JSON body contains an array of
  mediaItemIds. The arbitrary playlist feature is supported by the backend but will be deferred in the frontend for now.

### Admin form components

`GroupForm` fields:
- `parentGroupId`
- `title`
- `description`

`ItemForm` fields:
- `mediaGroupId` for edit mode only, or inferred from selected group in create mode
- `title`
- `description`
- `lastWatchedDate`
- `mediaFilePath`
- `tags`

### Thumbnail component behavior

The thumbnail panel should support:
- current thumbnail preview
- drag-and-drop file selection
- standard file picker
- replace action when a thumbnail already exists
- remove action with confirmation
- failure state if upload/delete fails

Prefer `multipart/form-data` uploads in the UI because that is the most natural browser workflow. The JSON base64 path can remain available for future clipboard/paste enhancements.

## Theme System and Token Schema

Theming should be driven by semantic design tokens mapped to CSS variables, not by hard-coded Tailwind colors directly in components.

### Recommended theme model

1. Define a small set of semantic tokens.
2. Store theme definitions as token maps.
3. Apply tokens to `:root` or a `[data-theme="..."]` selector.
4. Reference tokens inside Tailwind config via CSS variables.
5. Ensure components use semantic classes like `bg-surface`, `text-content`, `border-subtle`, not raw `bg-zinc-900` or `text-white`.

### Suggested token schema

#### Core surfaces
- `--color-bg-app`
- `--color-bg-subtle`
- `--color-surface`
- `--color-surface-elevated`
- `--color-surface-overlay`

#### Text/content
- `--color-text-primary`
- `--color-text-secondary`
- `--color-text-muted`
- `--color-text-inverse`

#### Borders/dividers
- `--color-border-default`
- `--color-border-subtle`
- `--color-border-strong`

#### Brand/interactive
- `--color-brand`
- `--color-brand-hover`
- `--color-brand-active`
- `--color-focus-ring`

#### Feedback states
- `--color-success`
- `--color-warning`
- `--color-danger`
- `--color-info`

#### Component-specific semantics
- `--color-card-bg`
- `--color-card-border`
- `--color-input-bg`
- `--color-input-border`
- `--color-input-placeholder`
- `--color-sidebar-bg`
- `--color-header-bg`
- `--color-table-row-hover`
- `--color-thumbnail-placeholder-bg`

#### Shadows and radii
- `--shadow-sm`
- `--shadow-md`
- `--shadow-lg`
- `--radius-sm`
- `--radius-md`
- `--radius-lg`
- `--radius-xl`

#### Spacing and typography tokens if desired
- `--font-sans`
- `--font-mono`
- `--text-xs`
- `--text-sm`
- `--text-md`
- `--text-lg`

Typography and spacing can remain mostly in Tailwind defaults initially, but the schema should allow theming to expand into those later.

### Example theme registry shape

```ts
const themes = {
  light: {
	colorBgApp: '248 250 252',
	colorSurface: '255 255 255',
	colorTextPrimary: '15 23 42',
	colorBrand: '59 130 246'
  },
  dark: {
	colorBgApp: '2 6 23',
	colorSurface: '15 23 42',
	colorTextPrimary: '241 245 249',
	colorBrand: '96 165 250'
  }
}
```

The exact values are less important than the pattern: components consume semantic tokens, and themes supply values.

### Tailwind integration approach

Map Tailwind colors to CSS variables, for example:
- `background: rgb(var(--color-bg-app) / <alpha-value>)`
- `surface: rgb(var(--color-surface) / <alpha-value>)`
- `content: rgb(var(--color-text-primary) / <alpha-value>)`
- `brand: rgb(var(--color-brand) / <alpha-value>)`

This gives you:
- light/dark switching without component rewrites
- future custom themes with no component changes
- cleaner design consistency across browse and admin apps

## Proposed Delivery Milestones

### Phase 0: Discovery and Frontend Foundation

Deliverables:
- confirm final route map against the backend endpoints
- decide single-app-with-route-groups vs two-app workspace
- scaffold React + Tailwind project structure
- configure shared API client, router, query provider, and theme provider
- implement base tokens for light and dark themes

Outcome:
- a working skeleton app with theming, routing, and API plumbing in place

### Phase 1: Shared Design System and Infrastructure

Deliverables:
- primitive UI components (`Button`, `Input`, `Card`, `Dialog`, `Table`, `Badge`, `Skeleton`, `Toast`)
- app shells for browse and admin
- reusable loading, empty, and error states
- semantic Tailwind token mappings
- responsive layout primitives

Outcome:
- both UIs can be assembled quickly from the same visual language

### Phase 2: Browse UI MVP

Deliverables:
- top-level group landing page
- group detail/browse page with child groups and group items
- item detail page
- pagination, filtering, breadcrumbs, and thumbnail rendering
- URL-driven browse state

Outcome:
- end users can navigate the media catalog comfortably on desktop and mobile

### Phase 3: Admin CRUD MVP

Deliverables:
- groups management screens
- group create/edit/delete workflows
- items management screens
- item create/edit/delete workflows
- basic confirmation dialogs and success/error feedback

Outcome:
- administrators can maintain the catalog end-to-end without touching the backend directly

### Phase 4: Thumbnail Management and UX Polish

Deliverables:
- group and item thumbnail upload/replace/remove controls
- drag-and-drop upload UX
- image preview and fallback handling
- skeletons, polished transitions, better empty states, richer table/card toggles

Outcome:
- the UI feels modern and media-centric rather than purely functional

### Phase 5: Hardening and Refinement

Deliverables:
- responsive QA across breakpoints
- accessibility pass on focus, contrast, keyboard nav, and dialogs
- error recovery improvements
- optional saved filters/recent navigation conveniences
- optional third theme to prove extensibility of the token system

Outcome:
- a more production-ready frontend structure, even if auth is still out of scope

## Further Considerations

1. Recommended structure: one repo, two apps (`browse` and `admin`), one shared component/theme package; simpler than fully separate repos while still maintaining clear separation.
2. For thumbnails, prefer multipart upload in the admin UI, with a fallback base64 path only if you later need clipboard/paste workflows.
3. Because there’s no auth in v1, keep separation purely by app/route; if this later needs access control, the admin app can be gated without redesigning the browse UI.
4. If implementation speed matters more than architectural purity, start as a single Vite app with `/browse` and `/admin` route groups, then split into two app entry points later.
5. Keep all component styling mapped to semantic tokens from day one; that decision will pay off immediately when refining dark mode and adding future themes.

