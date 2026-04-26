---
title: Vault
parent: Integrations
nav_order: 1
description: Pay-to-cancel cleanup mechanic using any Vault economy provider.
---

# Vault

[Vault](https://www.spigotmc.org/resources/vault.34315/) is the standard economy
abstraction layer for Bukkit plugins. EzClean uses Vault to power the optional
**pay-to-cancel** mechanic that lets players spend in-game currency to postpone an
upcoming cleanup cycle.

{: .note }
Vault itself does not provide an economy — it acts as a bridge. You also need an
economy plugin such as EssentialsX, CMI, or PlayerPoints installed alongside it.

## Requirements

- Vault installed and enabled
- A Vault-compatible economy plugin installed and enabled

## How it works

When a countdown broadcast is sent (warning, dynamic, or interval), players with the
`ezclean.cancel` permission see an interactive message. Clicking it runs
`/ezclean cancel <id>`, which:

1. Checks whether cancellation is enabled for the cleaner (`cancel.enabled: true`).
2. Charges the player the configured `cost` via Vault.
3. Resets the countdown to the full interval and broadcasts a cancellation notice.

If Vault is not installed, or no economy provider is registered, the cancel mechanic is
silently disabled and cleaner broadcasts are non-interactive.

## Configuration

Cancellation is configured per cleaner profile inside `cleaners/<id>.yml`:

```yaml
cancel:
  enabled: true
  # Amount in the server's default currency. Set to 0 for a free cancel.
  cost: 50000.0
```

All related messages are defined in `messages.yml` under `cleaners.<id>.cancel.*`
(hover tooltip, success message, broadcast, insufficient-funds, disabled, no-economy).

{: .tip }
Set `cost: 0` to allow free cancellation while still showing an interactive broadcast.
This is useful when you want players to be able to delay cleanup on demand, without
charging them.

## Permissions

| Node | Default | Description |
|---|---|---|
| `ezclean.cancel` | `true` | Allows players to use the pay-to-cancel mechanic |
| `ezclean.clean` | `op` | Allows triggering cleanup manually with `/ezclean run` |
