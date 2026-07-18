# nutrisnap-github-proxy

Cloudflare Worker that proxies the app's in-app feedback reporter to GitHub. The real
GitHub PAT lives only here (as a Worker secret), never inside the Android app. The app
authenticates to this Worker with a separate, low-privilege, rotatable shared secret that
only unlocks this Worker's limited surface (create issue, comment, upload a feedback
screenshot to a fixed directory) — not direct GitHub write access.

## Setup

1. Edit `wrangler.jsonc` and set `vars.GH_REPO_OWNER` / `vars.GH_REPO_NAME` to the target repo.
2. Install deps and log in:
   ```sh
   cd workers/github-proxy
   npm install
   npx wrangler login
   ```
3. Create the KV namespace used to store AI-response reports and note its id:
   ```sh
   npx wrangler kv namespace create AI_REPORTS
   ```
   Put the returned id into `wrangler.jsonc`'s `kv_namespaces[0].id`.
4. Set secrets:
   ```sh
   npx wrangler secret put GH_API_TOKEN        # the GitHub PAT (repo scope), also stored in GitHub Actions secrets
   npx wrangler secret put APP_SHARED_SECRET   # any random string; put the same value in the app config below
   npx wrangler secret put ADMIN_SECRET        # a SEPARATE random string — never shipped in the app,
                                                # only used by the ai-report-trends.yml GitHub Action
   ```
5. Deploy:
   ```sh
   npm run deploy
   ```
6. Wire the app to the deployed Worker (either in `local.properties` for local dev, or as
   `GH_PROXY_URL` / `GH_PROXY_SECRET` repo secrets for CI):
   ```properties
   github.proxy.url=https://nutrisnap-github-proxy.<your-subdomain>.workers.dev
   github.proxy.secret=<same value as APP_SHARED_SECRET>
   ```
7. Put the same `ADMIN_SECRET` value into the repo's `WORKER_ADMIN_SECRET` GitHub Actions
   secret — `.github/workflows/ai-report-trends.yml` uses it to poll `/reports/summary` on a
   schedule and opens/updates a GitHub issue labeled `ai-report-trend` when report volume
   spikes.

## Endpoints

- `POST /issues`, `GET /issues/:n`, `GET|POST /issues/:n/comments`, `PUT /assets/:filename` —
  feedback reporter, gated by `X-App-Secret` (the app's shared secret).
- `POST /reports` — app submits an AI food-estimate report (wrong food / wrong nutrition /
  inappropriate / other), gated by the same `X-App-Secret`. Stored in the `AI_REPORTS` KV
  namespace with a 90-day TTL.
- `GET /reports/summary?sinceMs=<epoch ms>` — aggregate report counts (total + by category) for
  the trend-monitor workflow, gated by a **separate** `X-Admin-Secret` header that is never
  shipped in the app — a leaked app secret can't be used to read report data.

## Notes

- Rotating `APP_SHARED_SECRET` only requires redeploying the Worker secret and shipping a new
  app build — it never grants GitHub write access on its own.
- Consider adding a Cloudflare Rate Limiting rule on this Worker's route to bound abuse from a
  leaked or reverse-engineered shared secret.
