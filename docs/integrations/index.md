---
title: Integrations
nav_order: 7
has_children: true
description: Optional plugin integrations supported by EzClean.
---

# Integrations

EzClean is designed to work alongside a range of popular server plugins.
All integrations are **optional** and soft-dependent — EzClean loads and operates
normally when any of them are absent.

| Integration | Purpose |
|---|---|
| [Vault](vault.md) | Pay-to-cancel cleanup mechanic using any Vault economy |
| [WorldGuard](worldguard.md) | Exclude regions from cleanup with a custom flag |
| [PlaceholderAPI](placeholderapi.md) | Expose EzClean data as `%ezclean_*%` placeholders and use PAPI tokens in broadcast messages |
| [EzCountdown](ezcountdown.md) | Mirror cleanup countdowns to EzCountdown's display system |
| [Folia](folia.md) | Full Folia compatibility with region-thread-safe entity removal |
| [Spark](spark.md) | Automatic CPU profiling to identify the root source of entity buildup |

To activate an integration simply install the corresponding plugin alongside EzClean.
No extra configuration is required unless documented per integration.
