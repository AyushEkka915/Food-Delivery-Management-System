# Design artefacts - Smart Food Delivery Management System

Phase 2 (UI/UX planning) outputs. Everything here is documentation and
low-fidelity wireframes, not production code.

## Contents

```
design/
  tokens/
    tokens.css              CSS custom properties (runtime source of truth)
    tokens.json             DTCG-style token contract (tooling / docs)
    tailwind.theme.js       Tailwind 3.4 theme.extend object
  flows/
    user-flows.md           10 Mermaid flows with happy + failure paths
    screen-inventory.md     31-screen contract (routes, states, real-time, phases)
  wireframes/
    wireframe.css           Low-fidelity wireframe kit (grayscale)
    customer.html           12 screens (public + customer)
    restaurant.html         5 screens (restaurant manager)
    delivery.html           4 screens (delivery partner)
    admin.html              9 screens (platform admin)
  component-inventory.md    37 primitives + 11 domain components
```

## How to view

Open any `design/wireframes/*.html` file in a browser. They are self-contained
and use the relative stylesheet `wireframe.css`. No build step, no network.

## How to use the tokens

1. Keep `tokens.css` and `tokens.json` in sync - `tokens.css` wins at runtime.
2. Import `tokens/tailwind.theme.js` into `tailwind.config.js`:
   `theme: { extend: require('./design/tokens/tailwind.theme.js').default }`
   (set `"type": "module"` in package.json, or rename to `.mjs`).
3. Never hard-code a colour in a component. Always use a token or the
   `StatusBadge` component for order statuses.

## Reading order for reviewers

1. `flows/screen-inventory.md` - the contract (31 routes).
2. `flows/user-flows.md` - the journeys and their failure branches.
3. `wireframes/customer.html` then `restaurant.html`, `delivery.html`, `admin.html`.
4. `component-inventory.md` - what gets built in Phase 13.
5. `tokens/tokens.css` - the visual language.

## Status

Phase 2 complete. Phase 3 (database schema) is next.
