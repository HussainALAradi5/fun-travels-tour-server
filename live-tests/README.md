# Live tests

316 checks that call a running server over HTTP and WebSocket, the way the web and mobile apps do. They complement the unit tests in `src/test`: these prove the whole stack (security, database, migrations, seed) behaves as documented. CI runs them on every pull request (`live` job in `.github/workflows/ci.yml`).

| Suite | Checks | What it proves |
| --- | --- | --- |
| `end_to_end_check.py` | 48 | Sign-in for every seeded role, registration, staff creation limits, tours, booking, payment, cancellation |
| `mobile_contract_check.py` | 20 | Every endpoint the mobile app calls returns the fields its interfaces read |
| `sign_in_and_profile_check.py` | 37 | Sign-in by email, username or phone; profiles; phone numbers with country codes |
| `mobile_screens_check.py` | 24 | The mobile screens' calls in app order, including photo upload |
| `access_check.py` | 32 | Who can see and change agencies, branches, staff and tours; cross-agency reads refused |
| `report_check.py` | 52 | Every PDF and Excel report for the allowed roles; downloads limited to the owner |
| `websocket_check.mjs` | 4 | Live notifications need a valid token and only reach the user's own topic |
| `business_check.py` | 53 | Pricing (kids chairs, premium seats, meals, sales), holds, capacity, refunds under agency policies, refund quotes |
| `gender_check.py` | 10 | Gender is required on sign-up and staff creation, stored, changeable, and listed |
| `rules_check.py` | 36 | Meal rules, seats in maintenance, hold expiry, vehicle capacity and overlap, staff roles |

## Run locally

1. Start Postgres and create an empty database.
2. Start the server on it with the seed and a relaxed sign-in limit (the suites sign in hundreds of times):

   ```bash
   DB_URL=jdbc:postgresql://localhost:5432/fun_travels_tour DB_USERNAME=postgres DB_PASSWORD=postgres \
   JWT_SECRET=local-secret-0123456789abcdef0123456789abcdef SEED_PASSWORD=Seed-Pass-2026 AUTH_RATE_LIMIT=1000 \
   java -jar target/server-0.0.1-SNAPSHOT.jar
   ```

3. Wait for `Demo catalog created` in the log, then:

   ```bash
   npm ci --prefix live-tests
   PGHOST=localhost PGUSER=postgres PGPASSWORD=postgres python -I live-tests/run.py http://localhost:8080 Seed-Pass-2026 fun_travels_tour
   ```

The suites create data with fixed names, so use a fresh database for each run. Python 3.10+ (standard library only), Node 20+, and `psql` on the path are needed.
