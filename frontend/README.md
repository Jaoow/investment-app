# Investize Web

Mobile-first React SPA for the Investize portfolio API. This application is maintained in its own repository, separately from the Java API.

## Requirements and local run

- Node.js LTS (Node 24 or a compatible current LTS).
- The Investize API running locally on port 8086 (or configure another port below).

```powershell
Copy-Item .env.example .env.local
npm ci
npm run dev
```

Vite serves the app at `http://localhost:3000` and proxies `/api` to `http://localhost:8086` by default. Set `API_PROXY_TARGET` if the API uses another address. `VITE_API_BASE_URL` is public client configuration, not a secret.

For the Windows/WSL API setup, initialize and run the API from its own repository
(`investment-app`). Run this repository's `.\scripts\Start-Frontend.ps1` in a
separate terminal; it locates the standard Windows Node installation if it is
missing from PATH. The proxy reads `API_PROXY_TARGET` from `.env.local`. Restart
Vite after changing it.

The API requires its own database, market-data token and JWT key; see the API repository README. Login tokens stay in JavaScript memory only. There is no refresh-token endpoint yet, so a browser reload or expired session requires signing in again. Never place the API token or a refresh secret in a `VITE_` variable.

## Available commands

```powershell
npm run dev
npm run api:types
npm run typecheck
npm run lint
npm run test
npm run build
npm run e2e
```

`npm run build` generates `src/shared/api/schema.d.ts` from the checked-in `openapi.yaml`, type-checks and builds the static site. `openapi.yaml` documents the frontend-used surface and must be reconciled with the API's `/v3/api-docs` after backend contract changes. Decimal schemas accept both strings and numbers while the Java API still emits JSON numbers; the client parses number tokens losslessly and exposes decimal values as strings before presentation. No financial calculations or suggestion scoring are done in the browser.

## Product behavior and limitations

- Assets and balance show configured tickers together with their effective target
  in the whole portfolio and current ceiling. A 60% category with a 25% within-category
  target produces a 15% portfolio target, not a recommendation weight.
- Register missing tickers with their category in Assets, or add them while editing
  balance targets. This is a manual catalog entry, not a verification of listing or
  available quotes. Existing categories cannot be overwritten by users.
- Category targets total 100%; within each active category, ticker targets total
  100%. Inputs accept Brazilian decimal commas and up to two decimal places.
- The asset detail shows paginated ceiling history, including previous/new prices,
  removals and timestamps. Imported baseline dates are clearly distinguished.

- The contribution screen calls `POST /v1/portfolio/{portfolioId}/suggestions`, displays the API's units, values, reasons, exclusions and residual, and can only confirm a local review. It does not create a movement or submit an order.
- The API currently does not provide refresh-session, quote streaming, editable user recommendation weights, portfolio-history series or direct B3/custodian synchronization.
- Quote polling is limited to the visible asset detail page, pauses in the background and is labelled as periodic provider data, not guaranteed real-time pricing.
- Public share links are bearer secrets. Their read-only page shows only fields returned by the API and follows the owner's include-values option.
- Recommendations are informative and are not financial advice.
