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

### Minimum player gate

Skip a cleanup run entirely when fewer than N players are online globally.
Useful for empty or near-empty servers where cleanup work is unnecessary.

```yaml
# 0 (default) — always run regardless of player count.
min-players: 0
```

A per-world threshold can also be set inside `world-overrides` (see [per-world overrides](#per-world-overrides) below).

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

### Spawn-reason filtering

Filter entity removal based on how an entity was originally spawned.

```yaml
spawn-reasons:
  restrict: []       # never remove entities with these reasons
  force-remove: []   # always remove, bypassing tamed/name-tagged protection
```

Common reason names: `NATURAL`, `SPAWNER`, `SPAWNER_EGG`, `BREEDING`, `CHUNK_GEN`,
`CUSTOM`, `COMMAND`, `EGG`, `DISPENSE_EGG`, `BUILD_SNOWMAN`, `VILLAGE_DEFENSE`,
`REINFORCEMENTS`, `JOCKEY`, `TRAP` (case-insensitive).

### Per-world overrides

Override any combination of `spawn-reasons`, `interval-minutes`, and `min-players` for a
specific world. Keys are optional — omit any key to inherit the global profile value.

```yaml
world-overrides:
  world_nether:
    # Independent cleanup interval for this world only.
    # The global timer skips this world when a per-world interval is configured.
    # Omit or set to -1 to fall back to the global interval.
    interval-minutes: 30

    # Skip cleaning this world when fewer than N players are present inside it.
    # 0 (default) — always clean regardless of occupancy.
    min-players: 1

    # spawn-reasons replaces (does not merge with) the global block for this world.
    spawn-reasons:
      restrict: []
      force-remove: []
```

{: .note }
Per-world interval timers fire **silently** — no countdown broadcasts are sent for them.
Warning and dynamic/interval broadcasts still come from the global timer only.

### Post-cleanup commands

Run arbitrary console commands immediately after every cleanup finishes, once the
removal and summary broadcast are complete.

```yaml
post-cleanup-commands: []
```

Example:

```yaml
post-cleanup-commands:
  - execute in overworld run summon lightning_bolt ~ ~ ~
  - say Cleanup complete!
```

Commands run as the console sender on the main thread in the listed order.

### Performance (async removal)

```yaml
performance:
  # Paper/Spigot: moves the entity scan off the main thread; spreads removal over
  # multiple ticks (async-removal-batch-size entities/tick) so the main thread is never
  # blocked by a large single-tick removal spike.
  # Folia: dispatches each entity.remove() to the entity's owning region thread via
  # the entity scheduler, so all removals execute concurrently across region threads.
  # Toggle live with: /ezclean toggle async-removal
  async-removal: false
  # Number of entities removed per tick when async-removal is true (Paper/Spigot only).
  # Lower values spread load further; higher values complete removal in fewer ticks.
  # Has no effect on Folia — all removals are dispatched in a single pass.
  async-removal-batch-size: 500
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

```text
/ezclean toggle death-chests
```

See the full key reference inside `death-chests.yml` — every key is documented
with a comment.
