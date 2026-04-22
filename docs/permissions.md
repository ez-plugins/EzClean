---
title: Permissions
nav_order: 5
description: Permission node reference for EzClean.
---

# Permissions

All permission nodes default to **op** unless stated otherwise.

| Node | Default | Description |
|---|---|---|
| `ezclean.clean` | op | Trigger a cleanup run with `/ezclean run` |
| `ezclean.reload` | op | Reload plugin config with `/ezclean reload` |
| `ezclean.status` | op | Check next cleanup time with `/ezclean time` |
| `ezclean.usage` | op | View scheduler usage with `/ezclean usage` |
| `ezclean.stats` | op | View cleanup statistics with `/ezclean stats` |
| `ezclean.toggle` | op | Toggle plugin features live with `/ezclean toggle` |
| `ezclean.cancel` | **true** | Pay to cancel an upcoming cleanup (player-facing) |
| `ezclean.deathchest.limit.bypass` | op | Bypass the per-player death chest limit |
| `ezclean.deathchest.protection.bypass` | op | Open any player's death chest regardless of ownership |

## Notes

- `ezclean.cancel` is granted to all players by default so they can interact with pay-to-cancel
  broadcasts. Remove this node from non-paying players if you want to restrict cancellations.
- `ezclean.toggle` is intentionally restricted to op-level. Granting it to staff players
  requires a permission plugin (e.g. LuckPerms).
- Death chest bypass nodes do not enable the death chest feature themselves —
  `death-chests.enabled` must be `true` in `death-chests.yml` first.

## Assigning with LuckPerms

```text
/lp user <player> permission set ezclean.toggle true
/lp group admin permission set ezclean.toggle true
```
