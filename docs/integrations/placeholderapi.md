---
title: PlaceholderAPI
parent: Integrations
nav_order: 3
description: Expose EzClean stats as %ezclean_*% placeholders and use PAPI tokens in broadcast messages.
---

# PlaceholderAPI

[PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (PAPI) is the
standard placeholder engine for Bukkit plugins. EzClean integrates with it in two ways:

1. **Expansion** — EzClean registers its own expansion so other plugins (scoreboards,
   holograms, chat formatters, etc.) can display EzClean data via `%ezclean_*%` placeholders.
2. **Message processing** — any `%papi_placeholder%` token placed inside an EzClean
   broadcast message template is resolved **per player** at send time, so each player
   sees their own values.

## Requirements

- PlaceholderAPI 2.11 or later

The integration activates automatically when PlaceholderAPI is present. EzClean logs a
confirmation line on startup:

```
[EzClean] Hooked into PlaceholderAPI — EzClean placeholders registered.
```

## Available placeholders

All placeholders use the `ezclean` identifier. Cleaner-specific placeholders accept an
optional `_<cleanerId>` suffix. When the suffix is omitted the first registered cleaner
is used automatically.

| Placeholder | Description |
|---|---|
| `%ezclean_cleaners%` | Comma-separated list of all registered cleaner IDs |
| `%ezclean_next%` | Human-readable time until the next cleanup (first cleaner) |
| `%ezclean_next_<id>%` | Time until the next cleanup for the named cleaner |
| `%ezclean_last_removed%` | Entities removed in the last cleanup run (first cleaner) |
| `%ezclean_last_removed_<id>%` | Last-run count for the named cleaner |
| `%ezclean_total_removed%` | All-time total entities removed (first cleaner) |
| `%ezclean_total_removed_<id>%` | All-time count for the named cleaner |
| `%ezclean_total_runs%` | Total number of cleanup runs recorded (first cleaner) |
| `%ezclean_total_runs_<id>%` | Run count for the named cleaner |

Placeholders that have no data to display (e.g. no runs recorded yet) return `N/A`.

### Example — TAB scoreboard

```yaml
# config.yml snippet for a TAB plugin sidebar
sidebar:
  - "&eNext cleanup: &a%ezclean_next%"
  - "&7Last removed: &f%ezclean_last_removed%"
```

## Using PAPI placeholders in broadcast messages

You can embed any PlaceholderAPI placeholder directly in an EzClean broadcast template
(warning, summary, interval, dynamic, pre-clean, stats-summary). Tokens are resolved
per player immediately before the message is rendered, so each player sees their own
values.

**Example** — show each player's balance in the warning broadcast:

```yaml
# messages.yml
cleaners:
  default:
    warning:
      message: "<yellow>⚠ Cleanup in <gold>{minutes}</gold> min. Your balance: <green>%vault_eco_balance_formatted%</green></yellow>"
```

{: .note }
PAPI placeholder resolution runs before MiniMessage deserialisation. Standard EzClean
MiniMessage tags (`{minutes}`, `{cleaner}`, `{count}`, etc.) are still applied
afterwards and work normally alongside PAPI tokens.

{: .tip }
Server-level placeholders (e.g. `%server_online%`) work in all broadcast messages.
Player-specific placeholders (e.g. `%player_name%`, economy balances) are resolved
per online player and may return an empty string for the console log.
