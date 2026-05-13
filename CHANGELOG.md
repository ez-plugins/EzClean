# Changelog

## 3.0.1 - 2026-05-13

### Bug Fixes
- Fixed `NullPointerException` when running `/ezclean setup` caused by `SetupGUI` being initialized after `registerCommands()` was called during plugin enable.
- Fixed `/ezclean setup` editor doing nothing when clicked: navigating between list and editor screens fired `InventoryCloseEvent`, which wiped the player's session before any click could be processed.

## 3.0.0

EzClean 3.0.0 focuses on making recurring entity cleanup clearer, more configurable, and easier to manage for live servers.

### Highlights
- Improved recurring entity cleanup to help reduce lag from built-up entities over time.
- Better configuration options so server owners can tune cleanup behavior to fit their server.
- More predictable cleanup behavior for ongoing server performance management.
- General improvements to reliability and day-to-day usability.

### What this means for server owners
- You have more control over how and when cleanup happens.
- It is easier to adjust the plugin for different server sizes and play styles.
- Regular cleanup can help keep performance stable during longer uptime periods.
- The update is aimed at making lag prevention simpler to manage without constant intervention.

### Recommended after updating
- Review your configuration before going live.
- Double-check cleanup timing and entity rules so they match your server’s needs.
- Test the new setup during a low-traffic period before full deployment.
- Let staff know about any cleanup behavior changes if they monitor server performance.

### Notes
- If you are upgrading from an older version, review your existing settings carefully.
- Server owners using custom cleanup schedules or stricter entity rules should verify that everything behaves as expected after updating.