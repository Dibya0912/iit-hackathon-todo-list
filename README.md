# LevelForge

A Life RPG for turning everyday intentions into character progression. Create a quest, complete the activity, earn XP and Gold, grow five attributes, climb the weekly leaderboard, and collect visible cosmetics.

## What is implemented

- Public introduction, signup, login, logout, protected routes, and persistent MySQL-backed sessions.
- Character dashboard with original bundled SVG artwork, derived levels, XP progress, Gold, streaks, attributes, and recent activity.
- Quest journal: create, detail view, pending-only edit, archive, completion, category/status filters, sorting, and pagination.
- Eight cosmetic items: three frames, three badges, two themes. Purchases, inventory, equip/unequip, and visible persistent appearance.
- Profile: display name, three avatars, validated IANA timezone, statistics, and paginated economy history.
- Native accessible dialogs, keyboard navigation, mobile drawer, loading/error/retry states, restrained reward animation, and reduced-motion support.
- Server-authoritative rewards with serialized per-character transactions, ownership checks, unique completion/inventory constraints, and an immutable completion snapshot.

Completion is self-reported. LevelForge protects reward/data integrity; it does not verify real-world performance.

## Repository

```text
frontend/   React application, lockfile, Nginx configuration, Dockerfile
backend/    Spring Boot, Maven Wrapper, Java tests, Flyway migrations
.vscode/    Extension recommendations, tasks, debug configurations
docs/       API contract, verification record, walkthrough, HTTP smoke test
compose.yaml
```

## Selected stack

| Component | Version |
| --- | --- |
| React / React DOM | 19.3.0 |
| TypeScript | 6.0.3 |
| Vite / React plugin | 8.3.0 / 6.1.1 |
| Tailwind CSS | 4.3.3 |
| Framer Motion | 13.2.0 |
| React Router | 7.18.3 |
| TanStack Query | 5.102.8 |
| Lucide React | 1.45.0 |
| Spring Boot | 4.0.3 |
| Maven Wrapper / Maven | 3.3.4 / 3.9.16 |
| Java compilation target | 21 |
| MySQL | 8.4 LTS family |

All frontend direct dependencies are pinned; transitive dependencies are in `frontend/package-lock.json`. Spring Boot's fixed parent manages backend dependencies. The wrapper pins Maven. The build was exercised with Java 26.0.1, Node 24.18.0, npm 11.16.0, and an isolated MySQL 8.4.0 instance.

Compatibility references: [Spring Boot requirements](https://docs.spring.io/spring-boot/4.0/system-requirements.html), [Vite requirements](https://vite.dev/guide/). Use a maintained MySQL 8.4 patch for deployment; the portable 8.4.0 archive was used only for local verification.

## Prerequisites

- VS Code and recommended extensions (accept the workspace recommendations).
- JDK 21 or a compatible newer JDK, with `java` available on PATH. Set `JAVA_HOME` if the wrapper cannot locate it.
- Node 24 LTS and npm.
- MySQL 8.4 service, either an existing local installation or optional Docker Compose.
- Internet access on the first Maven/npm run.

No IntelliJ, Eclipse, Spring Tool Suite, global Maven installation, or MySQL Workbench is required.

On the supplied workstation Java and Node were present; MySQL, Docker, and global Maven were absent. For verification, an isolated portable MySQL was installed under ignored `.runtime/`, listening only on `127.0.0.1:3307`. A generated local `backend/.env` points to it. This runtime and its credentials are not part of the source deliverable. Keep a normal MySQL service for long-term use.

## Windows / VS Code setup

Open this folder in VS Code. Use PowerShell integrated terminals.

### 1. Create the databases

Connect with the MySQL command-line client as your database administrator:

```powershell
mysql -u root -p
```

Run, replacing the example password:

```sql
CREATE DATABASE levelforge CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE levelforge_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'levelforge'@'localhost' IDENTIFIED BY '<generate-a-unique-value>';
GRANT ALL PRIVILEGES ON levelforge.* TO 'levelforge'@'localhost';
GRANT ALL PRIVILEGES ON levelforge_test.* TO 'levelforge'@'localhost';
```

The application does not need global database privileges. In production, run migrations with a migration identity, then use a runtime identity with only SELECT/INSERT/UPDATE/DELETE on the application schema. Do not use the test database in production.

### 2. Configure the backend

```powershell
Copy-Item backend/.env.example backend/.env
code backend/.env
```

Set `DB_URL`, `DB_USER`, and `DB_PASSWORD`. Default local URL:

```text
jdbc:mysql://localhost:3306/levelforge?connectionTimeZone=UTC
```

**Spring Boot does not automatically load .env.** The supplied `backend/run.ps1` explicitly reads simple `KEY=value` lines into process environment variables. It does not execute the file. Do not add quotes or shell expressions to these values. The VS Code Backend: dev task calls this loader on Windows. The Java debug configuration uses the Java extension's `envFile`.

### 3. Start in two terminals

Backend, using the environment loader:

```powershell
cd backend
.\run.ps1
```

Equivalent direct Maven Wrapper command, when environment variables are already set:

```powershell
cd backend
$env:DB_URL='jdbc:mysql://localhost:3306/levelforge?connectionTimeZone=UTC'
$env:DB_USER='levelforge'
$env:DB_PASSWORD='<value-from-your-password-manager>'
.\mvnw.cmd spring-boot:run
```

Frontend, in a second terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open [LevelForge locally](http://127.0.0.1:5173). Use this hostname consistently so the browser session stays on one origin. Vite proxies `/api` to Spring Boot on port 8080.

Alternatively: **Terminal → Run Task → LevelForge: dev** starts both tasks after dependencies/configuration are ready. **Run Build Task** invokes frontend build/lint and backend verification. F5 offers Java or frontend debugging; start the frontend server first.

### macOS / Linux differences

Use `./mvnw` instead of `.\mvnw.cmd`. If needed, run `chmod +x backend/mvnw`. Export backend variables in the integrated terminal before running the wrapper:

```bash
cd backend
export DB_URL='jdbc:mysql://localhost:3306/levelforge?connectionTimeZone=UTC'
export DB_USER='levelforge'
export DB_PASSWORD='<value-from-your-password-manager>'
./mvnw spring-boot:run
```

The Windows `run.ps1` loader is optional convenience; on other systems use explicit exports or a trusted environment manager. Frontend npm commands are identical.

## Migrations and seeds

Flyway V1 creates the normalized application and Spring Session JDBC tables. V2 inserts only the eight-item catalog. Hibernate uses `ddl-auto=validate`; it never recreates the database. Subsequent launches validate migration checksums and reuse existing data.

Every signup creates one level-1 character, zero XP/Gold/attributes/streak, Ranger avatar, and default appearance. There are no fake dashboard metrics or automatic completed demo quests. Tests create uniquely named accounts and actual transactions, isolated from normal signup.

The shared leaderboard lists every registered adventurer and ranks the current Monday-to-Monday UTC week by XP earned from completed quests. Rankings refresh every 15 seconds. After the week closes, qualifying places receive one server-side award: #1 gets 1000 XP, #2 gets 500 XP, #3 gets 200 XP, and #4–#10 get 100 XP each. Zero-score accounts remain visible but do not receive a prize. Higher quest XP immediately moves a player ahead; equal scores use the older account as a stable tie-break. Awards are recorded once in `leaderboard_rewards` and the economy ledger.

## Build and test

```powershell
cd frontend
npm ci
npm run typecheck
npm run lint
npm run build
cd ../backend
.\mvnw.cmd verify
```

Without `TEST_DB_URL`, Maven runs the pure rule tests and explicitly skips the MySQL integration class. That is **not** a concurrency verification.

To run all tests against a real isolated MySQL database:

```powershell
cd backend
$env:TEST_DB_URL='jdbc:mysql://localhost:3306/levelforge_test?connectionTimeZone=UTC'
$env:TEST_DB_USER='levelforge'
$env:TEST_DB_PASSWORD='<value-from-your-password-manager>'
.\mvnw.cmd verify
```

Use `export NAME=value` and `./mvnw verify` on macOS/Linux. Tests expect a disposable test schema, create uniquely named fixtures, and retain them for inspection; they never truncate the production database.

With the local servers running, from the workspace root:

```powershell
node docs/api-smoke.mjs
```

This HTTP smoke suite creates test accounts in the running application's database. Use a local/test deployment. It checks actual CSRF, session rotation, ownership, concurrent mutations, CRUD, levels, equipment, independent cookie sessions, and logout. See [verification](docs/verification.md) for executed results and limits.

## Game rules

| Difficulty | XP | Gold |
| --- | ---: | ---: |
| Easy | 10 | 5 |
| Medium | 25 | 10 |
| Hard | 50 | 20 |

The same XP goes to the category's attribute: Coding/Study → Intellect; Exercise → Strength; Reading → Wisdom; Meditation → Focus; Communication → Charisma.

Level L starts at `50 × L × (L−1)` total XP. Advancing from L needs `100 × L` XP. Levels derive from total XP; overflow is retained, including multiple thresholds. At 115 XP, the character is level 2 with 15/200. Attribute levels use the same function. XP and Gold are bounded to 1,000,000,000.

A local activity date counts once. First activity yields streak 1; the next date increments; a missed full date displays zero and the next completion restarts at 1. Yesterday's streak is retained while today is still open. Best streak never decreases.

Timezone changes during an active day are pending until its window ends. At the next completion outside that window, the new zone activates and the previous completion is reinterpreted in the new zone before comparing dates. Moving the clock forward or backward through timezone changes therefore cannot grant an extra increment. Activity records retain their original UTC timestamp, local date, timezone, and deterministic window boundaries. DST uses real IANA start-of-day boundaries, not 24-hour arithmetic. A pending timezone is applied lazily on the first subsequent completion, or can be applied by saving the profile after the old window ends.

## Security and integrity

- BCrypt cost 12; passwords are 10–72 characters and at most 72 UTF-8 bytes on signup.
- Same-origin routing; no wildcard CORS.
- Spring Security server-side authentication, HttpOnly session cookie, SameSite=Lax, session ID rotation at login/signup, server invalidation on logout.
- Spring Session JDBC persists sessions across backend restarts. Default idle timeout is seven days; `SESSION_TIMEOUT` can shorten it.
- CSRF remains enabled. The frontend obtains a masked token from `/api/auth/csrf` and sends its returned header for every mutation. Login/logout rotates or clears the token; stale tokens yield a retryable error without silently replaying a mutation.
- User identity comes only from the authenticated principal. No client XP/Gold/prices/ownership values are accepted.
- A character row is locked first for every quest/profile/economy mutation. This serializes one user's concurrent updates. Task and inventory lookups occur under that lock and enforce ownership.
- Unique task completion and non-stackable inventory constraints provide database-level protection. Duplicate completion/purchase returns `applied:false`, with no second reward/charge.
- Completed quests cannot be edited. DELETE archives; it never erases the completion or ledger.
- Login/signup throttling: 20 attempts per remote address per 15 minutes, bounded to 10,000 keys, in one JVM. This is intentionally basic. With a reverse proxy, the app sees the proxy address; add a trusted edge per-client limiter for deployment. Multiple instances need shared/edge rate limiting.
- No auth tokens/passwords or primary game data in localStorage. Queries reconcile from the server and never automatically replay offline mutations.
- API responses use DTOs. Exceptions never expose hashes or stack traces to clients.

## Configuration and deployment

| Variable | Meaning |
| --- | --- |
| DB_URL | MySQL JDBC URL; use UTC connection time zone |
| DB_USER / DB_PASSWORD | Backend-only database credentials |
| PORT | Backend listener, default 8080 |
| COOKIE_SECURE | false for localhost HTTP; **true for production HTTPS** |
| SESSION_TIMEOUT | Idle session timeout, default 7d |

No backend secret belongs in a `VITE_` variable. The frontend needs no environment secrets.

Optional MySQL-only Compose:

```powershell
Copy-Item .env.example .env
# Edit both passwords in .env
docker compose up -d mysql
```

Optional full-stack Compose:

```powershell
docker compose --profile full up --build -d
```

This serves the frontend and same-origin API at [local Compose endpoint](http://127.0.0.1:8088), with MySQL in a named volume. Do not run `docker compose down -v` unless intentionally deleting this database.

For production: provision a host capable of running Java/containers and persistent MySQL; terminate HTTPS at your load balancer; route to Nginx; set `COOKIE_SECURE=true`; keep Java/MySQL private; configure credentials and backups; run migrations; add edge rate limiting. Nginx serves the Vite build and proxies `/api/` to the backend. A missing DB prevents successful application startup; `GET /api/health` returns readiness only and uses status 503 if a running application's DB becomes unavailable. No secret details are returned.

The container setup is prepared but was not executed because Docker is unavailable here. No hosting credentials, domain, or deployment target were supplied, so no public deployment is claimed.

## Hackathon submission

- [150-second walkthrough and recording checklist](docs/walkthrough.md)
- [API contract](docs/api.md)
- [Verification record and manual follow-ups](docs/verification.md)

Repository: [Dibya0912/iit-hackathon-todo-list](https://github.com/Dibya0912/iit-hackathon-todo-list). The implementation was organized into three current-time development commits before its first push. No public video or deployment URL exists yet.

## Known limits

One-time quests only. No password recovery or email verification, recurring quests, payments, direct messaging, or teams. Basic local throttling needs a deployment-level replacement at scale. Java serialization in JDBC sessions may require session invalidation when upgrading incompatible application/security versions. Full device/screen-reader coverage and production HTTPS/container execution remain deployment follow-ups. Check the verification record for precisely what ran.
