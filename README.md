# app-console

**Console, on [`mokuroku`](https://github.com/kotoba-lang/mokuroku).**

Design: [ADR-2608035000](https://github.com/com-junkawasaki/root/blob/main/90-docs/adr/2608035000-app-standard-application-suite-on-a-shared-catalog-kernel.edn).

Capability: `log/read`. Nothing in this repo performs the effect — the host
supplies the provider function, and that is where the grant is spent.

## Three decisions

**Severity is ranked, not spelled.** Alphabetically `:error` < `:info` <
`:warn`, which reads as a severity order and is not one. Every level carries a
numeric `:severity`, and "warn and worse" filters on that.

**An unknown level is kept, not coerced.** A provider may emit a level this app
has never heard of. It ranks -1 and is excluded from severity filters rather
than being promoted to `:info` — pretending an unknown severity is
informational is how a fault gets filtered out of view.

**Colliding timestamps stay two entries.** A burst of log lines can share a
millisecond, so the id is `[timestamp seq]`. An id that collides means two
entries become one row, silently losing whichever arrived second.

Read-only by construction: there is no command here that writes, signals, or
deletes, and the empty state never says "no errors" — the reader is always
looking at a window of an unbounded log.

## Test

```sh
clojure -M:local:test    # sibling checkouts
clojure -M:test          # pinned git deps
clojure -M:lint
```

design-quality: 100.00 on every window state including awaiting-grant (2026-08-03).
