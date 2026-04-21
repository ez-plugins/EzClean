---
title: Commands
nav_order: 4
description: Full reference for all /ezclean subcommands.
---

# Commands

All commands require the `ezclean` permission group (or individual nodes listed on the
[Permissions page](permissions.md)).

| Command | Permission | Description |
|---|---|---|
| `/ezclean run [id]` | `ezclean.clean` | Immediately trigger a cleanup for one or all cleaner profiles |
| `/ezclean cancel [id]` | `ezclean.cancel` | Pay to cancel the next upcoming cleanup (Vault required) |
| `/ezclean reload` | `ezclean.reload` | Reload all YAML config files without restarting the server |
| `/ezclean time [id]` | `ezclean.status` | Show minutes remaining until the next cleanup for a profile |
| `/ezclean toggle <feature> [id]` | `ezclean.toggle` | Toggle a feature on/off live and persist it to YAML |
| `/ezclean usage [plugin\|live\|stop]` | `ezclean.usage` | Inspect Bukkit scheduler usage by plugin |
| `/ezclean stats [id]` | `ezclean.stats` | View detailed cleanup statistics |

---

## `/ezclean run`

Executes a cleanup immediately, bypassing the timer. Useful for testing or one-off maintenance.

If multiple cleaner profiles are configured, use the profile ID to target one:

```
/ezclean run           # runs all profiles
/ezclean run default   # runs only the "default" profile
```

---

## `/ezclean toggle`

Flips a feature on or off for a specific cleaner profile (or globally for `death-chests`).
Changes are written directly to the relevant YAML file and the plugin reloads automatically —
no server restart required.

```
/ezclean toggle async-removal            # toggle for the single profile (or prompts for ID)
/ezclean toggle async-removal default    # explicit profile ID
/ezclean toggle death-chests             # global feature (no profile ID needed)
```

### Toggleable features

| Feature | YAML key | Scope |
|---|---|---|
| `pile-detection` | `pile-detection.enabled` | Per cleaner |
| `warning` | `warning.enabled` | Per cleaner |
| `cancel` | `cancel.enabled` | Per cleaner |
| `interval-broadcast` | `broadcast.interval.enabled` | Per cleaner |
| `dynamic-broadcast` | `broadcast.dynamic.enabled` | Per cleaner |
| `stats-summary` | `broadcast.stats-summary.enabled` | Per cleaner |
| `async-removal` | `performance.async-removal` | Per cleaner |
| `death-chests` | `death-chests.enabled` | Global |

---

## `/ezclean cancel`

Lets a player pay (via Vault) to cancel the next upcoming cleanup cycle. Requires:

- `cancel.enabled: true` in the cleaner profile
- A Vault-compatible economy plugin

The cost is configurable in `cleaners/<id>.yml` under `cancel.cost`.

---

## `/ezclean usage`

Shows a snapshot of pending and active Bukkit scheduler tasks grouped by plugin, with an ASCII
load bar. Useful for diagnosing scheduler overhead from other plugins.

```
/ezclean usage                    # static snapshot of all plugins
/ezclean usage EzClean            # filter to a single plugin
/ezclean usage live               # continuous live view in chat + action bar
/ezclean usage live EzClean       # live view filtered to EzClean
/ezclean usage stop               # stop the live view
```

---

## `/ezclean stats`

Displays accumulated cleanup statistics: total runs, total entities removed, average duration
and TPS impact per profile, and a breakdown of removal reasons (entity type groups) and worlds.

```
/ezclean stats            # stats for all profiles
/ezclean stats default    # stats for the "default" profile only
```

