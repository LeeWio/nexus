# Analytics Time Contract

- Page-view event timestamps are `Instant` values. MySQL stores UTC in the existing `blog_page_view.viewed_at` DATETIME(6) column; JSON returns ISO-8601 timestamps with `Z`.
- Hibernate binds and extracts `Instant` with its UTC JDBC type, independently of the connection timezone. Keep the existing datasource settings: changing the connection timezone would change the persistence semantics of unrelated legacy `LocalDateTime` columns. Setting the JVM default timezone is not part of this fix.
- Reporting dates, daily trends, and the 00:05 aggregation schedule use `Asia/Shanghai`. Date windows are half-open `[start, end)`. A seven-day window includes today and six preceding calendar dates. Growth compares contiguous windows of equal elapsed duration.
- Native trend queries apply the Shanghai offset before extracting a date. The retained modern data is entirely within Shanghai's fixed UTC+8 period. The predicate still uses the unmodified indexed UTC column.
- The recent overview reads raw page views, so older daily aggregates calculated with a different day boundary cannot contaminate current reports. The daily aggregation job continues to maintain archived summaries using Shanghai boundaries.
- Existing shared `createdAt`/`verifiedAt` columns remain UTC `LocalDateTime` persistence fields. Analytics converts its UTC instant boundaries explicitly when querying those repositories; this change does not redefine unrelated application timestamps.
- The visitor UI accepts only timestamps with an explicit offset, displays them in `Asia/Shanghai`, and labels the timezone. It never guesses a timezone or adds eight hours manually.

## Release Notes

1. Establish the historical storage convention from the JVM timezone, JDBC timezone, raw database values and old API output. The old `LocalDateTime` mapping may shift values at the JDBC boundary even when the JVM is UTC. Do not infer storage timezone from the container clock alone.
2. Before replacing the API process, stop acceptance of new page-view events and flush the old page-view Redis buffer using the old application. Buffered payloads change from unzoned `LocalDateTime` to `Instant`; do not feed old payloads into the new reader or clear them to work around a deserialization error. Verify that the old buffer is empty, then start the updated API.
3. Back up historical `viewed_at` values. Only if the affected rows are proven to be Shanghai wall-clock values, normalize that bounded set to UTC once. Either do this before starting the new API, or keep analytics reads and collection gated until the new API is healthy and normalization is verified. Use a recorded ID cutoff, transaction and verification against the backup; retain a restore script. UTC deployments need no correction. Do not change other timestamp columns or use an unconditional Flyway migration across different environments.
4. Release the API before the stricter visitor UI. Both visitor summary and timeline responses must contain `Z` (or an explicit offset).

The schema and existing UTC raw values need no rewrite. For the November 2026 production rollout, the JVM was GMT but JDBC used `serverTimezone=Asia/Shanghai`; raw page-view values were eight hours ahead of their UTC instants. Those existing page-view timestamps require the bounded normalization above. These are deployment requirements, not operations performed automatically by the code change.

## Verification

The focused suite covers collection, raw UTC persistence and existing rows, native SQL grouping, typed visitor projections, real HTTP serialization, calendar windows and scheduled aggregation. Run it with `-Duser.timezone=UTC`, `Asia/Shanghai`, and `America/Los_Angeles`. Repository tests also run against an isolated MySQL instance with both UTC and production `Asia/Shanghai` JDBC settings, without forcing the session to UTC. Frontend tests check explicit offsets, Shanghai midnight and invalid/unzoned input.
