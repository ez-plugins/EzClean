---
title: Home
nav_order: 1
description: EzClean — automated, performance-first entity cleanup for Paper servers.
---

# EzClean

**EzClean** keeps your Paper server's worlds clean by automatically removing stray entities
on a configurable schedule. It is designed to impose the minimum possible TPS overhead — even
on servers with tens of thousands of entities.

{: .highlight }
EzClean v3 is fully open-source. Performance-intensive features are **disabled by default**
and can be enabled by an admin at any time, in-game, with a single command.

## Features

- **Flexible cleaner profiles** — define multiple independent cleanup schedules (e.g. one for
  the overworld every hour, one for the nether every 20 minutes)
- **Per-world interval overrides** — give individual worlds their own cleanup interval inside
  an existing profile without duplicating config; worlds without an override share the global
  timer automatically
- **Minimum player gates** — skip a cleanup run entirely when fewer than N players are online
  globally, or skip a specific world when fewer than N players are present inside it
- **Pile detection** — automatically culls entity piles that exceed a configurable threshold
- **Warning & cancellation system** — warn players before a cleanup; let them pay (via Vault)
  to cancel it
- **Post-cleanup commands** — run arbitrary console commands after each cleanup completes
  (e.g. summon effects, broadcast messages)
- **Death chests** — optional per-player death chest that holds loot for a configurable time
- **Async entity removal** — spread large removal batches across multiple ticks to keep TPS
  stable (disabled by default, toggle live with `/ezclean toggle async-removal`)
- **Live stats** — inspect total runs, removed counts, average TPS impact, and more
- **WorldGuard integration** — respects regions with the `ezclean-bypass` flag
- **Vault integration** — pay-to-cancel supported out of the box

## Quick links

| | |
|---|---|
| [Getting started](getting-started.md) | Install and configure in under five minutes |
| [Configuration](configuration.md) | Full reference for all YAML keys |
| [Commands](commands.md) | All `/ezclean` subcommands |
| [Permissions](permissions.md) | Permission node reference |
| [Performance guide](performance.md) | Tuning EzClean for large servers |
| [Moderation guide](moderation.md) | Admin workflows and best practices |
| [Developer guide](developer.md) | Building, testing, and contributing |
