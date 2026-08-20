# AutoCare — Vehicle Service Management System

PUSL2024 Software Engineering 2 — Referral Coursework 2025–2026, element **C1**
**Dasun Edirisinghe — 10965261**

A Spring Boot web application for a motor garage. Customers register vehicles and
book services; garage staff plan each job's parts and labour, then complete it —
which consumes the parts from stock and raises the invoice in one atomic operation.

---

## Running it

Requires **JDK 17 or later** (built and tested on JDK 21). Maven is *not* required —
the Maven wrapper downloads it.

```bash
./mvnw spring-boot:run          # macOS / Linux
mvnw.cmd spring-boot:run        # Windows
```

Then open <http://localhost:8081>. (Port 8081, so it can run alongside the other
referral project without a clash.)

To run the tests:

```bash
./mvnw test
```

### Demonstration accounts

Created on first start by [`DataSeeder`](src/main/java/lk/ac/nsbm/autocare/config/DataSeeder.java).

| Username | Password | State | Demonstrates |
|---|---|---|---|
| `10965261` | `customer123` | 2 vehicles, 1 open job, 1 invoiced job | successful booking, invoice history |
| `10965261B` | `customer123` | 2 open jobs | "too many open jobs" rejection |
| `k.perera` | `customer123` | 1 open job | "garage fully booked" rejection |
| `s.fernando` | `customer123` | 1 open job | filler for the full day |
| `admin10965261` | `admin123` | garage staff | inventory CRUD, job completion |

The seeder logs the date on which the diary is **full**, for the capacity rejection.

### Database

H2 in **file mode** at `./data/autocare.mv.db`, so data survives a restart.

Console: <http://localhost:8081/h2-console>
JDBC URL: `jdbc:h2:file:./data/autocare` — user `sa`, no password.

Deleting the `data/` directory resets everything.

---

## The five required components

| # | Requirement | Where it lives |
|---|---|---|
| 1 | **Data management** with categories and relationships | `PartCategory` 1→* `Part`; `Customer` 1→* `Vehicle` 1→* `ServiceJob` 1→* `JobLine` *→1 `Part`. `Part` uses JPA `JOINED` inheritance with `ConsumablePart` / `MechanicalPart`; `AppUser` uses `SINGLE_TABLE` with `Customer` / `StaffMember`. |
| 2 | **CRUD + validation** via REST and Thymeleaf | `AdminPartController` (web) and `PartRestController` (`/api/parts`) over the same `PartAdminService`. `PartForm` / `VehicleForm` / `BookingForm` carry Bean Validation constraints; `GlobalExceptionHandler` and `RestExceptionHandler` handle failures centrally. |
| 3 | **Transactional workflow** | Booking (`bookService`) and completion (`completeJob`) in `ServiceJobServiceImpl`, both `@Transactional`, with confirmation pages, rollback on failure, and eight custom exception types. |
| 4 | **Session management and roles** | `SecurityConfig`: `ROLE_ADMIN` vs `ROLE_CUSTOMER`, BCrypt passwords, session-fixation protection, `maximumSessions(1)`, plus `@PreAuthorize` on the service. |
| 5 | **Professional interface and standards** | Thymeleaf + Bootstrap 5 (vendored locally), strict Controller → Service → Repository → Entity layering, constructor injection throughout. |

---

## Architecture

```
Controller   HTTP translation and view selection. No business rules,
   ↓         no repository calls, no try/catch.
Service      Transaction boundary and all business rules.
   ↓
Repository   Spring Data JPA queries. No decisions.
   ↓
Entity       Persistent state, guarding its own invariants.
```

### Key design decisions

**The atomic operation.** `completeJob` consumes every planned part in a loop and
then prices the invoice. The shortfall may only be discovered on the last line, by
which point earlier parts have already been decremented. Because the method is
`@Transactional` and `InsufficientPartStockException` is unchecked, Spring's proxy
rolls the whole transaction back — every earlier decrement is undone and the job
stays open. Without it, each write would commit independently and a failure part-way
through would destroy inventory no job ever used. `JobCompletionTransactionTest`
proves this.

**Concurrency.** `PartRepository.findByIdForUpdate` takes a `PESSIMISTIC_WRITE` lock
and `Part` carries a `@Version` column, so two jobs completing simultaneously cannot
both read the same stock figure and both decrement it.

**Two security filter chains.** The web interface uses form login, sessions and full
CSRF protection. `/api/**` is a separate, *stateless* chain using HTTP Basic with CSRF
disabled — correct there and only there, because CSRF is an attack on ambient
credentials, and an API with no session and explicit per-request credentials has
nothing for a forged page to ride on.

**Access denied.** `ApiAwareAccessDeniedHandler` forwards browsers to the styled
`/403` page but answers `/api/**` with a real `403` and a JSON body. A plain
`accessDeniedPage` forwards while preserving the HTTP method, so a refused
`DELETE /api/parts/1` would otherwise surface as `405 Method Not Allowed`.

**Encapsulation.** `Part.stockQuantity` and `ServiceJob.status` have getters but no
setters. Stock moves only through `consumeStock` / `restock`, which refuse to go
negative; the job moves state only through `beginWork` / `complete` / `cancel`, and
`complete` computes the invoice totals from the job's own lines — a caller cannot
supply a total the line items do not support.

**Deletion.** Withdrawing a part is a **soft delete** (`active = false`). `JobLine`
rows reference `part_id`; a hard delete would either violate that constraint or, with
a cascade, erase the line items of historic invoices.

---

## Business rules

| Rule | Value | Exception raised |
|---|---|---|
| Open jobs per customer | 2 | `TooManyOpenJobsException` |
| Vehicles accepted per day | 4 | `GarageFullyBookedException` |
| Booking window | today → +90 days, closed Sundays | `InvalidBookingDateException` |
| Parts must be in stock to complete | — | `InsufficientPartStockException` |
| Nothing to invoice | — | `EmptyJobException` |
| Job already closed | — | `JobNotOpenException` |
| Part number unique | — | `DuplicatePartNumberException` |
| Registration unique | — | `DuplicateRegistrationException` |

---

## Endpoints

| Method | Path | Role | Purpose |
|---|---|---|---|
| GET | `/login` | public | Sign-in |
| GET/POST | `/my-vehicles` | CUSTOMER | List and register vehicles |
| GET/POST | `/book` | CUSTOMER | Book a service |
| GET | `/my-jobs`, `/my-jobs/{id}` | CUSTOMER | Own jobs and invoices |
| POST | `/my-jobs/{id}/cancel` | CUSTOMER | Cancel a booking |
| GET | `/admin/jobs`, `/admin/jobs/{id}` | ADMIN | Job diary and planning |
| POST | `/admin/jobs/{id}/start`, `/lines`, `/labour`, `/complete` | ADMIN | Work the job |
| GET/POST | `/admin/parts/**` | ADMIN | Inventory CRUD, stocktake, withdraw |
| GET | `/parts` | authenticated | Parts price list |
| GET | `/api/parts`, `/api/parts/{id}` | authenticated (Basic) | REST read |
| POST/PUT/DELETE | `/api/parts` | ADMIN (Basic) | REST write |
| GET | `/h2-console` | public | Database console (evidence only) |

The REST API uses HTTP Basic, so it can be called directly:

```bash
curl -u admin10965261:admin123 http://localhost:8081/api/parts
curl -u 10965261:customer123 -X DELETE http://localhost:8081/api/parts/1   # 403
```

---

## Tests

18 tests, all passing:

- `JobCompletionTransactionTest` — the short-stock rejection names the exact part;
  **a failed completion rolls back a decrement already applied earlier in the loop**;
  the job stays open; a successful completion consumes stock and the invoice
  arithmetic balances; double completion and empty jobs are refused.
- `BookingRuleTest` — all three date refusals, the open-job limit, capacity reporting,
  the daily capacity limit, cancellation restoring allowance, and that a customer can
  neither book against nor read another customer's records.
- `AutocareApplicationTests` — full context loads.
