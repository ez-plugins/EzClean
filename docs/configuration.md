---
title: Configuration
nav_order: 3
description: Full YAML configuration reference for EzClean.
---

# Configuration

EzClean uses four files inside `plugins/EzClean/`.
All files are created with safe defaults on first launch.

| File | Purpose |
|---|---|
| `config.yml` | Global plugin settings |
| `cleaners/*.yml` | Cleanup profile definitions (one file per profile) |
| `death-chests.yml` | Optional death chest feature |
| `messages.yml` | All player-facing messages (MiniMessage) |

---

## config.yml

```yaml
update-check:
  # Set false to suppress the SpigotMC version check on startup.
  enabled: true
```

---

## cleaners/default.yml

Create additional profiles by copying `default.yml` and giving it a new name, e.g.
`cleaners/nether.yml`. The file name (without `.yml`) becomes the cleaner ID.

### Timer

```yaml
interval-minutes: 60
```

### Warning broadcast

```yaml
warning:
  enabled: false          # toggle with: /ezclean toggle warning
  minutes-before: 5
```

Message text lives in `messages.yml` under `cleaners.<id>.warning.message`.

### Pre-clean pile summary

```yaml
broadcast:
  pre-clean:
    # Shown during the warning phase. Available tags: {minutes}, {cleaner},
    # {top_worlds}, {top_chunks}
    # Message lives in messages.yml under cleaners.<id>.broadcast.pre-clean.message
```

### Broadcast settings

```yaml
broadcast:
  start:
    enabled: true       # broadcast at cleanup start
  summary:
    enabled: true       # broadcast how many entities were removed
  interval:
    enabled: false      # toggle with: /ezclean toggle interval-broadcast
    every-minutes: 15
  dynamic:
    enabled: false      # toggle with: /ezclean toggle dynamic-broadcast
    minutes: [30, 15, 10, 5]
    seconds: []         # e.g. [5,4,3,2,1] for last-second countdowns
  stats-summary:
    enabled: false      # toggle with: /ezclean toggle stats-summary
    every-runs: 5
```

### Pay-to-cancel

```yaml
cancel:
  enabled: true         # toggle with: /ezclean toggle cancel
  cost: 50000.0         # Vault currency
```

Requires a Vault-compatible economy plugin. Set `cost: 0` for a free cancel.

### Worlds

```yaml
worlds:
  - "*"               # match all worlds; or list specific world names
```

If WorldGuard is installed, regions with the flag `ezclean-bypass` set to `ALLOW`
are automatically excluded per-cleanup.

### Pile detection

```yaml
pile-detection:
  enabled: true         # toggle with: /ezclean toggle pile-detection
  max-per-block: 120
  entity-types:
    - ITEM
    - EXPERIENCE_ORB
  ignore-named-entities: true
```

### Entity removal rules

```yaml
remove:
  hostile-mobs: true
  passive-mobs: false
  villagers: false
  dropped-items: true
  projectiles: true
  experience-orbs: true
  area-effect-clouds: true
  falling-blocks: true
  primed-tnt: true

protect:
  players: true
  armor-stands: true
  display-entities: true
  tamed-mobs: true
  name-tagged-mobs: true

entity-types:
  keep: []     # always kept, overrides remove rules
  remove: []   # always removed, after keep/protect rules
```

### Performance (async removal)

```yaml
performance:
  # Spread entity removal over multiple ticks (500 entities/tick).
  # Safe to enable on large servers. Disabled by default to preserve
  # synchronous-removal guarantees for vanilla-compatible plugins.
  # Toggle live with: /ezclean toggle async-removal
  async-removal: false
```

See the [Performance guide](performance.md) for when to enable this.

---

## messages.yml

All messages use [MiniMessage](https://docs.advntr.dev/minimessage/format.html) format.
Keys are organised under `cleaners.<id>.*` for profile-specific messages.
Run `/ezclean reload` after editing.

---

## death-chests.yml

Death chests are **disabled by default**. Toggle globally with:

```
/ezclean toggle death-chests
```

See the full key reference inside `death-chests.yml` — every key is documented
with a comment.

