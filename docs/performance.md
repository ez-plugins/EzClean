---
title: Performance guide
nav_order: 6
description: Tune EzClean for large servers with thousands of entities.
---

# Performance guide

EzClean is designed to operate with minimal TPS impact even on heavily-loaded servers.
All performance-intensive features are **disabled by default** — enable them when your
server's entity counts justify it.

## How cleanup works

1. **Collect phase** — EzClean iterates loaded entities in each enabled world, evaluates each
   against the removal rules, and builds a `List<Entity>` of candidates. No entities are
   removed yet.
2. **Remove phase** — entities are removed one by one (synchronous) or in batches across
   multiple ticks (async removal mode).
3. **Stats phase** — duration, entity counts, and TPS samples are recorded. The stats file is
   written asynchronously so I/O never blocks the main thread.

## Async entity removal

By default, all entities are removed in a single tick. On servers with tens of thousands of
entities scheduled for removal, this can cause a noticeable TPS spike.

**Async removal** spreads the removal work across multiple ticks (500 entities per tick),
smoothing the frame time impact.

{: .warning }
"Async removal" is misleading naming — entity removal in Bukkit **must** run on the main
thread. What this feature does is schedule 500-entity batches across consecutive ticks rather
than removing everything in one tick. Do not confuse this with true multi-threading.

### Enabling async removal

Via command (live, no restart needed):

```
/ezclean toggle async-removal
/ezclean toggle async-removal <cleaner_id>   # if you have multiple profiles
```

Via YAML (edit `cleaners/<id>.yml`, then `/ezclean reload`):

```yaml
performance:
  async-removal: true
```

### When to enable it

| Server size | Entities per cleanup | Recommendation |
|---|---|---|
| Small (< 50 players) | < 1 000 | Not needed |
| Medium (50–200 players) | 1 000–5 000 | Consider enabling |
| Large (200+ players) | 5 000+ | Strongly recommended |

Use `/ezclean stats` to check how many entities were removed in recent runs.

## Pile detection

Pile detection scans for clusters of the same entity type at the same block position.
This is relatively cheap — O(n) over the entity list — but it does run a full scan
during the **warning phase** to build the pre-clean summary.

After the first cleanup run, the summary is **cached** from the previous run's
collect phase. The expensive scan is only performed once per server start until the
first cleanup fires.

## Stats file I/O

Stats are written to `EzClean/stats/` after each run. The write happens on an async
Bukkit task (debounced — at most one write queued at a time) so the main thread is
never blocked by file I/O.

## Reducing entity counts at the source

EzClean cleans up what is already there — it does not prevent entity spawning.
For sustained performance improvements consider:

- Raising `max-entity-cramming` in `bukkit.yml`
- Using Paper's per-world entity limits
- Profiling with [Spark](https://spark.lucko.me/) to identify the root source of entity buildup
