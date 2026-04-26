---
title: Spark
nav_order: 6
parent: Integrations
description: Automatic CPU profiling to identify the root source of entity buildup.
---

# Spark

[Spark](https://spark.lucko.me/) is a performance profiling plugin for Minecraft servers.
EzClean integrates with Spark in two ways:

1. **Higher-quality TPS/MSPT data** — the [TPS-aware scheduling](../performance.md#tps--mspt-aware-scheduling)
   feature automatically switches to Spark's more accurate rolling averages when Spark is installed,
   instead of falling back to Bukkit reflection.
2. **Automatic profiling on high entity counts** — when a cleanup run is about to remove
   more entities than a configurable threshold, EzClean starts a Spark profiler session
   so you can identify _why_ so many entities accumulated.

---

## Requirements

- Spark 1.10+ must be installed as a separate plugin.
- No configuration is required for the TPS/MSPT upgrade — it activates automatically.
- Auto-profiling is **disabled by default** and must be opted in per cleaner profile.

When EzClean detects Spark on startup it logs:

```text
[EzClean] Hooked into Spark — using Spark TPS/MSPT data; auto-profiler available.
```

---

## TPS / MSPT data source

The [TPS-aware scheduling](../performance.md#tps--mspt-aware-scheduling) feature defers cleanups
while the server is under load (`min-tps`, `max-mspt` thresholds).

Without Spark, EzClean reads TPS and MSPT through Paper's server API. When Spark is present, it
instead reads from Spark's 1-minute rolling averages, which are sampled more consistently and
available on a wider range of server implementations.

No extra configuration is needed — the upgrade is transparent.

---

## Auto-profiling

When a cleanup run collects more entities for removal than the configured `threshold`, EzClean
automatically dispatches:

```bash
spark profiler start --order-by-count --timeout <duration-seconds>
```

After `duration-seconds` seconds the profiler stops automatically and uploads a shareable report
link to the console:

```text
[Spark] Profile available at: https://spark.lucko.me/...
```

Share this link with your support team or forum post to get help identifying the entity source.

### Enabling auto-profiling

In your cleaner YAML (e.g. `cleaners/default.yml`):

```yaml
performance:
  spark:
    auto-profile:
      enabled: true
      # Fire the profiler when >= 1000 entities are scheduled for removal.
      threshold: 1000
      # Profile for 30 seconds, then generate a report.
      duration-seconds: 30
```

Reload with `/ezclean reload` — no restart needed.

### Choosing a threshold

Set `threshold` to a value **above your server's normal cleanup count**. Check `/ezclean stats`
to see recent removal counts:

| Typical removal count | Suggested threshold |
|---|---|
| < 200 | 500 |
| 200–500 | 1 000 |
| 500–2 000 | 3 000 |
| > 2 000 | 5 000 |

The profiler only fires when entity counts are abnormally high, so a well-tuned threshold
causes zero overhead on normal runs.

### What the profiler captures

The `--order-by-count` flag sorts the flame graph by call count rather than sample duration,
which is better for identifying code paths that are called frequently (e.g. entity spawning
loops). Focus on methods near the top of the flame graph that relate to entity creation.

{: .note }
Only one Spark profiler session can run at a time. If a session is already active when EzClean
tries to start one, Spark will reject the duplicate command and log a warning — no data is lost.

---

## Interpreting the report

1. Open the Spark report link in your browser.
2. Look for methods related to entity spawning — `EntityManager#addEntity`, `World#spawnEntity`,
   plugin-specific `onTick` / `run` calls.
3. Check the **"Entities"** tab if available (Spark 1.10+) for a real-time snapshot of entity
   counts by type at the time of the profile.

Common causes of large entity buildup:

- **Mob spawners** with no cap configured — spawner-based mobs accumulate indefinitely.
- **Dropped items** from automatic farms — use `remove.dropped-items: true` and configure
  `remove.dropped-items-min-age-ticks` to give players time to collect loot.
- **Custom plugin spawning** loops — visible as repeated calls to `World#spawnEntity` in the
  flame graph.
- **Entity piles** at specific chunk coordinates — use EzClean's
  [pile detection](../configuration.md) to automatically cull these.
