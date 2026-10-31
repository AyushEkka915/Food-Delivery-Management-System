# Screen Inventory — Smart Food Delivery Management System

Status: Phase 2 contract, authoritative page list. Any screen not in this table does not exist in
the frontend. Behaviour for each row is defined in `design/flows/user-flows.md`; status names and
actor authority come from `docs/00-architecture-and-plan.md` §7.1; phase numbers come from
`docs/01-phases.md`.

Two notes before the table:

* Phase 2 originally named 18 wireframe screens. The canonical route list expands those into **31
  route-level screens** because one route carries two screens (tracking has an active and a terminal
  variant). No new feature is introduced here.
* There is exactly one row per screen. `/orders/:orderId/track` appears twice on purpose: the active
  screen and the terminal screen have different actions, different states and different
  real-time behaviour.

---

## 1. The 31 screens

| # | Route | Page | Role | Purpose (1 line) | Primary action | Key data shown | States required | Real-time | Delivered in phase |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `/` | Landing | Public | Explain the product and route each visitor to the right entry point | Browse restaurants | Value proposition, cuisine strip, how-it-works, sign-in CTA, open restaurants | skeleton, empty, error+retry | no | 13A |
| 2 | `/login` | Login | Public | Authenticate and land on the role home | Sign in | Email or phone, password, error banner, throttle notice, pending or suspended notice | error+retry, permission-denied | no | 13A |
| 3 | `/register` | Register | Public | Create a customer account or submit a restaurant/partner application | Create account | Role tabs, identity fields, password rules, pending-approval confirmation | error+retry, confirmation | no | 13A |
| 4 | `/restaurants` | Restaurant discovery | Customer | Find a restaurant by search, filter and sort | Open a restaurant card | Cards with rating, ETA, delivery fee, open-now, veg badge; filter bar; pagination | skeleton, empty, error+retry | no | 13B |
| 5 | `/restaurants/:restaurantId` | Restaurant detail and menu | Customer | Inspect the menu and add items to the cart | Add to cart | Address, hours, rating, fee and ETA, category tabs, item price, veg mark, stock, sort | skeleton, empty, error+retry, readonly | no | 13B |
| 6 | `/cart` | Cart | Customer | Review lines and quantities before checkout | Proceed to checkout | Line items, quantity steppers, server price breakdown, coupon box, restaurant header | skeleton, empty, error+retry, confirmation | no | 13B |
| 7 | `/checkout` | Checkout | Customer | Choose address, coupon and payment, then place the order | Place order | Address picker, read-only breakdown, coupon outcome, payment method, retry panel | skeleton, error+retry, confirmation, readonly | no | 13B (payment states 15) |
| 8 | `/orders/:orderId/confirmation` | Order confirmation | Customer | Confirm the order was placed and show what happens next | Track order | Order id, restaurant, item summary, total, ETA, payment status | skeleton, error+retry, permission-denied, readonly | no | 13B |
| 9 | `/orders/:orderId/track` | Live tracking, active order | Customer | Follow status, timeline and ETA until delivery | Cancel order (only while the window is open) | Status stepper, timeline, ETA, partner summary, payment status, cancel or review CTA | skeleton, error+retry, permission-denied, confirmation | yes | 13B, 14 |
| 10 | `/orders/:orderId/track` | Tracking, terminal variant: delivered, cancelled, rejected | Customer | Explain the final outcome and the next allowed step | Review order (delivered only) | Final status, full timeline, cancellation reason, refund state, totals, review CTA | skeleton, error+retry, permission-denied, readonly | no | 13B |
| 11 | `/orders/:orderId/review` | Review form | Customer | Rate and comment on a delivered order | Submit review | Restaurant summary, 1-5 star control, optional comment, existing review when present | error+retry, permission-denied, confirmation, readonly | no | 13B |
| 12 | `/orders` | Order history | Customer | Find past and active orders and act on them | Reorder | Order rows with status badge, date, total, item count; filters; pagination | skeleton, empty, error+retry | no | 13B |
| 13 | `/profile` | Profile and addresses | Customer | Maintain identity details and delivery addresses | Save changes | Name, email, phone, address list with default badge, add, edit, delete | skeleton, empty, error+retry, confirmation | no | 13B |
| 14 | `/restaurant` | Restaurant dashboard | Restaurant | Show today's trading position and the next thing to do | Open incoming queue | Today's orders, revenue, average prep time, queue depth, open or closed state | skeleton, empty, error+retry, permission-denied | no | 13C |
| 15 | `/restaurant/menu` | Menu management | Restaurant | Create, edit, price and stock menu items | Add item | Grouped items, price, category, availability, stock quantity, edit and delete | skeleton, empty, error+retry, confirmation | no | 13C |
| 16 | `/restaurant/orders` | Incoming orders queue | Restaurant | Work the FIFO queue by accepting or rejecting orders | Accept (head row only) | FIFO list, placed-at age, SLA timer, item count, total, payment method | skeleton, empty, error+retry, confirmation | yes | 13C, 14 |
| 17 | `/restaurant/orders/:orderId` | Restaurant order detail | Restaurant | See one order in full and advance preparation | Mark preparing, then mark ready for pickup | Items and notes, address snapshot, customer contact, status timeline, one legal action | skeleton, error+retry, permission-denied, readonly | no | 13C |
| 18 | `/restaurant/reports` | Sales report | Restaurant | Review revenue and popular items for a date range | Change date range | Revenue series, order count, top items, payout after 12% commission (SHOULD) | skeleton, empty, error+retry | no | 13C |
| 19 | `/partner` | Partner dashboard | Partner | Control availability and see today's earnings | Go online or go offline | Online toggle, deliveries today, earnings (25 per delivery + 5 per km), active assignment | skeleton, empty, error+retry, permission-denied | no | 13D |
| 20 | `/partner/assignments` | Available offers | Partner | Accept or decline dispatch offers | Accept offer | Offer cards with pickup ETA, distance, payout, 90 s expiry countdown, COD amount | skeleton, empty, error+retry, confirmation | yes | 13D, 14 |
| 21 | `/partner/deliveries/:assignmentId` | Active delivery and route | Partner | Execute pickup, delivery and completion | Advance to the single legal next status | Status stepper, route SVG, turn list, distance, ETA, drop address, COD amount | skeleton, error+retry, permission-denied, confirmation | no | 13D |
| 22 | `/partner/history` | Delivery history | Partner | Review completed deliveries and earnings | Open a past delivery | Completed deliveries, payout per delivery, date filter, totals | skeleton, empty, error+retry, readonly | no | 13D |
| 23 | `/admin` | Admin dashboard | Admin | Show platform health, the live order counter and the last reversible action | Undo last action | GMV, orders per hour, active partners, SLA breaches, live counter, stack top | skeleton, empty, error+retry, confirmation | yes | 13E, 14, 16 |
| 24 | `/admin/users` | User management | Admin | Find and suspend or reactivate customer accounts | Suspend or activate | Table of name, email, phone, status, joined date, order count | skeleton, empty, error+retry, confirmation | no | 13E |
| 25 | `/admin/restaurants` | Restaurant management | Admin | Approve, suspend or reactivate restaurants | Approve or suspend | Table of name, owner, cuisine, city, status, rating, actions | skeleton, empty, error+retry, confirmation | no | 13E, 16 |
| 26 | `/admin/partners` | Partner management | Admin | Approve, suspend or reactivate delivery partners | Approve or suspend | Table of name, phone, vehicle, status, online state, active deliveries | skeleton, empty, error+retry, confirmation | no | 13E, 16 |
| 27 | `/admin/orders` | Order monitoring | Admin | Monitor every order and force-cancel with a reason | Force cancel | Table of order id, customer, restaurant, status, total, age, SLA flag, payment state | skeleton, empty, error+retry, confirmation | no | 13E |
| 28 | `/admin/coupons` | Coupon management | Admin | Create coupons and toggle them on or off | New coupon | Code, type, minimum order, cap, validity window, usage count, active flag | skeleton, empty, error+retry, confirmation | no | 13E, 16 |
| 29 | `/admin/complaints` | Complaints (SHOULD) | Admin | Work support tickets to resolution | Update ticket status | Ticket list, linked order, raised by, status OPEN, IN_REVIEW, RESOLVED or REJECTED | skeleton, empty, error+retry, readonly | no | 13E |
| 30 | `/admin/analytics` | Analytics | Admin | Show 7-day revenue and top performers | Change date range | Revenue line chart, top restaurants, top items, order volume | skeleton, empty, error+retry | no | 13E, 16 |
| 31 | `/admin/audit` | Audit log | Admin | Inspect the durable admin action history | Filter by entity or date | Actor, action, entity, before and after values, timestamp, undone marker | skeleton, empty, error+retry, readonly | no | 13E, 16 |

Row count: **31**. Routes covered: 30 distinct routes; `/orders/:orderId/track` is the only route
with two screens.

Open route decision: A6 category management is listed in Phase 13E but has **no canonical route** in
this inventory. It is therefore not a screen yet. Either fold it into an existing admin screen or add
a route before 13E starts; do not invent a path in the frontend.

---

## 2. Global states

Shared behaviour, implemented once in `axiosClient`, `ProtectedRoute`, the layout shells and
`ErrorState`, not per page.

| Situation | Behaviour |
|---|---|
| `401` access token expired | Attempt one silent refresh with the rotating refresh token. On success, replay the original request once. On failure, clear auth state, store `returnTo` (path + query, never a full URL), and redirect to `/login`. |
| `401` during a background poll or WS reconnect | Do not redirect mid-form. Stop polling, show the offline banner, and redirect only on the next user-initiated request. |
| After login with a `returnTo` | Navigate to `returnTo` if the role is allowed, otherwise to the role home: customer `/restaurants`, restaurant `/restaurant`, partner `/partner`, admin `/admin`. |
| `403` role mismatch | Render the permission-denied screen inside the current layout, with the user's actual role and a link to their own home. Never render a broken shell or an empty table. |
| `403` ownership failure (IDOR) | Same permission-denied screen, message "This order does not belong to your account". Never an empty state, never a 404 disguise. |
| `403` pending approval (restaurant/partner) | Permission-denied variant with the reason "Awaiting admin approval", plus sign out. No KPI panels, no zero-filled cards. |
| `403` suspended account | Permission-denied variant with "Account suspended", plus sign out. Refresh tokens are already revoked server-side. |
| `5xx` | Error state with the server `code`, a human message, and a Retry button that re-runs the same request. Keep filters, form input and scroll position. Show the error `timestamp` as a small reference line. |
| `409` conflict | Never a generic error page. Inline or banner message naming the conflict (stock, cart restaurant, illegal transition, duplicate review), followed by an automatic refetch of the authoritative state. |
| `404` unknown id | Error state: "We could not find that order/restaurant" with a link back to the matching list. |
| Offline or WebSocket dropped | Sticky banner "You are offline. Showing the last known status." Polling continues where the screen supports it; write actions are disabled with a reason; nothing is queued for automatic replay on reconnect. |
| Reconnect after offline | Refetch the current resource once, dismiss the banner, resume the socket subscription. Because events are not replayed, the refetch is mandatory. |
| Loading | Skeleton that matches the final layout. Show it only after 150 ms to avoid flashing; never replace a populated list with a full-page spinner during a refetch. |
| Empty | Illustration-free empty state with one sentence and one CTA. Empty is never used for an error, a permission failure or a filtered-out result with active filters (that case offers "Clear filters"). |
| Toast policy | Success: 4 s, top-right on desktop, above the bottom nav on mobile, max 3 stacked. Errors that belong to a field or a form stay inline, never toast-only. Real-time status changes announce through the timeline and `aria-live`, not toast. Destructive actions use a confirmation dialog, not an undo toast. Toasts never contain the only copy of an error message. |
| Confirmation policy | Required for cancel, reject, force-cancel, delete menu item, delete address, suspend, undo, and mark delivered. The dialog names the entity and the consequence ("Customer is refunded", "Items are restocked"). Confirm button uses the verb, not "OK". |
| Session expiry during a multi-step form | Preserve the local draft, redirect to login, restore the draft after login when the role still matches. |

---

## 3. Responsive rules

Breakpoints are the ones locked in `design/tokens/tokens.css`: 360 (base), 640 (`sm`), 768 (`md`),
1024 (`lg`), 1280 (`xl`). Mobile-first means the 360 px layout is the default and the wider layouts
add, never subtract.

### 3.1 Mobile-first screens

| Screen | Why mobile-first | Behaviour at 360-767 px |
|---|---|---|
| `/orders/:orderId/track` | Customers open tracking from a phone notification | Sticky status header with ETA, vertical timeline, full-width action bar at the bottom with safe-area inset; map or route collapsed into a 200 px preview |
| `/partner`, `/partner/assignments`, `/partner/deliveries/:assignmentId` | Riders work one-handed, outdoors | Single-column offer cards, 44 px minimum controls, sticky primary action, large COD figure, route SVG scaled by `viewBox` with turn list below instead of a side panel |
| `/restaurants`, `/restaurants/:restaurantId`, `/cart`, `/checkout`, `/orders`, `/profile` | The customer journey is phone-first | One card per row, filters in a bottom sheet, cart as a full-screen route rather than a drawer, quantity steppers 44 px |
| `/`, `/login`, `/register` | First contact is usually mobile | Single column, form width capped at 420 px, no horizontal scroll at 360 px |

### 3.2 Desktop-optimised screens

| Screen | Why desktop | Behaviour at 1024 px and above |
|---|---|---|
| `/admin/users`, `/admin/restaurants`, `/admin/partners`, `/admin/orders`, `/admin/audit`, `/admin/coupons`, `/admin/complaints` | Bulk scanning, comparison, filtering | Full data table with sticky header, column sort, row actions in an overflow menu, side filter panel |
| `/restaurant/menu`, `/restaurant/reports`, `/admin/analytics` | Editing many rows and reading charts | Two-column editor or chart plus table, side-by-side comparison |
| `/restaurant/orders` | Queue work is faster on a larger screen | Two-pane view: queue list left, selected order right |

Restaurant and partner dashboards remain **fully usable on a phone** (architecture §6); desktop is
an optimisation, never a requirement.

### 3.3 Structural rules

| Element | Below 768 px | 768-1023 px | 1024 px and above |
|---|---|---|---|
| Tables (admin, menu, orders, history) | Convert to stacked cards: title, status badge, 2 key figures, overflow menu for actions. No horizontal scrolling of data tables. | Table with 3-4 priority columns, remaining columns behind a row expand | Full table |
| Dashboard sidebar (restaurant, partner, admin) | Hamburger drawer over content, closed by default, focus trapped while open | Icon rail with tooltips, expandable | Persistent 248 px sidebar |
| Customer navigation | Fixed bottom nav with Home, Orders, Cart, Profile plus a cart badge; navbar reduces to logo, search icon, avatar | Top nav with inline links | Top nav with full labels |
| Charts | One chart per row, min height 200 px, legend below | Two per row | Two or three per row with shared legend |
| Filter bars | Bottom sheet triggered by a "Filters" button with an active-filter count | Inline row that wraps | Inline row with a sort dropdown |
| Forms (checkout, profile, menu item) | Single column, full-width inputs, sticky submit | Two columns where fields pair naturally | Single column capped at 640 px for readability, editor panes for menu |
| Route map | 200 px preview, tap to expand full screen | 320 px | Side-by-side with the turn list |
| Modals | Full-screen sheet from the bottom | Centred dialog, max 560 px | Centred dialog, max 640 px |

Additional rules: no page may require horizontal scrolling at 360 px; every interactive target is at
least 44x44 px on touch; sticky action bars must not cover the last list row (bottom padding equals
bar height plus safe area); print or PDF is not supported anywhere.

---

## 4. Accessibility checklist

Applies to every screen in §1. Each item is verifiable in review and in the Phase 17 a11y pass.

| # | Requirement | How it is checked |
|---|---|---|
| A1 | One `h1` per screen; heading levels never skip. Dashboard cards use `h2`, not styled `div`s. | DOM inspection per route |
| A2 | Skip link "Skip to main content" is the first focusable element on every layout and moves focus to `#main`. | Tab once from page load |
| A3 | Every input has a visible `<label>` bound with `for`/`id`. Placeholder is never the only label. Icon-only buttons carry `aria-label`. | Form audit |
| A4 | Text and essential icons meet 4.5:1 contrast; status badges are checked against their own background, including the amber `PREPARING` and violet `READY_FOR_PICKUP` tokens. | Contrast check on `tokens.css` values |
| A5 | Status is never conveyed by colour alone: every badge has a text label, and the stepper marks current, completed and upcoming steps with text or icon, not just tint. | Grayscale screenshot review |
| A6 | Modals and the mobile drawer trap focus, close on `Escape`, restore focus to the trigger, and mark the background `aria-hidden`. | Keyboard-only pass |
| A7 | All actions work with keyboard only: quantity steppers, star rating, tabs, table row menus, and the route turn list. No `div` with `onClick`. | Keyboard-only pass |
| A8 | Real-time updates are announced through a polite `aria-live` region ("Order is out for delivery"). The region is not the whole timeline, so screen readers do not re-read history. | Screen reader spot check |
| A9 | Form validation errors are announced: the first invalid field receives focus, the message is linked with `aria-describedby`, and the summary uses `role="alert"`. | Submit an invalid form with a screen reader on |
| A10 | Touch targets are at least 44x44 px with 8 px spacing; this includes stepper buttons, table overflow menus and toast dismiss buttons. | 360 px device pass |
| A11 | Focus order follows visual order; no positive `tabindex`; sticky headers and bottom bars do not hide the focused element. | Tab through each route |
| A12 | Charts and the route map have a text alternative: a data table or ordered turn list with distance and direction, plus a one-line summary. | Hide images and re-read the page |

---

## 5. Copy and formatting rules

All formatting lives in `src/utils/formatCurrency.js`, `formatDate.js`, `statusMeta.js`. No screen
formats a number or a date by hand.

| Item | Rule | Example |
|---|---|---|
| Currency | Indian rupee, `Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' })`, always two decimals, **Indian digit grouping** (last group of 3, then groups of 2). | `₹1,234.50`, `₹12,34,567.00`, `₹89.00` |
| Currency in tables | Same formatter, right-aligned, tabular numerals, no currency symbol repeated in a total column header. | Column "Total (₹)" with `1,234.50` |
| Free or zero charge | Explicit word, never a bare dash where money is expected. | `Delivery: Free` |
| Date | `d MMM yyyy` with a three-letter month, 12-hour clock, uppercase meridiem, comma between date and time. | `30 Oct 2026, 7:42 PM` |
| Date only (reports, filters) | No time component. | `30 Oct 2026` |
| Date range | En dash with spaces, both ends formatted fully. | `24 Oct 2026 – 30 Oct 2026` |
| Time only | 12-hour, uppercase meridiem, no leading zero on the hour. | `7:42 PM` |
| Relative ETA | Integer minutes, unit `min`, no "mins", no tilde. Under a minute uses `< 1 min`. Over an hour uses `1 h 05 min`. | `12 min`, `1 h 05 min` |
| Distance | One decimal kilometre, space before unit. | `3.4 km` |
| Order id | `FD-` + 4-digit year + `-` + 6-digit zero-padded sequence, monospace, never truncated, copy button beside it. | `FD-2026-004182` |
| Counts | `en-IN` grouping, singular and plural handled, no bare number where a noun is needed. | `1 item`, `3 items`, `1,20,000 orders` |
| Status labels in UI | Sentence case of the enum, never the raw `UPPER_SNAKE` value; API and DB keep the enum. | `OUT_FOR_DELIVERY` → `Out for delivery`, `READY_FOR_PICKUP` → `Ready for pickup` |
| Refund and payment labels | Use the domain word, not a code. | `Refunded`, `Cash on delivery`, `Paid` |
| Buttons | Verb first, sentence case, no "Submit" or "OK" for a meaningful action. | `Place order`, `Accept order`, `Mark ready for pickup` |
| Destructive buttons | Verb plus object, danger styling, never the primary colour. | `Cancel order`, `Suspend restaurant` |
| Empty states | One sentence of fact plus one CTA; no apology, no exclamation mark. | `No orders yet.` + `Browse restaurants` |
| Error copy | What happened, what to do next, and a retry action; no stack traces, no raw enum, no blame. | `We could not load your orders. Check your connection and try again.` |
| Numbers in prose | Numerals for all quantities, including small ones. | `2 restaurants` not `two restaurants` |
| Punctuation and tone | Sentence case everywhere, no emoji, no exclamation marks, Oxford comma not used, "you" for the customer and "your restaurant" for the manager. | `Your order is on the way.` |
| Truncation | Restaurant and dish names truncate at 2 lines with a title attribute; order ids and money are never truncated. | `Masala Dosa with coconut chut…` |
