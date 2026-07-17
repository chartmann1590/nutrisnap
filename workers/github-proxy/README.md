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
3. Set secrets:
   ```sh
   npx wrangler secret put GH_API_TOKEN        # the GitHub PAT (repo scope), also stored in GitHub Actions secrets
   npx wrangler secret put APP_SHARED_SECRET   # any random string; put the same value in the app config below
   ```
4. Deploy:
   ```sh
   npm run deploy
   ```
5. Wire the app to the deployed Worker (either in `local.properties` for local dev, or as
   `GH_PROXY_URL` / `GH_PROXY_SECRET` repo secrets for CI):
   ```properties
   github.proxy.url=https://nutrisnap-github-proxy.<your-subdomain>.workers.dev
   github.proxy.secret=<same value as APP_SHARED_SECRET>
   ```

## Notes

- Rotating `APP_SHARED_SECRET` only requires redeploying the Worker secret and shipping a new
  app build — it never grants GitHub write access on its own.
- Consider adding a Cloudflare Rate Limiting rule on this Worker's route to bound abuse from a
  leaked or reverse-engineered shared secret.
