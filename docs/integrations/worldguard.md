---
title: WorldGuard
parent: Integrations
nav_order: 2
description: Exclude WorldGuard regions from EzClean entity removal.
---

# WorldGuard

[WorldGuard](https://dev.bukkit.org/projects/worldguard) is the standard region-protection
plugin for Bukkit servers. EzClean registers a custom region flag that lets you mark
areas where entity cleanup should never run.

## Requirements

- WorldGuard 7.0.9 or later
- WorldEdit (required by WorldGuard)

## The `ezclean-bypass` flag

EzClean registers a custom **StateFlag** named `ezclean-bypass` during the plugin load
phase (before `onEnable`), so it is always available in WorldGuard's flag registry by the
time worlds load.

When the flag is set to `ALLOW` on a region, every entity inside that region is skipped
by all EzClean cleaner profiles during every cleanup run. The flag defaults to `DENY`
(i.e. cleanup proceeds normally).

## Setting the flag

Select a region with WorldEdit and run:

```
/rg flag <region-name> ezclean-bypass allow
```

To remove the bypass and restore normal cleanup behaviour:

```
/rg flag <region-name> ezclean-bypass deny
```

You can also set the flag in `regions.yml` directly or via a WorldGuard GUI plugin if
you prefer a graphical interface.

{: .tip }
The bypass flag is evaluated per-entity based on the entity's current location. It
respects region priority and inheritance, so child regions inherit the flag from parent
regions unless explicitly overridden.

## Configuration

No EzClean configuration is required — the integration activates automatically when
WorldGuard is present. You can see per-world region skipping in the server log at the
`FINE` logging level.
