# Investize

Investize is a Spring Boot REST API for tracking investment portfolios and simulating whole-unit contributions. The React web frontend is maintained separately.

## Requirements and local setup

- Java 23 or compatible JDK capable of compiling with `--release 23`.
- PostgreSQL for local application runs. H2 and Flyway are used by the automated tests.
- Maven (or the checked-in Maven wrapper).

### Local setup on Windows with Docker in WSL

From the repository root, create the ignored API environment file once:

```powershell
.\scripts\Setup-Local.ps1
.\scripts\Start-Database.ps1
```

The setup generates a random 64-byte Base64 JWT key without printing it and preserves
the existing environment file when rerun. Add your own `BRAPI_API_TOKEN` to the root
`.env.local` to test quotes and suggestions, then restart the API. An empty token
allows the application to start but does not guarantee provider access.
Never add API secrets to the frontend environment or commit the local file.

This isolated Compose project uses PostgreSQL 17 on `localhost:5433`, its own volume,
and API port `8086` so it does not interfere with other projects on `5432` or `8080`.
It does not reuse or baseline existing databases. Database credentials in the
example and Compose file are for this local-only service.

Keep the database terminal open and wait for the script to report that PostgreSQL
is ready. After Compose confirms database health, the script keeps a WSL process
attached; on this machine, detached containers alone do not prevent WSL from shutting down.
Start the API in another terminal:

```powershell
.\scripts\Start-Api.ps1
```

The API automatically imports `.env.local` as Java properties when started from
the repository root, including via the IDE with that working directory. Environment
variables override file values. Configure the separate frontend's development
proxy to forward `/api` to `http://localhost:8086`.

- Allowed frontend origins: `http://localhost:3000` and `http://127.0.0.1:3000`.
- API OpenAPI: `http://localhost:8086/v3/api-docs`.
- Register a new account in the frontend, then create a portfolio.
- Add movements, ceiling prices and allocations before simulating a contribution.
- Confirming a contribution is only a local review, never a movement or order.

Use Ctrl+C in each terminal to stop the apps. Stop only this database (preserving
its data) with:

```powershell
wsl.exe --cd "$PWD" --exec docker compose -f compose.local.yml stop postgres
```

### Alternative: existing PostgreSQL and explicit environment variables

Start PostgreSQL for local development:

```powershell
docker compose up -d postgres
```

Set the required environment variables before starting the app:

```powershell
$env:JDBC_DATABASE_URL = "jdbc:postgresql://localhost:5432/investmentdb"
$env:JDBC_DATABASE_USERNAME = "investmentuser"
$env:JDBC_DATABASE_PASSWORD = "investmentpass"
$env:BRAPI_API_TOKEN = "<brapi API token>"
$env:JWT_SECRET_KEY = "<base64-encoded random key of at least 32 bytes>"
.\mvnw.cmd spring-boot:run
```

The compose credentials are for local development only. Do not reuse them in a shared or production environment, and do not commit tokens or JWT keys.

Flyway owns the PostgreSQL schema (`src/main/resources/db/migration`). V1 creates the initial schema for an empty database; V2 adds per-portfolio price ceilings; V3 adds portfolio shares. Hibernate validates the schema at startup. **Do not point this initial migration history at an existing database with tables/data without first reviewing and baselining that database.**

## Tests

```powershell
.\mvnw.cmd test
```

Tests use H2 with the same migrations and do not call the external market-data API.

## Web frontend

The mobile-first React SPA is maintained in a separate repository and is not
included in this API's commits. It consumes this API; contribution confirmation
only reviews a simulation and does not create a movement or submit an order.
For local development, point its API proxy at `http://localhost:8086` and use one
of the configured CORS origins. Never expose the market-data token or JWT signing
key in frontend environment variables.

## API capabilities

- JWT authentication and portfolio CRUD under `/auth` and `/v1/portfolio`.
- Portfolio holdings/movements and XLSX import under `/v1/portfolio/{portfolioId}/movement`.
- Category/asset targets under `/v1/portfolio/{portfolioId}/allocations`.
- Current quote endpoint `GET /v1/quotes?ticker=...`.
- Per-portfolio ceiling CRUD at `/v1/portfolio/{portfolioId}/assets/{tickerSymbol}/ceiling`.
- Authenticated manual ticker registration at `POST /v1/ticker/register` with
  `{"symbol":"ITUB4","category":"EQUITIES"}`. Categories also include
  `REAL_ESTATE_FUNDS` and `BDRS`. This is a shared catalog; users can add a symbol
  but cannot overwrite its existing category or global metadata. Manual registration
  does not verify exchange/provider availability.
- Consolidated configured tickers, effective portfolio target percentages and
  ceiling prices at `GET /v1/portfolio/{portfolioId}/asset-settings`, independent
  of market-data availability.
- Paginated ceiling audit trail at
  `GET /v1/portfolio/{portfolioId}/assets/{tickerSymbol}/ceiling/history?page=0&size=10`.
  Changes and removals are recorded atomically, newest first; saving the same
  numeric value does not create another event. Removing and recreating a ceiling
  preserves its history. V4 seeds existing ceilings as `BASELINE` events dated
  at migration time, not their unknown original creation dates. History is private
  to the portfolio owner and removed if the portfolio is deleted.
- Contribution simulation via `POST /v1/portfolio/{portfolioId}/suggestions` with `{"amount": 1000.00, "currency": "BRL"}`.
- Read-only, revocable portfolio links under `/v1/portfolio/{portfolioId}/shares`, with a public snapshot at `/public/portfolio-shares/{token}`.

To create a share, send `expiresAt` as a future ISO-8601 instant and set `includeValues` only when the owner consents to disclosing quote/value fields, for example `{"expiresAt":"2030-01-01T00:00:00Z","includeValues":false}`.

The suggestion endpoint returns whole units, estimated value, residual amount, score, reasons, quote source/time and excluded-asset reasons. It is a simulation only: it does not create movements, place orders, or include costs/taxes. It blocks the calculation if required market data is missing, lacks timestamps, or is older than the configured freshness limit; quotes above the portfolio-specific ceiling are never eligible.

Allocation percentages are decimal values with two fractional digits. Category targets must total 100%; asset targets within each non-zero category must total 100%. The effective asset target is the product of category and within-category percentages. Recommendation weights are configured separately from those targets in `application.properties`:

- `investize.recommendations.weights.ceiling-distance`
- `investize.recommendations.weights.daily-drop`
- `investize.recommendations.weights.allocation-gap`

Weights must be non-negative; available signals are normalized by their configured weight sum. Quote-cache TTL/size and the maximum quote age are also configurable in `application.properties`.

## Market data and limitations

The market adapter uses brapi.dev's documented v2 quote endpoint and Bearer authentication; cache entries include the complete query key. The app requires a market timestamp for recommendations and does not present a quote without one as fresh. The API provider and its data are not a direct B3 integration. Confirm the provider's current terms, licensed display/storage rights, plan limits and commercial-use conditions before a public/commercial launch. No undocumented API quotas or prices are assumed here.

No direct B3/custodian holdings synchronization is implemented. XLSX movement import remains manual and now rejects a malformed row instead of silently importing a partial portfolio. Recommendations are informational and do not constitute financial advice.

## Security and sharing

Portfolio-scoped read/write cases verify the authenticated user owns the portfolio. Shared links are read-only, store only a SHA-256 hash of a random token, require an owner-selected expiration, and omit values unless the owner opts in. A shared snapshot exposes holdings (ticker and quantity); when values are opted in, it also includes fresh quote and value fields. It never includes owner identity, movement history, or portfolio ID.
