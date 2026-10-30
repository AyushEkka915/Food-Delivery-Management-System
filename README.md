# Smart Food Delivery Management System

A full-stack food delivery platform with four roles — customer, restaurant, delivery partner and admin — built as a college major project.

## What this is

This is a food delivery management platform covering the full order journey: browsing restaurants and menus, cart and pricing, order placement, restaurant acceptance and preparation, delivery-partner assignment and live tracking, and admin oversight.

It is built as a college major project to demonstrate practical Java backend development, data structures and algorithms (queues, heaps, graphs, sorting/searching), DBMS design, and a REST API consumed by a React frontend.

The repository is currently at **Phase 0 (environment and repository setup)**. No application code exists yet — the sections below describe the intended stack and layout, not a working system.

## Tech stack

| Layer | Technologies |
|---|---|
| Frontend | React 18, Vite, Tailwind CSS 3.4, Axios, React Router, STOMP over WebSocket |
| Backend | Java 21, Spring Boot 3.5, Spring Web, Spring Data JPA / Hibernate, Spring Security, WebSocket |
| Database | MySQL 8, Flyway |
| Testing | JUnit 5, Mockito |
| Tools | Maven, Postman, Git |

## Repository layout

```
.
├── backend/          Spring Boot application (Maven)
├── frontend/         React + Vite application
├── docs/             Architecture, phase plan and project documentation
├── design/           Design tokens, wireframes and component inventory
├── scripts/          Helper scripts (setup, run, database utilities)
├── postman/          Postman collections and environments
├── .tools/           Project-local JDK/Maven/Node toolchain (gitignored)
├── .editorconfig
├── .env.example      Placeholder-only environment template (safe to commit)
├── .gitattributes
├── .gitignore
└── README.md
```

`backend/`, `frontend/`, `design/` and `postman/` are empty placeholders in Phase 0, each holding a `.gitkeep` so the folder is tracked. `scripts/` holds the Phase 0 environment helpers (`env.ps1`, `doctor.ps1`, `create-database.sql`). The pre-existing `TicketBookingSystem/` folder is an unrelated earlier exercise, not part of this project's layout.

## Prerequisites

| Requirement | Version |
|---|---|
| JDK | 21 |
| Maven | 3.9 |
| Node.js | 20 or newer |
| npm | Bundled with Node.js 20+ |
| MySQL | 8 |

## Getting started

There is nothing to build yet. This project is scaffolded only: no `pom.xml`, no `package.json`, no application source and no database migrations exist at this stage.

Build and run instructions will be added in **Phase 4 (Spring Boot initialization)** once the backend skeleton actually exists. Commands are deliberately not documented here yet, because inventing them before the code exists would produce instructions that fail.

What you *can* run today is the environment check:

```powershell
. .\scripts\env.ps1     # activates the project-local JDK 21 + Maven 3.9 toolchain
.\scripts\doctor.ps1    # read-only PASS/FAIL report for every dependency
```

The toolchain lives in `.tools/` (gitignored), so nothing is installed system-wide. `scripts/create-database.sql` creates the MySQL database and a least-privilege application user; see `docs/02-phase-0-environment.md` for the full procedure.

## Project status

- [x] **Phase 0** — Environment setup
- [ ] Phase 1 — Requirements & architecture
- [ ] Phase 2 — UI/UX & wireframes
- [ ] Phase 3 — Database & ER
- [ ] Phase 4 — Spring Boot init
- [ ] Phase 5 — Domain layer
- [ ] Phase 6 — Auth & RBAC
- [ ] Phase 7 — Restaurant & menu
- [ ] Phase 8 — DSA core + tests
- [ ] Phase 9 — Cart, pricing, coupons
- [ ] Phase 10 — Order lifecycle + FIFO queue
- [ ] Phase 11 — Delivery assignment (heap)
- [ ] Phase 12 — Graph + Dijkstra + fees
- [ ] Phase 13 — React frontend
- [ ] Phase 14 — WebSocket live tracking
- [ ] Phase 15 — Payments
- [ ] Phase 16 — Admin + analytics + audit
- [ ] Phase 17 — Testing & optimization
- [ ] Phase 18 — Deployment & documentation

## Author

Ayush Ekka
