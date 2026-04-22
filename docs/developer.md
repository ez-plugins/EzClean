---
title: Developer guide
nav_order: 8
description: Build, test, and contribute to EzClean.
---

# Developer guide

## Prerequisites

| Tool | Version |
|---|---|
| JDK | 25 (`--enable-preview`) |
| Maven | 3.9+ (or use `./mvnw`) |

## Building

```bash
./mvnw clean package
```

The shaded JAR is written to `target/ezclean-<version>.jar`.

## Running tests

```bash
./mvnw test
```

Tests use **JUnit 5** and **Mockito**. No running server is required.
Resource YAML validation is also part of the test suite (`ResourceYamlValidationUnitTest`).

## Code style

Checkstyle is configured in `checkstyle.xml` (run by CI). Key rules:

- No wildcard (`*`) imports
- Standard Java naming conventions
- Braces required for all blocks
- No trailing whitespace

Run locally with:

```bash
./mvnw checkstyle:check
```

## Project structure

```text
src/main/java/com/skyblockexp/ezclean/
├── Bootstrap.java              Plugin lifecycle (onEnable / onDisable)
├── EzCleanPlugin.java          Main plugin class
├── Registry.java               Static service locator
├── command/                    All /ezclean subcommands
├── config/                     CleanupSettings, ConfigurationLoader
├── integration/                Soft-depend shims (WorldGuard, Vault)
├── manager/                    DeathChestManager
├── model/                      DTOs (DeathChest, DeathChestSettings)
├── scheduler/                  EntityCleanupScheduler, RemovalEvaluator
├── service/                    BroadcastService
├── stats/                      CleanupStatsTracker, UsageSnapshot
├── update/                     SpigotUpdateChecker
└── util/                       EntityPileDetector
```

## Adding a new cleaner feature (checklist)

1. Add the YAML key to `cleaners/default.yml` with a comment and a safe default.
2. Add a `private final boolean` field and getter in `CleanupSettings`.
3. Read the key in `CleanupSettings.loadFromSection()` via `section.getBoolean(..., default)`.
4. Wire the feature in `EntityCleanupScheduler` or wherever it applies.
5. Register the toggle key in `ToggleSubcommand.featureToConfigKey()`.
6. Document the key in [configuration.md](configuration.md).
7. Add a test in `ResourceYamlValidationUnitTest` or create a new unit test.

## Configuration migration

Config migration logic lives in `EzCleanConfigurationLoader`. When bumping the config schema:

- Add a new migration step inside the loader.
- Keep old keys readable so upgrades do not wipe existing values.
- Bump the schema version constant so the loader triggers the migration exactly once.

## Pull requests

See [CONTRIBUTING.md](https://github.com/ez-plugins/ezclean/blob/main/CONTRIBUTING.md)
for the full contribution guide. In summary:

- Target the `main` branch.
- Include tests for new logic.
- Run `./mvnw test checkstyle:check` before opening a PR.
