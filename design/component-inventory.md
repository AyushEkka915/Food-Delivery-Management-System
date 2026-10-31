# Component Inventory

Reusable UI specification for the React app (Phase 13). Every value resolves to
`design/tokens/tokens.css`. If a component is not listed, it does not belong in `src/components/`.

## 1. Purpose and rules

The inventory builds 23 screens from one finite vocabulary instead of 23 private implementations.

1. **States live in the component.** Anything that can be loading, empty, errored or populated owns
   all four. Pages compose; they never re-implement.
2. **No fetching in components.** Only page containers in `pages/*` call hooks and services. The one
   exception is `useOrderTracking`, owned by the tracking page.
3. **Props are JSDoc-typed**, e.g. `@param {{ status: OrderStatus, size?: 'sm'|'md' }} props`.
4. **Tokens only.** Colour, radius, shadow and duration come from the generated Tailwind theme.
5. **Accessible by default.** Visible focus, labelled controls, `aria-live` for async feedback.
6. **Composable, not configured.** Four booleans means it is really two components.

Screens: Land, Auth, RL (restaurant list), RD, Cart, CO (checkout), Conf, Track, Orders, Prof,
R-Dash, Menu, Queue, OD, Sales, P-Dash, Route, P-Hist, Admin, Users, Rests, A-Orders, Analytics.

## 2. Primitives

| Component | Variants | States | Keyboard / a11y | Screens |
|---|---|---|---|---|
| Button | primary, secondary, ghost, danger, icon; sm/md/lg | hover, active, focus, loading, disabled | native `button`; `aria-busy` when loading; icon-only needs `aria-label` | All |
| Input | text, email, password, tel, number; sm/md | focus, filled, error, disabled, readOnly | inside `FormField`; `aria-invalid` + `aria-describedby` | Auth, CO, Prof |
| Textarea | 3-6 rows, auto-grow | as Input | same wiring; visible counter, not a tooltip | Prof, Menu, OD |
| Select | native, sm/md | open, focus, error, disabled | real `<select>`; no div listbox; Escape closes | Menu, Rests, A-Orders |
| Checkbox | single, indeterminate | checked, indeterminate, focus, disabled | real input; group is `fieldset` + `legend` | CO, Users, A-Orders |
| RadioGroup | horizontal, vertical | selected, focus, disabled | `role="radiogroup"`; arrows move and select | CO |
| Switch | sm/md | off, on, pending, disabled | `role="switch"`; Space/Enter toggles; visible label | Menu |
| FormField | label-top, label-left; hint, error, required | default, error, disabled | generates `id`/`htmlFor`; error has `role="alert"` and an icon | All forms |
| SearchInput | inline, full-width | typing, debouncing, results, no-results | 300 ms debounce; `type="search"`; Escape clears | RL, Menu, Users |
| QuantityStepper | sm/md | min, default, max | "Decrease/Increase quantity" labels; value `aria-live="polite"` | RD, Cart |
| Badge | neutral, brand, success, warning, danger, info | static | text carries meaning; dots `aria-hidden` | All |
| StatusBadge | 10 statuses; sm/md; with-dot, with-time | static | `span` with `title`; never interactive | Orders, Queue, OD, Track |
| Avatar | initials, image; xs-lg | loaded, fallback, broken-image | `alt` from name; initials `aria-hidden` | Navbar, Users, Queue |
| Card | flat, raised, interactive | hover, focus, selected | interactive card is one link, not a div with onClick | RL, RD, Orders, Admin |
| Modal | sm/md/lg; dismissible, blocking | closed, open, closing | focus trap, `aria-modal`, labelled title, Escape, focus restored | Users, Menu, A-Orders |
| Drawer | right, bottom; sm/md/lg | closed, open, dirty-form guard | same trap rules as Modal; scroll lock | Cart, Filters, OD |
| Tabs | underline, pill | selected, focus, disabled | `role="tablist"`; arrows move; `aria-selected` | Prof, OD, Analytics |
| Accordion | single-open, multi-open | collapsed, expanded, focus | header button with `aria-expanded` + `aria-controls` | Prof, Land |
| Tooltip | top, right, bottom; 400 ms | hidden, visible | shows on focus and hover; `aria-describedby`; never critical info | Menu, Analytics |
| Toast | success, danger, warning, info; 4 s | entering, visible, leaving | `role="status"` (`alert` for danger); pause on hover; dismissible | All |
| Skeleton | text, block, circle, card, row | animating, static | `aria-hidden`; wrapper has `aria-busy` plus hidden "Loading" | All lists |
| EmptyState | default, filtered, first-use | static | heading, one sentence, one action; not image-only | RL, Orders, Queue, Menu |
| ErrorState | inline, page, retryable | static | `role="alert"`; plain-language cause; retry is a real button | All lists |
| Pagination | numbered, prev/next | first, middle, last, loading | `nav aria-label="Pagination"`; `aria-current="page"`; edges `disabled` | RL, Orders, A-Orders, Users |
| DataTable | compact, comfortable; sortable headers, row actions | loading, empty, error, sorted, selected | real `<table>`, `<th scope="col">`, `aria-sort`; actions keyboard-reachable | Queue, A-Orders, Users, Rests, Sales |
| ConfirmDialog | danger, neutral | open, pending, error | built on Modal; focus starts on Cancel; button names the object | Menu, Users, A-Orders |
| StarRating | input, readonly; sm/md | empty, partial, set, hover-preview | input is a 1-5 radiogroup; readonly reads "4 out of 5" | RD, Orders |
| PriceBreakdown | checkout, compact | with-discount, free-delivery | `<dl>`; amounts right-aligned; totals in `font-mono` | Cart, CO, Conf, OD |
| Stepper | horizontal, vertical; 3-6 steps | done, current, upcoming, failed | `ol` with `aria-current="step"`; labels visible | Conf, Track, OD |
| Timeline | default, compact | default, live-appending | event `ol`; ordering announced via `aria-live` | Track, OD, P-Hist |
| FileUpload | image, drag-drop | dragging, uploading, preview, invalid, error | hidden input, focusable via label; limits in hint text | Menu |
| CopyButton | inline, icon | idle, copied (1.5 s), error | `aria-label` names the copied value; announces "Copied" | Conf, Track |
| KpiCard | plain, delta-up, delta-down | loading, value, no-data | value is text; delta is arrow plus word ("up 12%") | R-Dash, P-Dash, Admin |
| ChartCard | line, bar, donut | loading, empty, populated, error | text summary or table fallback; colour never the only cue | Sales, Analytics |
| DateRangePicker | presets (today, 7d, 30d), custom | open, invalid range, applied | native date inputs; no custom calendar maths | Sales, Analytics |
| FilterBar | inline, stacked, active-chips | default, filtered, cleared | chips are removable buttons; "Clear all" last | RL, Queue, A-Orders |
| Breadcrumb | default, truncated | static | `nav aria-label="Breadcrumb"`; current page has `aria-current` | RD, OD, Admin |

## 3. Domain components

| Component | Responsibility | Variants / states | Notes | Screens |
|---|---|---|---|---|
| RestaurantCard | image, name, cuisines, rating, ETA, fee, open/closed | open, closed, loading | whole tile is one link; closed blocks ordering | RL, Land |
| MenuItemCard | dish, price, veg marker, description, add control | available, unavailable, added, loading | quantity via `QuantityStepper`; unavailable reads "Currently unavailable" | RD |
| CartLine | name, unit price, qty, line total, remove | default, updating, at-max, unavailable | confirm before removing a multi-quantity line | Cart, CO |
| CouponBox | code entry, applied chip, savings line | idle, applying, applied, invalid, expired | server decides eligibility; show the API message verbatim | Cart, CO |
| AddressCard | label, address line, phone, default marker | selectable, selected, loading, empty | radiogroup item; edit/delete in overflow menu | CO, Prof |
| OrderCard | customer order summary for lists | active, past, cancelled, loading | status via `StatusBadge`; action follows status | Orders |
| OrderQueueTable | restaurant queue built on `DataTable` | loading, empty, error, new-order | new rows prepend with `animate-slide-up`; reject confirms | Queue, R-Dash |
| RouteMap | SVG of graph nodes, edges, shortest path | loading, no-route, path, in-progress | frozen snapshot only: no tiles, no SDK; nodes use `<title>` | Route, Track, OD |
| DeliveryAssignmentCard | order, pickup, drop, distance, fee, countdown | offered, accepted, expired, rejected | countdown stops at zero; buttons, not a swipe | P-Dash |
| StatusStepper | `Stepper` bound to the order state machine | current, terminal | index and colour from the `StatusBadge` map; cancellation is a terminal branch | Track, Conf, OD |
| LiveIndicator | "Live" dot plus connection label | connecting, live, reconnecting, offline | `animate-pulse-soft`; honours `prefers-reduced-motion` | Track, Queue, P-Dash |

## 4. Rules

- **Status colour has one home.** `StatusBadge` (and `StatusStepper`, which imports the same map)
  resolves status to a token. No page writes `text-red-600` for an order state.
- **Money** goes through `formatCurrency(paise)` in `lib/format.js`: INR, two decimals, grouped,
  `font-mono` in tables. No `toFixed`, no manual rupee sign.
- **Dates** go through `formatDate`, `formatDateTime`, `formatRelative` from the same module, backed by
  one `Intl.DateTimeFormat` locale. No ad-hoc `toLocaleString`.
- **Destructive actions always use `ConfirmDialog`**, naming the object in the button. No
  `window.confirm`, no delete without confirmation or undo.
- **Every table and list has four states**: skeleton, error with retry, empty, populated. Empty
  distinguishes "nothing yet" from "nothing matched your filters"; skeletons mirror row heights so
  loading never collapses the layout.

### Status token map

| Status | Token | Hex |
|---|---|---|
| `placed` | `status.placed` | `#6B7280` |
| `confirmed` | `status.confirmed` | `#2563EB` |
| `preparing` | `status.preparing` | `#D97706` |
| `readyForPickup` | `status.readyForPickup` | `#7C3AED` |
| `assigned` | `status.assigned` | `#4F46E5` |
| `pickedUp` | `status.pickedUp` | `#0891B2` |
| `outForDelivery` | `status.outForDelivery` | `#0D9488` |
| `delivered` | `status.delivered` | `#16A34A` |
| `cancelled` | `status.cancelled` | `#DC2626` |
| `rejected` | `status.rejected` | `#B91C1C` |

## 5. What NOT to build

- **No custom date library or calendar engine.** Native date inputs plus `Intl` cover every report
  range; a hand-rolled calendar is unbounded work with no marks attached.
- **No third-party UI kit** (MUI, Ant, Bootstrap, shadcn). The inventory is the deliverable.
- **No map SDK or tile service.** `RouteMap` renders the graph the backend already returns.
- **No charting wrapper beyond `ChartCard`**; if Recharts is used, it is imported only there.
- **No inline hex, `rgb()` or arbitrary colour values** such as `bg-[#E23744]`.
- **No animation library.** `fade-in`, `slide-up`, `pulse-soft` are the whole motion set.
