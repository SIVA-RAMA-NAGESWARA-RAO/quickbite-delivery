# QuickBite Delivery — Frontend

React (Vite) single-page app for both sides of the platform: customers
browsing restaurants and placing orders, and restaurant owners managing
their menu and incoming orders.

See the **root `README.md`** (one level up) for full setup instructions,
including how to start the backend this app talks to. The short version:

```bash
npm install
npm run dev
```

The app expects the API Gateway to be running at the URL configured in
`.env` (`VITE_API_BASE_URL`, defaults to `http://localhost:8080/api`).

## Structure

```
src/
├── api/client.js        Axios instance — attaches the JWT to every request
├── context/              Auth session and shopping cart (React Context)
├── components/           Shared UI: nav bar, restaurant card, status badge, route guard
├── pages/                Customer-facing pages (browse, menu, cart, orders)
├── pages/owner/          Restaurant-owner pages (dashboard, incoming orders board)
└── utils/format.js       Currency + order-status formatting helpers
```

## Available scripts

- `npm run dev` — start the dev server with hot reload
- `npm run build` — production build to `dist/`
- `npm run preview` — serve the production build locally
- `npm run lint` — run oxlint over `src/`
