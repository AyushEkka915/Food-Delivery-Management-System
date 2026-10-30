# Smart Food Delivery — Master Phase Plan (18 Phases)

Total estimated effort: **≈ 220 hours**. At 15 h/week that is ~15 weeks; at 18 h/week ~12 weeks.
Phases are ordered by dependency, not by the original numbering, where that saves rework
(noted per phase).

Legend: **[CORE]** must ship · **[SHOULD]** adds marks · **[STRETCH]** droppable.

---

## Master table

| # | Phase | Wk | Est. h | Deliverable you can show | Exit gate |
|---|---|---|---|---|---|
| 0 | Environment setup | 0 | 2 | JDK 21, Node 20, MySQL 8, Postman, Git repo | `java -version`, `mvn -v`, DB connects |
| 1 | Requirements & architecture | 1 | 6 | `docs/00-architecture-and-plan.md` | approved |
| 2 | UI/UX & wireframes | 1 | 10 | 18 wireframes + design tokens + component inventory | approved |
| 3 | Database & ER | 2 | 10 | `V1__schema.sql` (22 tables) + `V2__seed.sql` + ER diagram | `flyway:migrate` clean, `validate` passes |
| 4 | Spring Boot init | 2 | 4 | Runnable app + `/api/v1/health` | 200 in Postman |
| 5 | Domain layer (entity/repo/DTO/mapper) | 3 | 16 | Restaurant + menu CRUD working | Postman CRUD + 400 validation shape |
| 6 | Auth & RBAC | 3–4 | 14 | register/login/refresh, 4 roles | role matrix passes, 401/403 correct |
| 7 | Restaurant & menu module | 4 | 14 | approval workflow, public catalog, filters | customer can browse & filter |
| 8 | **DSA core + tests** | 5 | 20 | 13 structures/algorithms, ~45 tests | all green, ArchUnit boundary passes |
| 9 | Cart, pricing, coupons | 6 | 12 | server-priced cart, coupon engine | price/coupon test suite green |
| 10 | Order lifecycle + FIFO queue | 7 | 16 | place→accept→prepare, state machine | illegal transition = 409, concurrency test |
| 11 | Delivery assignment (heap) | 8 | 12 | dispatch, accept/decline, pickup→delivered | order reaches DELIVERED |
| 12 | Graph + Dijkstra + fees | 8–9 | 10 | route API, distance-based delivery fee | route preview + fee match |
| 13 | React frontend (5 sub-phases) | 3–4, 9–12 | 28 | all 4 role UIs | click-through E2E |
| 14 | WebSocket live tracking | 10 | 10 | live status push + reconnect | two browsers sync |
| 15 | Payments | 10–11 | 10 | COD + sandbox online, refunds | duplicate payment blocked |
| 16 | Admin + analytics + audit stack | 11 | 14 | dashboards, undo, reports, charts | undo works, charts use real data |
| 17 | Testing, debugging, optimization | 12 | 12 | coverage report, Postman collection | all tests green, N+1 fixed |
| 18 | Deployment + documentation | 13 | 12 | deployed URL, README, report, viva Q&A | demo runs from a clean machine |

---

## Phase 0 — Environment setup (2 h)

Install JDK 21, Maven 3.9, Node 20 LTS, MySQL 8, Postman, Git. Create GitHub repo with
`.gitignore` (Java + Node + `.env`). Create MySQL user + `food_delivery` schema.
**Gate:** a `SELECT VERSION()` from Workbench and `mvn -v` in the terminal.

## Phase 1 — Requirements & system architecture (6 h) ✅ delivered

Feature tiering (CORE/SHOULD/STRETCH), 5-layer architecture, DSA→feature map with complexity
table, ER model, folder structures, state machine, 13-week roadmap.
**Gate:** you approve the architecture and the 7 decisions.

## Phase 2 — UI/UX planning & wireframes (10 h)

Objective: lock the design before writing a single component (cheapest possible rework).
Concept: design tokens, atomic component inventory, 4 mandatory list states.
Files: `design/tokens.json`, `design/wireframes/` (Excalidraw/Penpot), `design/component-inventory.md`.

18 screens: Landing, Login, Register, Restaurant list, Restaurant detail, Cart, Checkout,
Order confirmation, Order tracking, Customer orders, Profile · Restaurant dashboard, Menu
management, Incoming queue, Order detail, Sales report · Partner dashboard, Route view, History ·
Admin dashboard, Users, Restaurants, Orders, Analytics.
**Gate:** you can trace "customer cancels an order" through the wireframes without a dead end.

## Phase 3 — Database & ER diagram (10 h)

Objective: hand-written, normalized DDL — this is your DBMS component.
Files: `db/migration/V1__schema.sql`, `V2__seed.sql`, `docs/er-diagram.png`.

22 tables: users, roles, user_roles, refresh_tokens, addresses, locations, road_edges,
restaurants, food_categories, food_items, cart_items, orders, order_items, payments,
delivery_partners, delivery_assignments, order_status_history, reviews, coupons,
coupon_redemptions, complaints, audit_logs.
Seed: 4 roles, 8 categories, 36 graph nodes, 58 edges, 1 admin, demo customer/restaurant/partner,
6 restaurants, ~40 dishes.

**Gate:** `flyway:migrate` on an empty schema succeeds; `ddl-auto=validate` passes with entities.

## Phase 4 — Spring Boot initialization (4 h)

Objective: a running skeleton with correct configuration and error envelope.
Files: `pom.xml`, `FoodDeliveryApplication.java`, `application{,-dev,-prod}.yml`,
`ApiResponse`, `ApiError`, `GlobalExceptionHandler`, `WebConfig` (CORS), actuator.
**Gate:** `GET /api/v1/health` → `{"status":"UP"}`; unknown path → clean 404 JSON.

## Phase 5 — Domain layer (16 h)

Objective: entities, repositories, DTOs, mappers, and the first real module.
Files: `entity/*` (22), `repository/*` (20), `dto/**`, `mapper/*`, `service/restaurant/*`,
`controller/RestaurantAdminController` + `MenuController`.
Key details: `BaseEntity` auditing, `@Version`, `EnumType.STRING`, `BigDecimal` money,
snapshot fields, no entity in any response.

**Gate:** Postman creates restaurant → category → food item; invalid payload returns
`400` with `fieldErrors[]`.

## Phase 6 — Authentication & RBAC (14 h)

Objective: real security, not a toy login.
Files: `security/{JwtService,JwtAuthenticationFilter,CustomUserDetailsService,SecurityUser,
RateLimitFilter,OwnershipGuard}`, `service/auth/*`, `AuthController`, `SecurityConfig`.
Details: BCrypt(10), 15-min access token, rotating hashed refresh token, revocation on
logout/suspend, path-based rules + `@PreAuthorize`, custom token-bucket login throttle.
**Gate:** 4×N role matrix in Postman: correct 200/401/403; customer A reading customer B's
order → 403.

## Phase 7 — Restaurant & menu module (14 h)

Objective: the supply side of the marketplace.
Files: `RestaurantService`, `MenuService`, `RestaurantQueryService`, `AdminController`
(approval part), `RestaurantPublicController`.
Details: PENDING→APPROVED/SUSPENDED lifecycle, ownership checks, availability + stock,
filter (cuisine, rating, veg, open-now, price band) + sort + pagination.
**Gate:** a new restaurant is invisible until approved; filter combinations return correct pages.

## Phase 8 — DSA core + unit tests (20 h) ← the marking centrepiece

Objective: every structure hand-written, test-first, zero Spring imports.
Files: `dsa/queue/LinkedQueue`, `dsa/stack/ArrayStack`, `dsa/heap/{MinHeap,DispatchCandidate}`,
`dsa/graph/{RoadGraph,Edge,RouteResult,Dijkstra}`, `dsa/sorting/{MergeSort,QuickSort}`,
`dsa/searching/Searcher`, `dsa/list/DoublyLinkedList`, `dsa/map/SimpleHashMap`,
`dsa/cache/LruCache`, `dsa/trie/Trie`, `dsa/ratelimit/TokenBucket`,
`test/**/dsa/*Test` (~45 tests) + `BenchmarkTest`.
**Gate:** all green; ArchUnit test proves `dsa` has no Spring/JPA dependency;
benchmark table generated for the report.

## Phase 9 — Cart, pricing, coupons (12 h)

Objective: money correctness — the #1 source of real bugs.
Files: `CartService`, `CartValidator`, `PricingService`, `CouponService`,
`DeliveryFeeCalculator`, `util/Money`, `cart_items` table usage.
Details: single-restaurant cart (conflict → 409), live availability check, coupon rules
(min order, window, global limit, per-user limit, cap, first-order), rounding HALF_UP.
**Gate:** 25-case pricing test suite (tax, discount cap, free-delivery threshold, rounding).

## Phase 10 — Order lifecycle + FIFO queue (16 h)

Objective: the transactional heart.
Files: `OrderService`, `OrderStateMachine`, `OrderQueueRegistry`, `OrderTimelineService`,
`OrderCancellationService`, `OrderController`, `OrderStatusHistory` writes.
Details: explicit transition map + actor rules, snapshots at creation, conditional stock
decrement, idempotency key, queue rebuild on startup + 30 s reconciler, restock on cancel,
illegal transition → 409, `@Version` optimistic locking.
**Gate:** two simultaneous accepts → one wins; 5 checkouts for 3 units → exactly 3 succeed;
restart mid-order → queue intact.

## Phase 11 — Delivery assignment (12 h)

Objective: the priority queue in production.
Files: `DispatchEngine`, `DeliveryService`, `PartnerService`, `DeliveryController`,
`delivery_assignments`.
Details: eligibility filter, candidate construction with Dijkstra ETA, heap drain, atomic
claim, 90 s decline window, re-dispatch, earnings accrual.
**Gate:** assignment picks the genuinely best partner (assert with a fixture of 5 partners);
decline triggers the next candidate.

## Phase 12 — Graph + Dijkstra + distance fee (10 h)

Objective: the algorithm with financial consequences.
Files: `GraphLoader`, `RoadGraph`, `Dijkstra`, `RouteService`, `RouteController`,
`GeoUtil`. Seeded 36-node network, undirected weighted edges (metres + speed).
Details: adjacency list, distance array, own min-heap, visited set, relaxation, path
reconstruction, route snapshot JSON frozen at assignment, fee = 20 + 8 × km.
**Gate:** route preview returns path + distance + ETA; identical route for reversed pair;
fee matches distance for 5 sample pairs.

## Phase 13 — React frontend (28 h, in 5 sub-phases)

Objective: a UI that looks like a product, integrated continuously (not big-bang).
* 13A (wk 3–4): Vite + Tailwind + router + `axiosClient` + AuthContext + login/register +
  layouts + design system `ui/` components.
* 13B (wk 9): customer flow — discovery, detail, cart, checkout, confirmation, tracking, history, profile, review.
* 13C (wk 10): restaurant — dashboard, menu CRUD, incoming queue, order detail, reports.
* 13D (wk 11): partner — dashboard, assignments, route SVG view, active delivery, history.
* 13E (wk 12): admin — dashboard, users, restaurants, partners, orders, coupons, categories, analytics, audit.
**Gate:** full click-through demo with no dead buttons and all four list states implemented.

## Phase 14 — WebSocket live tracking (10 h)

Objective: real-time without polluting business logic.
Files: `WebSocketConfig`, `StompAuthChannelInterceptor`, `OrderEventPublisher`,
frontend `wsClient.js`, `useOrderTracking.js`, `OrderTimeline` component.
Topics: `/topic/orders/{id}`, `/topic/restaurants/{id}/orders`, `/user/queue/deliveries`,
`/topic/admin/live`. JWT authenticated in CONNECT; subscription ownership check.
**Gate:** status changed in the restaurant tab appears in the customer tab < 1 s; reconnect
after server restart restores the feed.

## Phase 15 — Payments (10 h)

Files: `PaymentService`, `PaymentProvider` (interface), `SimulatedPaymentProvider`,
`PaymentController`, `payments` table, idempotency handling.
Details: COD → PAID on delivery; online → CREATED→CAPTURED/FAILED→REFUNDED; webhook
signature verification if Razorpay is enabled; duplicate prevention via unique indexes.
**Gate:** replaying the same idempotency key returns the same payment; refund after cancel.

## Phase 16 — Admin, analytics, audit stack (14 h)

Files: `AdminService`, `AdminActionService` (custom stack + undo), `ComplaintService`,
`ReportService` (MergeSort), `AnalyticsService`, `AuditLog` writer, admin controllers,
5 admin pages + charts.
Details: KPI aggregation, top restaurants/items (HashMap frequency), 7-day revenue series,
reversible actions pushed on the stack, `Undo` applies the inverse + writes an audit row.
**Gate:** suspend a restaurant → undo → state restored and both entries audited; charts show
real seeded data.

## Phase 17 — Testing, debugging, optimization (12 h)

Files: `*IT` integration tests (Testcontainers MySQL or H2/MySQL-mode fallback),
`postman/FoodDelivery.postman_collection.json`, `docs/testing.md`, JaCoCo report.
Details: service coverage ≥ 70%, N+1 detection (`hibernate.show_sql` review), index review
with `EXPLAIN`, pagination everywhere, concurrency scripts, empty/error UI states, a11y pass.
**Gate:** `mvn verify` green from a clean clone; Postman collection runs end-to-end.

## Phase 18 — Deployment & documentation (12 h)

Files: `Dockerfile`, `nginx.conf`, `.env.example`, `README.md`, `docs/report/` (all 25 report
sections), `docs/viva-30-questions.md`, `docs/demo-script.md`, `docs/screenshots/`.
Deploy: backend on Render/Railway (or local MySQL + jar), frontend on Vercel/Netlify with
`/api` proxy, or fully local for the viva.
**Gate:** fresh machine → README → running demo in < 15 min; report PDF complete.

---

## Dependency graph

```
P0 → P1 → P2 → P3 → P4 → P5 → P6 → P7 ─┐
                     └→ P8 ────────────┴→ P9 → P10 → P11 → P12
                                                   │      │
                                            P13A ──┴──────┴→ P13B..E
                                                                 │
                                          P14 ← P10/P11 ────────┘
                                          P15 ← P9/P10
                                          P16 ← P10..P15
                                          P17 → P18
```

## Rules that keep this on schedule

1. Every phase ends with a **demo you can show**, not "the layer is 80% done".
2. Screenshots are captured **during** the phase, not reconstructed at the end.
3. No phase starts before the previous gate passes — except P8 (DSA), which is independent
   of P6/P7 and can run in parallel when you are blocked.
4. If a week slips, cut from the STRETCH list, never from P8/P10/P12.
5. Report chapters are written as each phase closes (≈30 min per phase).
