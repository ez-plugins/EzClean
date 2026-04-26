---
title: Folia
parent: Integrations
nav_order: 5
description: Folia-compatible entity removal using region-thread-safe scheduling.
---

# Folia

[Folia](https://papermc.io/software/folia) is a Paper fork that introduces
**regionised multithreading** — each world region runs on its own thread. Standard
`Bukkit.getScheduler()` calls are unsafe on Folia because they always run on the
global thread rather than the owning region thread.

EzClean is fully Folia-aware and uses a scheduler abstraction (`FoliaScheduler`)
that dispatches every task to the correct thread at runtime.

{: .note }
No special configuration is needed. EzClean detects Folia automatically and adjusts
its scheduling behaviour accordingly.

## How EzClean handles Folia

### Entity scanning

On Folia, `world.getEntities()` must be called from the owning region thread.
EzClean performs the entity scan synchronously on the global region thread for Folia
servers, regardless of the `async-removal` setting.

### Entity removal

When `performance.async-removal: true` is set on a Folia server, EzClean dispatches
each `entity.remove()` call to the entity's owning region thread using
`entity.getScheduler().run()`. This means:

- All removals run **concurrently across region threads** instead of serially on one thread.
- The global region thread is unblocked almost immediately after the scan completes.
- Large cleanups (thousands of entities) impose significantly less single-tick overhead.

### Post-cleanup work

`finishCleanup` (stats recording, summary broadcast, post-cleanup commands) always
executes on the global region thread, which is safe for console commands and server-wide
broadcasts.

## Recommended configuration for Folia

```yaml
# cleaners/default.yml
performance:
  async-removal: true        # dispatches per-entity removal to region threads
  async-removal-batch-size: 500   # has no effect on Folia (ignored)
```

{: .tip }
On Folia, `async-removal-batch-size` is ignored because removals are not batched
— every entity is dispatched individually to its own region thread.

## Plugin.yml declaration

EzClean declares Folia support in `plugin.yml`:

```yaml
folia-supported: true
```

This is required for the plugin to load on Folia without a compatibility warning.
