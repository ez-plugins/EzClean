---
title: EzCountdown
parent: Integrations
nav_order: 4
description: Mirror EzClean cleanup countdowns to EzCountdown's display system.
---

# EzCountdown

[EzCountdown](https://www.spigotmc.org/resources/ezcountdown.186927/) is a companion
plugin that displays live countdown timers to players via action bar, boss bar, title,
scoreboard, or chat. When both plugins are installed, EzClean automatically mirrors each
cleanup countdown into EzCountdown's display system so players always see accurate,
beautifully-formatted timers without any extra configuration.

## Requirements

- EzCountdown installed and enabled

## How it works

For each enabled cleaner profile, EzClean creates a **DURATION-type countdown** in
EzCountdown on startup. Every time a cleanup cycle completes, EzClean resets that
countdown to the full interval duration, keeping it in sync with the actual next-run
time.

EzCountdown handles the visual presentation — you configure the display style, format, and
target audience entirely inside EzCountdown's own configuration. EzClean only provides the
timer data.

## Configuration

The integration options live in `config.yml` under the `integrations.ezcountdown` block:

```yaml
integrations:
  ezcountdown:
    # Name template for the countdown created in EzCountdown.
    # Use {cleaner} to embed the cleaner ID.
    countdown-name: "ezclean_{cleaner}"
```

| Key | Default | Description |
|---|---|---|
| `countdown-name` | `ezclean_{cleaner}` | Name pattern for the countdown. `{cleaner}` is replaced with the cleaner ID. |

{: .note }
EzClean uses EzCountdown's Java API at runtime via reflection. No compile-time
dependency is required, so the plugin loads cleanly even without EzCountdown present.

## Troubleshooting

**Countdown not visible to players** — EzCountdown controls all display behaviour.
Ensure the countdown named `ezclean_<id>` (or your custom name) is configured with a
display type in EzCountdown's `countdowns.yml`.

**Timer drifts after a reload** — Run `/ezclean reload` to resync all countdowns after
changing the cleanup interval.
