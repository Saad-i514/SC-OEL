# Inventory OEL

Java 21 Swing inventory application backed by Supabase PostgreSQL.

## Feature commits
1. Project setup and requirements.
2. Validated inventory model and tests.
3. Supabase schema and JDBC persistence.
4. Swing CRUD interface.
5. Verification and generated Javadoc.

## Scope
Add, list, update and delete items; manually choose In Stock, Low Stock or Out of Stock; reject blank names and invalid quantities. Database operations run off the Swing event thread. No login or public distribution: this is a trusted operator desktop application.

## Run
Install JDK 21 and run `./build.ps1 test`, then `./build.ps1 run` in PowerShell. Copy `db.properties.example` to `db.properties` and supply database credentials locally. Never commit that file. Import the directory into IntelliJ or Eclipse with Java 21; add the downloaded `lib/postgresql-42.7.14.jar` as a library and run `inventory.InventoryApp` from the project directory.

`./build.ps1 docs` generates submission-ready HTML in `docs/javadoc`.

## Supabase
Migration files are under `supabase/migrations`. Use `./configure-db.ps1` to apply migrations through Supabase CLI using the local connection settings. The table is not exposed to anonymous API clients. The publishable key is intentionally unnecessary for JDBC; the database connection uses TLS.

## Submission
The supplied brief also requires two group members, an IDE demo, screenshots, a detailed PDF report, a ZIP and a GitHub upload. These must reflect actual execution and the students' names. See `docs/verification.md` for verified results and outstanding evidence.
