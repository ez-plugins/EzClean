# EzClean – Minecraft Entity Cleaner Plugin

**Automated entity sweeps, live countdowns, pay-to-cancel, and async scheduler monitoring for SpigotMC, Paper, and Purpur 1.21+**

**SpigotMC/Bukkit, Paper, Purpur 1.21+, Java 17+, Multi-profile scheduler, MiniMessage broadcasts, WorldGuard bypass, Vault (optional)**

---

## Why Choose EzClean?
EzClean is designed for modern Minecraft servers that demand performance, flexibility, and safety. Here’s why it stands out:

- **100% SpigotMC/Bukkit compatible** – Works on Spigot, Paper, and Purpur. No Paper-only dependencies.
- **Profile-based scheduling** – Create multiple cleaner profiles, each with its own interval, world targets, entity rules, and broadcast messages.
- **Interactive countdowns** – Themed warnings, recaps, and staff-only cancel prompts using MiniMessage placeholders: `{minutes}`, `{count}`, `{cleaner}`, `{cost}`.
- **Granular removal rules** – Toggle hostile mobs, passive mobs, villagers, vehicles, drops, orbs, projectiles, TNT, clouds. Protect players, armor stands, displays, tamed pets, named mobs.
- **World targeting** – Clean specific worlds or use `*` for network-wide sweeps.
- **Force keep/remove lists** – Tag any `EntityType` to always keep or remove for custom events or mechanics.

---

## Feature Highlights
EzClean offers powerful features to keep your Minecraft server running smoothly and your players happy:

- **MiniMessage ready** – Themed warning, interval, dynamic, pre-clean, and cancel broadcasts using Adventure MiniMessage tags: `minutes`, `count`, `cleaner id`, `cancel costs`.
- **WorldGuard integration** – Regions flagged with `ezclean-bypass` are automatically skipped during entity sweeps.
- **Pay-to-cancel safety net** – Optional Vault hooks let you charge players to cancel a cleanup, with hover tooltips and refund protection.
- **Safe defaults** – Critical entities are protected until you explicitly opt in to remove them.
- **Lightweight configuration** – Cleaners, MiniMessage copy, and death chests in tidy YAML files.
- **Hot reload friendly** – Reload profile changes instantly with your favorite plugin manager or `/reload confirm`.
- **Smart pile detection** – Automatically cull stacks of drops or other spammy entities that exceed your per-block limit.
- **Optional death chests** – Capture player drops in temporary containers that despawn on a timer instead of scattering items everywhere.

---

## Configure What Gets Cleaned
Take full control over what entities are removed or protected on your Minecraft server. Mix and match profiles, entity groups, and custom rules for ultimate flexibility.

- Create unlimited cleaner profiles, each with unique world targets, countdown cadence, and broadcast messages.
- Toggle entity groups—hostile mobs, passive mobs, villagers, vehicles, drops, orbs, projectiles, clouds, falling blocks, TNT—per profile.
- Fine-tune specific `EntityType` values using `keep`/`remove` arrays to protect custom mobs or purge event clutter.
- Mix profiles for global sweeps, world-specific cleanups, or niche arenas—without affecting other schedules.
- Reload the plugin to instantly apply changes and monitor `/ezclean time` for updated countdowns.

---

## Pay-to-Cancel Cleanups
Let staff delay scheduled cleanups with a single click—optionally charging a Vault-backed fee and showing dynamic MiniMessage prompts.

- Clickable countdown broadcasts let staff with `ezclean.cancel` permission delay the next cleanup instantly.
- Charge an optional Vault-backed fee per profile and automatically refund players if cancellation fails.
- Customize hover, success, broadcast, and error messages with MiniMessage placeholders: `{player}`, `{cleaner}`, `{minutes}`, `{cost}`.
- Players without permission see the standard warning—keeping the feature gated to trusted ranks.

---

## Monitor Async Scheduler Load

![Scheduler Monitor Example](https://i.ibb.co/Rk6h5JvX/image.png)
## Tracking the Cause of Lag

EzClean makes it easy to diagnose and track down the source of server lag, especially when it is related to scheduled tasks or plugin activity. Use the following features to pinpoint lag causes:

- **Scheduler Usage Snapshots:**
  - Run `/ezclean usage` to instantly view a breakdown of all pending sync and async tasks, sorted by plugin. This helps you identify which plugins are scheduling the most work and may be contributing to lag.

- **Live Usage Monitoring:**
  - Use `/ezclean usage live` to start a real-time feed in chat and action bar, showing task counts and the busiest plugins as they change. This is ideal for catching lag spikes as they happen and correlating them with plugin activity.

- **Plugin Filtering:**
  - Add a plugin name to `/ezclean usage` or `/ezclean usage live` to focus on a specific plugin's scheduler activity.

- **Stopping the Feed:**
  - Use `/ezclean usage stop` to end a live monitoring session at any time.

- **Visual Feedback:**
  - The action bar graph and chat output make it easy to spot sudden increases in scheduled tasks, which often correlate with lag events.

**Tip:** If you notice a spike in scheduled tasks or a particular plugin dominating the scheduler, investigate that plugin's configuration or recent activity. EzClean's tools are especially useful during peak times or after adding new plugins.

![Scheduler Monitor Example](https://i.ibb.co/Rk6h5JvX/image.png)

---

## Optional Death Chests
- Convert player drops into a 54-slot chest near the death location instead of littering the ground with items.
- Set a despawn timer (or keep chests indefinitely) and auto-drop leftovers if the chest breaks or expires.
- Customize the MiniMessage inventory title and toggle the feature on the fly without restarting the server.
- Keeps messy drop piles off the ground so your cleanup cycles have less to sweep in the first place.

### Advanced Death Chest Options (Optional)
- **Per-player chest limits** – Restrict the number of active death chests each player can have at once to prevent abuse and clutter.
  - Example:
    ```yml
    max-chests-per-player: 2
    ```
    Players exceeding the limit will have their oldest chest removed automatically.
    - Permission: `ezclean.deathchest.limit.bypass` lets a player ignore the chest limit.
- **Configurable loot protection** – Only the owner can open their death chest for a configurable time after death (e.g., 5 minutes), after which anyone may access it.
  - Example:
    ```yml
    loot-protection:
      enabled: true
      owner-only-minutes: 5
    ```
    - Permission: `ezclean.deathchest.protection.bypass` lets a player open any chest regardless of protection.
- **Hologram or particle effects above chests** – Display a floating label or visual effect above each death chest for easy spotting.
  - Example:
    ```yml
    hologram:
      enabled: true
      text: "<gold>Death Chest</gold>"
    particles:
      enabled: true
      type: VILLAGER_HAPPY
    ```

---

## Admin Commands
- `/ezclean run [cleaner]` – Trigger any configured profile instantly when staff need a manual sweep.
- `/ezclean cancel [cleaner]` – Delay the next cleanup cycle (and optionally charge a Vault fee) via command or the clickable broadcast prompt.
- `/ezclean time [cleaner]` – Check how many minutes remain before the next scheduled cleanup runs.
- `/ezclean reload` – Reload EzClean's configuration bundle without restarting to apply new profiles, messages, or entity rules.
- `/ezclean usage [plugin|live|stop] [plugin]` – Diagnose heavy scheduler usage, start a live monitor, or cancel an active session (`ezclean.usage`).
- `/ezclean stats [cleaner]` – View detailed statistics for a cleaner profile, including run counts, total entities removed, average duration, TPS impact, and top affected worlds/groups.
- `ezclean.deathchest.limit.bypass` – Allows a player to ignore the per-player death chest limit.
- `ezclean.deathchest.protection.bypass` – Allows a player to open any death chest regardless of loot protection timer.

---

## Quick Start
- Drop `EzClean.jar` into your Paper or Purpur `plugins/` directory and restart the server.
- Let EzClean generate `cleaners/default.yml`, `messages.yml`, and `death-chests.yml` inside its data folder on first launch.
- Duplicate `cleaners/default.yml` for world-specific schedules, then adjust world lists, removal toggles, broadcast cadence, and cancel costs per profile.
- Theme broadcasts in `messages.yml`, tweak death chest timers in `death-chests.yml`, and reload the plugin (or restart) to apply changes before checking `/ezclean time`.

---

## WorldGuard Bypass Tag
- Install [WorldGuard](https://enginehub.org/worldguard) alongside EzClean to unlock region-based exclusions.
- EzClean automatically registers the `ezclean-bypass` state flag during startup—no manual flag setup required.
- Use `/rg flag <region> ezclean-bypass allow` to mark safe zones (spawn, showcases, redstone labs) that should never be swept.
- Switch the flag back to `deny` (or remove it) when you want EzClean to resume removing entities from that region.
- Entities inside an `allow` region are skipped while the rest of the server is tidied normally.

---

## Cleanup Scope at a Glance
| Category                        | Default   | Notes                                                                 |
|----------------------------------|-----------|-----------------------------------------------------------------------|
| Hostile mobs                     | Enabled   | Removes `Enemy` instances unless explicitly kept.                     |
| Passive mobs                     | Disabled  | Culls `Animals`, `WaterMob`, `Ambient`, `Golem`, and `Allay` entities |
| Villagers & traders              | Disabled  | Targets villagers and wandering traders (`AbstractVillager`)          |
| Vehicles                         | Disabled  | Removes `Vehicle` entities such as boats and minecarts                |
| Dropped items                    | Enabled   | Clears loose `Item` entities server-wide                              |
| Projectiles & orbs               | Enabled   | Targets arrows, snowballs, thrown potions, and experience orbs        |
| Area effect clouds               | Enabled   | Sweeps lingering potion clouds from combat-heavy areas                |
| Falling blocks & TNT             | Enabled   | Removes rogue falling blocks and primed TNT                           |
| Players, armor stands & displays | Protected | Stay intact unless you toggle the `protect.*` switches off            |
| Tamed mobs                       | Protected | Ignored while `protect.tamed-mobs` stays enabled                      |
| Named mobs                       | Protected | Skip entities with custom names when `protect.name-tagged-mobs` is on |

---

## Example Configurations

### cleaners/default.yml
```yml
interval-minutes: 60

warning:
  enabled: false
  minutes-before: 5

broadcast:
  start:
    enabled: true
  summary:
    enabled: true
  interval:
    enabled: false
    every-minutes: 15
  dynamic:
    enabled: false
    minutes: [30, 15, 10, 5]

cancel:
  enabled: true
  cost: 50000.0

worlds:
  - "*"

pile-detection:
  enabled: true
  max-per-block: 120
  entity-types:
    - ITEM
    - EXPERIENCE_ORB
  ignore-named-entities: true

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
  keep: []
  remove: []
```

### messages.yml
```yml
defaults:
  warning:
    message: "<yellow>⚠ Entity cleanup in <gold>{minutes}</gold> minutes. Clear valuables!</yellow>"
  broadcast:
    start:
      message: "<red><bold>✦ Entity cleanup commencing...</bold></red>"
    summary:
      message: "<gray>✓ Removed <gold>{count}</gold> entities. Next cleanup in <gold>{minutes}</gold> minutes.</gray>"
    pre-clean:
      message: ""
    interval:
      message: "<yellow>⚠ Cleanup in <gold>{minutes}</gold> minutes. Clear valuables!</yellow>"
    dynamic:
      message: "<gold>⏳ {cleaner} cleanup in <yellow>{minutes}</yellow> minute(s)!</gold>"
  stats-summary:
    message: "<gray>Cleanup stats for <aqua>{cleaner}</aqua>: <gold>{runs}</gold> runs, <gold>{total_removed}</gold> removed total. Avg duration: <gold>{avg_duration}</gold>. Avg TPS impact: <gold>{avg_tps_impact}</gold>. Top groups: {top_groups}. Top worlds: {top_worlds}.</gray>"
  cancel:
    hover-message: "<yellow>Click to pay <gold>{cost}</gold> to cancel this cleanup.</yellow>"
    success-message: "<green>You paid <gold>{cost}</gold> to cancel the <aqua>{cleaner}</aqua> cleanup.</green>"
    broadcast-message: "<gold>{player}</gold> canceled the <aqua>{cleaner}</aqua> cleanup. Next cleanup in <yellow>{minutes}</yellow> minutes."
    insufficient-funds-message: "<red>You need <gold>{cost}</gold> to cancel the cleanup.</red>"
    disabled-message: "<red>This cleanup cannot be canceled.</red>"
    no-economy-message: "<red>Economy is unavailable. Cleanup cannot be canceled.</red>"

cleaners: {}
```

### death-chests.yml
```yml
enabled: false
despawn-minutes: 30
inventory-title: "<gold>Death Chest</gold>"
```

---

## Requirements
- Java 17 or newer (matches the Paper 1.21 API baseline).
- Paper or Purpur 1.21+ server build.
- Vault + economy provider (only if you enable pay-to-cancel cleanups).
- Optional: A plugin manager if you prefer in-game reloads instead of full restarts.

---

## Support & Links
- Questions or feature requests? [Join our Discord](https://discord.gg/yWP95XfmBS) and open a ticket under the EzClean category.
- Share performance feedback or suggestions on your resource discussion thread so we can keep tuning the defaults.

---

**Ready for spotless worlds?**
Install EzClean today and let automated cleaners do the grunt work while staff focus on players!

[![Try the other Minecraft plugins in the EzPlugins series](https://i.ibb.co/PzfjNjh0/ezplugins-try-other-plugins.png)](https://modrinth.com/collection/Q98Ov6dA)