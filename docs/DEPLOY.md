# Deploying QuickBite Delivery live

This covers taking the platform from "running on my laptop" to "running at
a real URL I can send someone." **I (Claude) cannot do this step for
you** - it needs your own hosting accounts, and my tools have no network
access to Vercel, Railway, Render, or any cloud provider. Everything below
is written so you can follow it yourself in roughly 15–20 minutes.

## The plan: Vercel (frontend) + Railway (backend)

**Frontend → Vercel.** Free, and genuinely a good fit — a static Vite
build has no server to keep alive, so there's no cost or sleep/cold-start
concern at all.

**Backend → Railway.** This is my pick, and here's the honest reasoning,
checked against current (2026) pricing rather than assumed:

| Platform | Why / why not for *this* project |
|---|---|
| **Railway** (recommended) | Docker-based, gives every service a private network address it can use for Eureka discovery, one-click Postgres. **Requires a credit card** for its 30-day/$5-credit trial (this changed in 2023 — it's no longer card-free). After the trial, ongoing "Free" plan caps out at 3 services/0.5GB RAM each, which is **not enough for six Java services** — you'd need the $5/month Hobby plan for anything beyond a short trial-credit window. |
| **Render** | Free tier needs no card, but free web services **sleep after 15 minutes of inactivity** and take 30–60s to cold-start. For this architecture that's a real problem, not just slowness: a sleeping Eureka server drops every other service's registration, and they don't reliably recover in sync when things wake back up. Fine for a single simple API; a poor fit for a Eureka-based multi-service demo. Paid tier removes sleep but is $7/service — six services would run ~$42+/month. |
| **A cheap VPS** (DigitalOcean, Hetzner, etc.) running `docker-compose.yml` | Most predictable: one flat price (~$4-6/month), no sleep, no per-service juggling, one `docker compose up -d` deploys everything. More setup work up front (you manage the server), and needs a public IP/domain if you want a fixed URL. Good choice if you already have a VPS or want to keep this running long-term. |

**Bottom line:** for a short evaluation window (a demo, a few days of
grading), Railway's trial credit is almost certainly enough and costs
nothing beyond the card-verification hold — just delete the project
afterward. For anything longer-running, budget $5/month (Railway Hobby) or
use the VPS route.

---

## Part 1 — Deploy the backend to Railway

1. Push this whole `quickbite-delivery` folder to a GitHub repository (a
   private repo is fine — Railway can deploy from private repos once you
   authorize it).
2. At [railway.app](https://railway.app), create a new Project, choose
   **"Deploy from GitHub repo"**, and select your repo.
3. Railway will try to auto-detect a single service. Instead, add **six
   separate services** in the same project, each pointing at the same repo
   but with a different **Root Directory**:
   `eureka-server`, `auth-service`, `restaurant-service`, `payment-service`,
   `order-service`, `api-gateway`. Each folder already has a `Dockerfile`,
   so Railway will build each one as a Docker image automatically — you
   don't need to pick a builder manually.
4. Add a **PostgreSQL** database from Railway's "+ New" → "Database" menu
   (one instance is enough; it'll host all four databases, same as local).
   Once it's created, open its "Connect" tab and run `db/init.sql`'s
   contents through Railway's built-in query console (or connect with
   `psql` using the connection string Railway gives you) to create the
   `quickbite` role and four databases, exactly like the local setup.
5. For **every one of the six services**, set these environment variables
   (Service → Variables tab). Generate one real `JWT_SECRET` and reuse the
   exact same value everywhere:
   ```
   JWT_SECRET=<a long random string, e.g. from `openssl rand -base64 48`>
   ```
6. For the **four services with a database** (`auth-service`,
   `restaurant-service`, `payment-service`, `order-service`), also set:
   ```
   DB_HOST=<your Postgres service's private hostname, e.g. postgres.railway.internal>
   DB_PORT=5432
   DB_USER=quickbite
   DB_PASSWORD=quickbite
   DB_NAME=authdb        (or restaurantdb / paymentdb / orderdb — one per service)
   ```
7. For **every service except Postgres itself**, set Eureka's address to
   the Eureka service's Railway-private hostname (Railway gives every
   service a `<service-name>.railway.internal` address automatically —
   check the Eureka service's "Networking" tab for its exact private
   domain):
   ```
   EUREKA_URI=http://eureka-server.railway.internal:8761/eureka/
   ```
8. On **`api-gateway`** only, also set:
   ```
   CORS_ORIGINS=https://<your-vercel-app>.vercel.app
   ```
   (you'll fill in the real Vercel URL after Part 2 — come back and update
   this, then redeploy the gateway).
9. On **`restaurant-service`** only, if you want uploaded photos to
   survive a redeploy, attach a Railway **Volume** mounted at `/data/uploads`
   and set `UPLOAD_DIR=/data/uploads`. Without a volume, uploads work fine
   but are wiped on the next deploy (fine for a demo, not for real use).
10. Only **`api-gateway`** needs a public domain — under its "Networking"
    tab, click "Generate Domain". Leave the other five services private
    (no public domain) so nothing bypasses the gateway's JWT/rate-limit
    checks.
11. Deploy `eureka-server` first and wait for it to go healthy before the
    others start dialing it (Railway deploys all six roughly in parallel by
    default — if a service errors on its first boot because Eureka wasn't
    up yet, just redeploy that one service once Eureka shows healthy).
12. Once everything is green, copy the API Gateway's public URL — that's
    your live backend, e.g. `https://api-gateway-production-xxxx.up.railway.app`.

## Part 2 — Deploy the frontend to Vercel

1. At [vercel.com](https://vercel.com), "Add New… → Project", import the
   same GitHub repo, and set **Root Directory** to `frontend`. Vercel
   auto-detects the Vite framework and the `vercel.json` in that folder
   (already included) handles client-side routing so React Router pages
   don't 404 on refresh.
2. Before the first deploy (or right after, then redeploy), go to
   **Settings → Environment Variables** and add:
   ```
   VITE_API_BASE_URL = https://<your-railway-gateway-domain>/api
   ```
   Use the exact Gateway URL from Part 1, step 12, with `/api` on the end.
3. Deploy. Vercel gives you a URL like `https://quickbite-delivery.vercel.app`.
4. Go back to Railway's `api-gateway` service and update `CORS_ORIGINS` to
   this exact Vercel URL (step 8 above), then redeploy the gateway so it
   actually accepts requests from your live frontend.

You now have two real links: the Vercel URL (what you share with people)
and the Railway gateway URL (what the frontend talks to behind the
scenes — you generally don't share this one directly).

---

## Alternative: one VPS, docker-compose, no PaaS juggling

If you have (or are willing to rent) a small Linux server:

```bash
# On the server, with Docker + Docker Compose installed:
git clone <your-repo-url>
cd quickbite-delivery
cp .env.example .env
nano .env          # fill in JWT_SECRET, and CORS_ORIGINS with your Vercel URL
docker compose up -d --build
```

That single command builds and starts all six services plus Postgres,
wired together on Docker's internal network exactly like the Railway
setup above, but on one machine with one bill. Point your domain (or just
share `http://<server-ip>:8080`) as the API base URL for the Vercel
frontend, same as step 2 in Part 2.

---

## After you're done demoing

Delete the Railway project (or stop the VPS) to make sure nothing keeps
running/billing after you no longer need it. Vercel's free tier has no
ongoing cost either way, so the frontend is safe to leave up indefinitely.
