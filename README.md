# Hisab Kitab

An employee statement app for a farm: you lend workers money, they work it off, and this
keeps the running balance straight for every person.

- **Backend** — Java 21, Spring Boot 3.4, Hibernate/JPA, PostgreSQL, Flyway, JWT auth
- **Frontend** — React 19, TypeScript, Vite, Tailwind, TanStack Query — mobile-first, laid
  out like Splitwise

---

## The domain

| Entity | What it is |
| --- | --- |
| **Organization** | The farm. Everything else belongs to exactly one. Holds the currency, time zone and weekly off days. |
| **Employer** | A person inside the farm whose own money funds the advances. Two brothers running one farm are two employers, and every entry records whose money moved. |
| **Employee** | A worker. Never a login user. Carries a daily wage rate with dated history. |
| **Transaction** (`ledger_entries`) | One movement of money, signed from the employee's point of view. |
| **Attendance** | Exception rows only — see below. |
| **WageRun** | One month of wages posted to the ledger. |

### Balance sign convention

Every ledger entry stores a positive `amount` plus a `signed_amount` that carries the
direction, so a balance is a plain `SUM(signed_amount)`:

- **negative** — the employee owes the farm (an outstanding advance)
- **positive** — the farm owes the employee (unpaid wages)

The UI reads this the way Splitwise does: green *"owes you"*, orange *"you owe"*.

| Entry type | Sign | Meaning |
| --- | --- | --- |
| `ADVANCE` | − | Cash lent to the worker |
| `EXPENSE_ON_BEHALF` | − | Employer paid a hospital, shop or school fee for them |
| `PAYOUT` | − | Earned wages handed over in cash |
| `DEDUCTION` | − | Penalty or damage recovery |
| `WAGE` | + | Wages earned — written only by a wage run |
| `BONUS` | + | Extra on top of wages |
| `REPAYMENT` | + | Worker handed cash back |
| `ADJUSTMENT` | ± | Manual correction; the caller picks the direction |

Entries are **voided, never deleted**, so a statement already read out to a worker can
always be reproduced line for line.

### Attendance is recorded by exception

Presence is the default. A working day inside the employment window with **no** attendance
row counts as a full day worked — you only mark the deviations:

| Status | Earns |
| --- | --- |
| *(no row)* | 1.0 × daily rate |
| `ABSENT` | 0 |
| `HALF_DAY` | 0.5 × |
| `PAID_LEAVE` | 1.0 × |
| `OVERTIME` | 1.5 × |

With 50 workers this means storing a handful of rows a month instead of 1,500.

### The monthly wage run

`GET /api/wage-runs/preview?month=2026-07` computes the month and **writes nothing**, so it
can be opened as often as you like. `POST /api/wage-runs` writes one `WAGE` entry per worker
and locks the month.

For each working day in the period that the worker was on the books:

```
payable_days  = Σ day_fraction(attendance)
amount        = Σ day_fraction × rate_in_force_on_that_day
```

Rates come from `wage_rates`, which is dated history rather than a single column — so
reposting an old month prices those days at the rate that actually applied then, not
today's. Weekly off days configured on the Organization are skipped entirely.

A partial unique index (`uq_wage_runs_posted_period`) allows only one **POSTED** run per
month. Voiding a run cancels its wage entries and frees the month to be corrected and
posted again — owners only.

---

## Running it

### Prerequisites

| Tool | Version | Status on this machine |
| --- | --- | --- |
| JDK | 21+ | **not installed** |
| Maven | 3.9+ | **not installed** |
| PostgreSQL | 14+, installed locally | **not installed** |
| Node | 20+ | installed ✓ |

### 1. Database

Install PostgreSQL from [postgresql.org/download/windows](https://www.postgresql.org/download/windows/)
and keep the default port 5432. The installer creates a `postgres` superuser and asks you to
set its password — the backend expects that password to be `root`, so either set it to that
or override `DB_PASSWORD`.

Then create the database once:

```bash
psql -U postgres -f backend/db/setup.sql
```

On Windows `psql` is usually not on `PATH`. Either add `C:\Program Files\PostgreSQL\<version>\bin`
to it, or call it in full from PowerShell:

```powershell
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -f backend/db/setup.sql
```

The script is safe to re-run. Flyway creates the tables on first backend start.

The backend connects as `postgres` / `root` by default. That is a superuser on your own
laptop and nothing more, but anything shared or reachable needs a dedicated role with a real
password, set through `DB_USER` and `DB_PASSWORD`:

```powershell
$env:DB_USER = "hisabkitab"
$env:DB_PASSWORD = "something-long-and-random"
```

### 2. Backend

```bash
cd backend && mvn spring-boot:run
```

Serves on `http://localhost:8080`, with API docs at `/swagger-ui.html`.

On an **empty** database it creates one organization and one owner login:

```
username: owner
password: owner123
```

Change these before real use via `BOOTSTRAP_USERNAME` / `BOOTSTRAP_PASSWORD`, and set a real
`JWT_SECRET` (at least 32 bytes). It also seeds 10 demo workers with advances and
attendance — turn that off with `SEED_DEMO=false`.

### 3. Frontend

```bash
cd frontend && npm install && npm run dev
```

Serves on `http://localhost:5173` and proxies `/api` to port 8080, so there are no CORS
round trips in development.

### Opening it on a phone

The dev server listens on all interfaces, and Vite reaches the backend server-side, so the
phone only needs port 5173 — the backend never has to leave the machine.

Find this machine's LAN address:

```powershell
Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -notlike '127.*' -and $_.PrefixOrigin -ne 'WellKnown' } | Select-Object IPAddress, InterfaceAlias
```

Allow it through the firewall once, from an **admin** PowerShell. `-RemoteAddress LocalSubnet`
keeps it reachable only from the same network, which holds whether Windows has the wifi
marked Private or Public:

```powershell
New-NetFirewallRule -DisplayName "Vite dev server" -Direction Inbound -LocalPort 5173 -Protocol TCP -Action Allow -Profile Any -RemoteAddress LocalSubnet
```

Then open `http://<that-address>:5173` on the phone, on the same wifi.

To undo it later:

```powershell
Remove-NetFirewallRule -DisplayName "Vite dev server"
```

### Troubleshooting

**403 with `Invalid CORS request` on login.** The browser's `Origin` is not in
`hisabkitab.cors.allowed-origins`. This check runs before authentication, so even the
`permitAll` login endpoint returns 403. The backend logs the active patterns at startup:

```
CORS allowed origin patterns: [http://localhost:[*], http://127.0.0.1:[*], ...]
```

Compare that against the `Origin` request header in your browser's network tab. Note that
`http://localhost:5173` and `http://127.0.0.1:5173` are *different* origins to a browser.
To allow another one:

```powershell
$env:CORS_ORIGINS = "http://localhost:[*],http://192.168.1.50:[*]"
```

`[*]` wildcards the port. Restart the backend after changing it.

---

## API

| Method | Path | Notes |
| --- | --- | --- |
| `POST` | `/api/auth/login` | Returns a JWT |
| `GET` | `/api/auth/me` | Current user + organization |
| `GET` | `/api/dashboard` | Headline totals for the home screen |
| `GET` `PUT` | `/api/organization` | Farm name, weekly offs |
| `GET` `POST` `PUT` | `/api/employees` | `POST`/`PUT` are owner-only |
| `POST` | `/api/employees/{id}/wage` | Dated wage change |
| `POST` | `/api/employees/{id}/status` | Mark a worker as left |
| `GET` `POST` `PUT` `DELETE` | `/api/employers` | `DELETE` deactivates, never removes |
| `GET` `POST` | `/api/transactions` | |
| `POST` | `/api/transactions/{id}/void` | |
| `GET` | `/api/transactions/statement/{employeeId}` | Passbook with running balance |
| `GET` | `/api/attendance/roster?date=` | Everyone on the books that day |
| `POST` | `/api/attendance/mark` | `status: null` clears the mark |
| `POST` | `/api/attendance/mark-bulk` | Same status for several workers |
| `GET` | `/api/attendance/employee/{id}?month=` | Month totals |
| `GET` | `/api/wage-runs/preview?month=` | Computes, writes nothing |
| `GET` `POST` | `/api/wage-runs` | `POST` is owner-only |
| `POST` | `/api/wage-runs/{id}/void` | Owner-only |

Roles: **OWNER** can set up workers, employers and post wages; **MANAGER** handles the
day-to-day recording. Every request is scoped to the organization on the JWT.

---

## Layout

```
backend/src/main/java/com/hisabkitab/
  domain/       entities + enums
  repository/   Spring Data JPA, balance rollups
  service/      WageCalculator ← the wage engine lives here
  security/     JWT filter, principal
  web/          controllers + dto/
  config/       security, properties, first-run bootstrap
  exception/
backend/src/main/resources/db/migration/   Flyway migrations (schema)
backend/db/setup.sql                       one-time role + database creation

frontend/src/
  lib/          api client, types, TanStack Query hooks, formatting
  auth/         AuthContext
  components/   AppLayout (tab bar + FAB), ui.tsx
  pages/        Home, Employee, AddEntry, Attendance, Wages, People, Settings
```

## Screens

- **Home** — total outstanding, this month's advances/wages/repayments, and the worker list
  with a balance on each row
- **Worker** — statement with a running balance, attendance month, profile and wage history
- **Add entry** — pick worker → pick what happened → amount
- **Attendance** — a day at a time, tap `P` `A` `½` `OT` per worker
- **Wages** — month preview, post once, history with void
- **People / Settings** — workers, employers, weekly offs

---

## Known gaps

- The backend has **not been compiled or run** — no JDK, Maven or PostgreSQL on this
  machine. The frontend typechecks and builds clean, and its login screen was rendered in a
  browser.
- `WageCalculatorTest` covers the wage engine (weekly offs, part months, dated rate
  changes), but it has not been executed for the same reason. Nothing else has tests.
- No PDF or WhatsApp export of a statement.
- Single organization per deployment in practice, though the schema supports more.
