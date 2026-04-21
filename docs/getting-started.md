---
title: Getting started
nav_order: 2
description: Install EzClean and run your first cleanup in under five minutes.
---

# Getting started

## Requirements

| Requirement | Version |
|---|---|
| Java | 25+ |
| Paper (or fork) | 1.21.4+ |
| Vault (optional) | Any |
| WorldGuard (optional) | 7.x |

## Installation

1. Download the latest `ezclean-*.jar` from [SpigotMC](https://www.spigotmc.org/resources/ezclean.129782/)
   or from the [GitHub releases page](https://github.com/ez-plugins/ezclean/releases).
2. Place the JAR in your server's `plugins/` folder.
3. Restart the server (or use a plugin loader that supports hot-loading).
4. EzClean will create its data folder at `plugins/EzClean/` containing:
   - `config.yml` — global settings
   - `death-chests.yml` — death chest feature toggle and settings
   - `messages.yml` — all player-facing messages (MiniMessage format)
   - `cleaners/default.yml` — the default cleanup profile

## First run

After installation, a single `default` cleaner is active.  
It runs every **60 minutes** and removes hostile mobs, dropped items, projectiles,
experience orbs, area-effect clouds, falling blocks, and primed TNT.

All __heavy__ features (pile-detection, warnings, cancellation, async removal, death chests)
are **off by default**. Enable them safely one at a time via `/ezclean toggle` or by editing
the relevant YAML files and running `/ezclean reload`.

### Verify it is working

```
/ezclean time            # Shows minutes until the next cleanup run
/ezclean run             # Triggers a manual cleanup immediately
/ezclean stats           # Shows total runs and removed entity counts
```

## Next steps

- [Configure cleanup profiles](configuration.md)
- [Enable performance features when ready](performance.md)
- [Review all available commands](commands.md)
