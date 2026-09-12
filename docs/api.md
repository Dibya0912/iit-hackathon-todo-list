# API contract

All paths begin with `/api`. Requests/responses use JSON except successful 204 responses. Mutations require a CSRF header and an authenticated session except signup/login, which still require CSRF.

## Session protocol

1. GET `/auth/csrf` with cookies enabled. Response: `{"token":"masked-token","headerName":"X-CSRF-TOKEN"}`.
2. POST signup or login with that header. Accept the HttpOnly SESSION cookie; the server rotates the session ID.
3. Discard the old CSRF token after login/signup/logout and fetch another before the next mutation.
4. Send credentials on same-origin API calls. Never copy the cookie to localStorage.
5. POST logout invalidates the session. 401 means sign in; 403 means reject the mutation and reacquire the token before a user-triggered retry. Do not automatically replay reward/purchase mutations.

## Endpoints

| Method/path | Input/query | Response |
| --- | --- | --- |
| GET /health | None; public | 200 ready or 503 unavailable |
| GET /auth/csrf | None; public | Token/header name |
| POST /auth/signup | displayName, email, password, timezone | 201 UserView, authenticated session |
| POST /auth/login | email, password | 200 UserView |
| POST /auth/logout | None | 204 |
| GET /auth/me | None | UserView |
| GET /character | None | HeroView |
| PATCH /profile | displayName, timezone, avatar | HeroView |
| GET /tasks | status=pending/completed/archived, optional category, sort=newest/due, page=0, size=20 | PageView<Quest> |
| POST /tasks | QuestInput | 201 Quest |
| GET /tasks/{id} | Owned quest ID | Quest |
| PATCH /tasks/{id} | QuestInput; pending only | Quest |
| DELETE /tasks/{id} | Owned quest ID | 204; archives |
| GET /leaderboard?page=0&size=25 | — | Current UTC week, countdown, paged global rankings, and current user's rank |
| POST /tasks/{id}/complete | No client reward/timestamp | CompletionResult |
| GET /shop | None | ItemView[] |
| POST /shop/{id}/purchase | No client price | PurchaseResult |
| GET /inventory | None | Owned ItemView[] |
| PATCH /inventory/{id}/equip | **Inventory ID**, not shop ID | HeroView |
| PATCH /inventory/{id}/unequip | Inventory ID | HeroView |
| GET /activity | page=0,size=20 | PageView<Activity> |

Page sizes clamp to 1–50. Page numbers clamp at zero. Ownership failures return 404 without revealing another user's record.

## Shapes

```typescript
type UserView = { id: number; displayName: string; email: string }
type Progress = { level: number; current: number; required: number; total: number }
type QuestInput = {
  title: string; description?: string;
  category: 'CODING'|'STUDY'|'EXERCISE'|'READING'|'MEDITATION'|'COMMUNICATION';
  difficulty: 'EASY'|'MEDIUM'|'HARD';
  dueDate?: string | null; // YYYY-MM-DD
}
type Quest = QuestInput & {
  id: number; completed: boolean; archived: boolean;
  createdAt: string; updatedAt: string; completedAt: string | null;
  rewardXp: number; rewardGold: number; // authoritative preview
}
type ItemView = {
  id: number; name: string; slot: 'FRAME'|'BADGE'|'THEME';
  appearance: string; description: string; price: number;
  inventoryId: number|null; owned: boolean; equipped: boolean;
}
type HeroView = {
  displayName: string; avatar: 'RANGER'|'MAGE'|'KNIGHT';
  progress: Progress; gold: number; streak: number; bestStreak: number;
  timezone: string; pendingTimezone: string|null;
  attributes: { type: string; progress: Progress }[];
  cosmetics: ItemView[]; // equipped only
}
type CompletionResult = {
  applied: boolean; xp: number; gold: number;
  levelUp: boolean; character: HeroView;
}
type PurchaseResult = { applied: boolean; character: HeroView }
type Activity = {
  id: number; kind: 'QUEST'|'PURCHASE'; description: string;
  xp: number; gold: number; createdAt: string;
}
type PageView<T> = { content: T[]; page: number; totalPages: number; totalElements: number }
```

Timestamps are server UTC ISO-8601 strings. Due dates are dates, not instants. A successful completion also returns `leaderboardXp` and `elapsedSeconds`; completions under 300 seconds receive zero leaderboard XP. Scores use capped tiers of 1.25× at 5 minutes, 1.5× at 10 minutes, 1.75× at 30 minutes, and 2× at 60 minutes or longer. Activity results include quest creation, completion, elapsed time, and leaderboard score fields. Requesting a completion twice returns `applied:false`, zero new reward, and current state. Purchasing an owned item returns `applied:false`.

The completion snapshot stores the original title, category, XP, Gold, completion timestamp, resolved activity date, and zone. The ledger retains quest reward and purchase deltas.

## Validation and errors

Titles are trimmed/nonblank, 1–120 chars; descriptions max 1,000; display names trimmed/nonblank max 60; emails max 254, case-normalized; passwords 10–72 chars and max 72 UTF-8 bytes at signup. Timezones must exist in Java's IANA zone set.

```json
{
  "code": "VALIDATION",
  "message": "Check the highlighted fields.",
  "fields": { "title": "must not be blank" }
}
```

Stable codes include VALIDATION, INVALID_REQUEST, INVALID_TIMEZONE, LOGIN_FAILED, SIGNUP_UNAVAILABLE, RATE_LIMITED, UNAUTHENTICATED, FORBIDDEN, NOT_FOUND, READ_ONLY, ARCHIVED, INSUFFICIENT_GOLD, PROGRESSION_LIMIT, CONFLICT, INTERNAL_ERROR.

Status codes: 400 invalid input, 401 no session/bad credentials, 403 CSRF/access rejection, 404 inaccessible/missing record, 409 conflict, 429 throttled, 500 unexpected failure. Internal exception details stay in server logs.

