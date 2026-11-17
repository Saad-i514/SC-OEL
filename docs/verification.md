# Verification record - 17 November 2026

| Check | Result |
| --- | --- |
| Java 21 compilation with all compiler warnings | PASS |
| Valid item, whitespace normalization, zero quantity | PASS |
| Blank name, negative, nonnumeric, fractional and overflowing quantities | PASS |
| Invalid status and name over 200 characters | PASS |
| Javadoc generation | PASS |
| Supabase CLI migration | PASS: migration applied through Supabase CLI using session pooler |
| Live add/view/update/delete and missing-row checks | PASS: live CRUD and missing-row handling; test record removed |
| GUI visual walkthrough and empty-state demonstration | PASS: empty/populated workspace, navigation, search, selection and live save checked |
| Javadoc browser review | Not yet verified |
| GitHub push | PASS: published to Saad-i514/SC-OEL on main |

Run `./build.ps1 test` for local validation. Run `./configure-db.ps1`, then `./build.ps1 integration` using the configured Supabase session-pooler address. Integration tests create one uniquely named item and clean up only that ID, never clear the inventory.

## Manual demo checklist
1. Run `./build.ps1 run` from the project root.
2. Add a valid item; refresh and confirm it persists.
3. Try a blank name, negative quantity and nonnumeric quantity; confirm validation messages.
4. Select a row, change its name, quantity and each stock status; save and refresh.
5. Cancel deletion, then confirm deletion; verify only the selected row disappears.
6. With an empty test inventory, verify the empty-inventory message.
7. Disconnect the network; verify an error is displayed and the window remains responsive.
8. Open docs/javadoc/index.html in a browser and review the API documentation.

## Remaining submission work
Provide the actual two group-member names, demonstrate from an IDE, capture genuine successful GUI screenshots, prepare the detailed report PDF and credential-free source ZIP. No results have been fabricated for these steps.

## Security and operating scope
Credentials live only in ignored db.properties. The migration enables RLS and revokes anonymous/authenticated API access. JDBC is intended for a trusted store operator on a private machine. Do not distribute database administrator credentials with the application; a distributed product needs user authentication and restricted database permissions. SQL values use prepared statements; failures do not silently fall back to temporary storage.

## Connection verification follow-up

The direct IPv6 endpoint was unreachable. The supplied Tokyo session-pooler endpoint resolved that issue. Java required Supabase's CA certificate; the committed public certificate now enables `sslmode=verify-full` (certificate chain and hostname verification). `./build.ps1 integration` completed successfully with exit code 0. Credentials remain in ignored db.properties.


## Multi-page UI verification

- Compiled with `-Xlint:all`; validation and view-logic checks passed.
- View checks cover empty summaries, sums exceeding integer range, literal/case-insensitive search, combined filters, sorted row identities, page selection with reduced motion, empty/populated chart painting, and decoding both bundled illustrations.
- Visually inspected the empty and populated interfaces at the available desktop size; the window fits the usable screen and the item form scrolls while its action buttons remain visible.
- Navigated Overview, Inventory, and Activity; searched a temporary record, selected a row, saved through the UI, and confirmed the save in Activity.
- Live JDBC CRUD regression passed. Temporary UI demonstration records were removed by their exact generated IDs.
- No student IDE demonstration or Javadoc browser review is claimed. Broader OS/display-scaling and assistive-technology testing remain outstanding.

Actual screenshots of the final empty Overview/Inventory and initial session Activity are included in `docs/screenshots/`.


## Magnetic controls and palette follow-up

Compilation, validation and view tests passed after the colour/control update. New checks cover bounded magnetic offsets, unchanged component hit bounds, action invocation, disabled-button suppression, reduced-motion reset, and styled input/dropdown painting with preserved values. The running Inventory page was inspected for rounded fields, dropdowns, card depth and contrast. Database code was unchanged; live CRUD was not repeated for the styling-only update.


## Demo inventory

Executed `scripts/SeedDemo.java` with assertions enabled against Supabase. First run inserted and read back all 12 sample products (192 units). A second run inserted zero and skipped all 12 existing names, confirming repeat-run behaviour. Every sample name is marked `(Demo)`; existing inventory was not updated or deleted. Status distribution: 7 In Stock, 3 Low Stock, 2 Out of Stock.
