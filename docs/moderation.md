---
title: Moderation guide
nav_order: 7
description: Admin workflows for managing EzClean on a live server.
---

# Moderation guide

This page covers common admin workflows for managing EzClean on a running server without
requiring restarts.

## Enabling and disabling features live

Use `/ezclean toggle` to flip any feature on or off. Changes are written to YAML immediately
so they persist across restarts.

```
/ezclean toggle async-removal         # enable spread-tick removal (recommended for large servers)
/ezclean toggle pile-detection        # enable pile culling
/ezclean toggle warning               # enable pre-cleanup warning broadcast
/ezclean toggle cancel                # enable pay-to-cancel for players
/ezclean toggle death-chests          # enable the death chest feature globally
```

After a toggle the plugin reloads automatically. No `/restart` or `/reload` is needed.

## Triggering a manual cleanup

```
/ezclean run              # run all cleaner profiles immediately
/ezclean run default      # run only the "default" profile
```

Useful for cleaning up after a large event or lag spike before the scheduled run.

## Checking cleanup timers

```
/ezclean time             # when will each profile next run?
```

## Reviewing cleanup stats

```
/ezclean stats            # totals: runs, removed entities, average TPS impact
/ezclean stats default    # stats for the "default" profile only
```

Stats are persistent across restarts (stored in `plugins/EzClean/stats/`).

## Reloading configuration

After editing any YAML file manually:

```
/ezclean reload
```

All cleanup profiles, messages, and death chest settings are reloaded without restart.

{: .warning }
`/ezclean reload` cancels all active timers and reschedules them. Any pending cleanup
countdown broadcasts will restart from the beginning.

## Diagnosing scheduler overhead

If other plugins are causing scheduler lag, use the usage inspector:

```
/ezclean usage            # overview of pending tasks by plugin
/ezclean usage live       # live action-bar + chat view with auto-refresh
/ezclean usage SomePlugin # filter to a specific plugin
/ezclean usage stop       # stop the live view
```

## Managing player death chests

Death chests are off by default. Enable them with:

```
/ezclean toggle death-chests
```

Players with `ezclean.deathchest.protection.bypass` can open any player's death chest
(e.g. to retrieve loot for an offline player). Players with `ezclean.deathchest.limit.bypass`
bypass the per-player chest limit set in `death-chests.yml`.

## Multiple cleaner profiles

Create a second profile by copying `plugins/EzClean/cleaners/default.yml` to a new file
(e.g. `nether.yml`), edit it, and run `/ezclean reload`. The new profile becomes active
immediately.

To restrict a profile to specific worlds, set:

```yaml
worlds:
  - world_nether
```

## WorldGuard region bypass

If WorldGuard is installed, you can exclude a region from cleanups by setting the EzClean
bypass flag:

```
/rg flag <region> ezclean-bypass allow
```

Remove the flag to restore normal cleanup behaviour in that region:

```
/rg flag <region> ezclean-bypass deny
```

## Security notes

- `/ezclean toggle` requires `ezclean.toggle` (default: op). Do not grant this to untrusted
  players — toggling `async-removal` or `pile-detection` can affect server performance.
- The pay-to-cancel cost is set server-side in YAML. Players cannot adjust costs.
- Death chest loot is protected per-player. Only bypass-permission holders can access
  other players' chests — audit these grants carefully.
