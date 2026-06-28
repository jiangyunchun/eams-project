# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EAMS (Enterprise Asset Management System) — a Spring Boot + Vue 3 monorepo for managing fixed assets through their full lifecycle: asset ledger, procurement, requisition/return, transfer, repair, scrap, inventory counting, RBAC permission control, and AI-powered query.

## Development Commands

### Backend (`eams-backend/`)

```bash
# Compile (Maven 3.6.0 with custom settings)
"D:/Program Files/apache-maven-3.6.0/bin/mvn" -s "D:/Program Files/apache-maven-3.6.0/conf/settings-eams.xml" clean compile

# Run Spring Boot (development)
"D:/Program Files/apache-maven-3.6.0/bin/mvn" -s "D:/Program Files/apache-maven-3.6.0/conf/settings-eams.xml" spring-boot:run

# Package as JAR
"D:/Program Files/apache-maven-3.6.0/bin/mvn" -s "D:/Program Files/apache-maven-3.6.0/conf/settings-eams.xml" clean package -DskipTests
```

No tests exist yet — `-DskipTests` is implicit if there are none.

### Frontend (`eams-frontend/`)

```bash
# Install dependencies
npm install

# Dev server (port 5173, proxies /api and /uploads to localhost:8080)
npx vite

# Dev server with network access
npx vite --host

# Build for production
npx vite build
```

### Database

```bash
# MySQL 8.0 local connection
"C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" -u root -pAdmin@123456 -h 127.0.0.1 --default-character-set=utf8mb4

# Initialize the database
"C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" -u root -pAdmin@123456 -h 127.0.0.1 --default-character-set=utf8mb4 < sql/init.sql
```

Created databases per `application.yml`: `eams` (dev), `eams_prod` (production).

### Admin Credentials

All pre-seeded users in `sql/init.sql` have password `Eams@123456` (BCrypt-encoded). Super admin: `admin` / `Eams@123456`.

## Architecture

### Backend (Spring Boot 2.3.12 + Java 8)

**Package: `com.example.eams`**

The backend uses a **domain-driven package structure**, with each business module containing its own controller/dto/entity/mapper/service layers:

```
system/     — RBAC: users, roles, departments, menus, dicts, config, operation/login logs
asset/      — Asset ledger (CRUD, Excel import/export, depreciation)
common/     — Cross-cutting: config, exception handling, Result/PageResult wrappers, utilities
security/   — Custom JWT auth filter, @RequireRole annotation + AOP, ThreadLocal context
```

Stub packages exist for: `procurement/`, `requisition/`, `transfer/`, `repair/`, `scrap/`, `inventory/`, `ai/` — directory structure created but no Java files yet.

**Key architectural decisions:**

- **No Spring Security** — uses a custom `JwtAuthenticationFilter` (`@Order(1)` servlet filter) + `@RequireRole` AOP annotation for authorization. Simpler, no framework lock-in.
- **Static utility pattern** — `JwtUtil`, `RedisUtil`, `AIClientUtil` are static classes. Spring-managed beans are injected into them via `StaticBeanInitConfig.@PostConstruct`. This means these utils can be called anywhere without `@Autowired`.
- **JWT blacklist** — on logout, the token's `jti` is stored in Redis with its remaining TTL. The filter checks the blacklist on every authenticated request.
- **Login brute-force protection** — Redis keys `eams:login:fail:{username}` counter, locks account for 30 minutes after 5 failures (`eams:login:lock:{username}`).
- **Auto-fill audit fields** — `MyMetaObjectHandler` reads `username` from `SecurityContextHolder` (ThreadLocal) to populate `createBy`/`updateBy`. The filter sets the context on request entry and **clears it in a `finally` block** — never hold references across threads.
- **Logical delete** — all entities use MyBatis-Plus `@TableLogic` with `is_deleted` column. App-wide logical delete is configured in `application.yml`.
- **Uniform API response** — every endpoint returns `Result<T>` (`{ code, message, data }`) or `PageResult<T>` (`{ total, pageNum, pageSize, pages, list }`). Use the static factory methods (`Result.ok(data)`, `Result.fail(code, msg)`, etc.).
- **Operation logging** — `@OperationLog(module, actionType, description)` on controller methods, processed by `LogAspect` AOP. `{0}` in description is replaced with the first method parameter.

### Frontend (Vue 3 + Vite + Element Plus)

**Path alias:** `@` → `./src`

**Directory structure:**

```
src/
  router/index.js     — Vue Router (history mode, auth guard via meta.perm)
  store/              — Pinia: auth (token/user/perms), dict (full dict cache), config (system params cache)
  utils/request.js    — Axios instance: /api baseURL, Bearer token interceptor, global loading overlay
  api/                — API functions (system.js, asset.js, system/*.js)
  layouts/AdminLayout — Main admin shell: collapsible sidebar + breadcrumb + user dropdown
  components/common/  — Shared components (ConfirmDialog, DictSelect, DeptTreeSelect, FileUpload, etc.)
  views/              — Page components by module
```

**Key patterns:**

- **Auth flow:** Login → store token in localStorage → load dict cache (`/api/system/dict/all`) → load config cache → fetch user info. Token sent as `Authorization: Bearer <token>` on every request.
- **Permission model:** Menu visibility via `checkPerm(perm)` checking `auth.permissions` array. Route-level guard via `meta.perm` (only `asset:list` used so far). Server-side uses `@RequireRole` for role-based checks.
- **Router vs sidebar mismatch:** The `AdminLayout` sidebar renders menu entries for ALL 9 modules (with permission checks), but `router/index.js` only defines routes for Dashboard and Asset List. Adding a new business module requires both a route definition and the view component.
- **Global loading:** Axios interceptor shows Element Plus loading overlay for non-GET requests. Disable per-request with `showLoading: false` in config.
- **No TypeScript** — the frontend is pure JavaScript with Vue Composition API (`<script setup>`).

### Database

27 tables in 7 groups: system permissions (10), system logs (2), asset ledger (3), procurement (2), requisition (2), transfer/repair/scrap/inventory/AI (7), AI log (1). All use `utf8mb4`, InnoDB, soft-delete, and audit columns. See `../sql/init.sql` for full schema and seed data.

## Adding a New Business Module

When adding a new domain (e.g., procurement, repair):

1. **Backend:** Create package `com.example.eams.{module}/` with sub-packages: `controller/`, `dto/`, `entity/`, `mapper/`, `service/impl/`. Follow the pattern in `system/` or `asset/`: entity extends a base pattern with `@TableLogic`, mapper extends `BaseMapper<T>`, service uses `@Transactional`, controller methods annotate with `@RequireRole` and `@OperationLog`.
2. **Frontend:** Create `src/views/{module}/` with page components, add API functions in `src/api/`, add route in `src/router/index.js`, menu entries already exist in `AdminLayout.vue`.
3. **Database:** Tables already exist in `sql/init.sql` for all planned modules.

## Configuration Notes

- **Maven:** Project uses a custom Maven at `D:/Program Files/apache-maven-3.6.0/` with `conf/settings-eams.xml`. Always use the full path with `-s` flag.
- **Spring profiles:** `dev` (default) uses hardcoded local creds and SQL logging; `prod` reads all secrets from environment variables (`DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PASSWORD`, `JWT_SECRET`, `DEEPSEEK_API_KEY`).
- **File uploads:** Stored at `D:/Projects/eams-project/eams-backend/upload/`, served at `/uploads/**`. Max 10MB per file, 50MB per request.
- **Druid monitoring:** Available at `http://localhost:8080/druid/` (dev only).
- **Frontend proxy:** Vite proxies `/api` and `/uploads` to `http://localhost:8080`. Ensure the backend is running on 8080 before starting the frontend.
