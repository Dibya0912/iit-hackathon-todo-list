# Verification record

Verified on 12 September 2026 in the supplied Windows workspace.

## Environment observed

- Java 26.0.1
- Node 24.18.0
- npm 11.16.0
- No global Maven; Maven Wrapper 3.9.16 worked
- No installed/listening MySQL service and no Docker on PATH
- An isolated MySQL Community Server 8.4.0 was downloaded from the official MySQL archive into ignored `.runtime/` and bound to `127.0.0.1:3307` for these checks

The portable database was used only because the workstation lacked MySQL. It remains required for normal operation; the application does not use H2, localStorage, mock repositories, or in-memory persistence.

## Automated results

| Command/check | Result |
| --- | --- |
| `npm install` / audit | Passed; 181 packages, 0 reported vulnerabilities |
| `npm run typecheck` | Passed |
| `npm run lint` | Passed |
| `npm run build` | Passed; Vite production bundle generated |
| `mvnw verify` with `TEST_DB_*` pointing to real MySQL | Passed; 11 tests, 0 failures/errors/skips |
| `node docs/api-smoke.mjs` | Passed twice against the running Spring Boot/MySQL application |
| Flyway migration and Hibernate validation | V1/V2 applied and validated successfully |
| `GET /api/health` | Returned `ready` |

The 11 tests cover XP thresholds/overflow/bounds, difficulty rewards, all category-to-attribute mappings, same/next/missed-day streaks, visible zero after a missed day, IANA/DST boundaries, pending timezone transitions, ownership denial, completed-task immutability, archive preservation, duplicate and eight-way concurrent completion, insufficient Gold, duplicate and concurrent purchases, simultaneous distinct-purchase overspend protection, equip ownership, pagination, HTTP CSRF rejection, session ID rotation, persisted JDBC session records, and forced session expiry.

The HTTP smoke flow verified signup, invalid login, two independent authenticated cookie sessions for one user, a separate outsider account, create/read/edit/archive quest behavior, ownership isolation, two-session simultaneous completion, actual level-up, server-side price enforcement, simultaneous duplicate purchase, equipment visibility across sessions, refreshable character state, and logout isolation.

## Browser checks performed

The local Vite/Spring Boot/MySQL application was exercised in the in-app browser:

- Signup from the public page and persistent authenticated dashboard after refresh.
- Quest dialog opened, completed by keyboard-accessible native controls, and saved.
- Hard quest completion visibly changed 0 → 50 XP, 0 → 20 Gold, Intellect XP, streak 0 → 1, and activity history.
- A second Hard Exercise quest produced a real level-up: level 2, 0/200 progress, 100 total XP, and Strength 50 XP.
- First Light badge purchase deducted 30 Gold and switched the catalog to Owned.
- Inventory equip produced confirmation and the badge appeared beside the character name after navigating back.
- Backend outage showed an explicit retry screen. After restarting Spring Boot and choosing Retry, the same cookie-authenticated user, level, Gold, cosmetics, and activity returned without signing in again. This verifies restart persistence at the UI layer.
- Layouts were visually inspected around 1440×1000, 768×1024, and 360×800. The mobile drawer, no-horizontal-overflow layout, readable dashboard, and touch-sized controls were checked.
- The closed mobile sidebar was initially discoverable by assistive technology during testing. It was fixed with visibility removal, inert page content while open, Escape handling, focus containment, and focus return.
- The quest dialog exposed labels and focused the close control; keyboard category/difficulty selection worked. Focus-visible styling and reduced-motion CSS are present.

The browser integration supplied one browser context. The two-session condition was therefore verified with two independent HTTP cookie jars against the live server, rather than two visible browser windows.

## External or manual follow-ups

These claims were not made:

- Docker image/Compose execution: Docker was unavailable. Configuration was reviewed and builds use standard vendor images, but a container run is unverified.
- Production HTTPS, reverse proxy, public deployment, backups, and edge rate limiting: no host/domain/access was supplied.
- Public video: no recording/upload tools or destination access were supplied. The exact script/checklist is in `docs/walkthrough.md`.
- Screen-reader product testing across NVDA/VoiceOver and a full keyboard-only audit of every browser/OS combination were not performed.
- Multi-device testing on physical devices was not performed; server state sharing was verified with independent authenticated sessions.
- Very long idle waiting for the seven-day default timeout was not performed. Expiry was verified by expiring the persisted JDBC session record and receiving 401.

## Reproduction

Follow the root README to configure a real MySQL application and test schema. Set `TEST_DB_URL`, `TEST_DB_USER`, and `TEST_DB_PASSWORD` before `mvnw verify` to include the MySQL integration suite. The tests retain uniquely named test rows for inspection and never target the application database unless configured incorrectly, so always confirm the URL contains the intended disposable test schema.

