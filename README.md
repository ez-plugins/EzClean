# EzClean

EzClean is a lightweight Paper plugin that periodically removes unwanted entities and optionally manages death-chests. It's designed for performance and configurable cleaning profiles.

## Quick links
- Documentation: docs/index.md
- Contributing: CONTRIBUTING.md
- License: MIT (see LICENSE)

## Requirements
- Paper/PaperMC compatible server (see `plugin.yml` for supported versions)
- Java runtime compatible with the build (see `pom.xml`)

## Auto-Profile Generation

You can auto-generate cleaner profiles for each world using:

```
/ezclean autogenprofiles
```

- This will create a `cleaners/<world>.yml` for every world that does not already have a profile.
- Each profile uses sensible defaults (interval, enabled-worlds, basic broadcasts, etc).
- The command reports how many profiles were created or skipped.
- All user-facing messages are configurable in `messages.yml`.

## Configurable Messages

All command and broadcast messages are loaded from `messages.yml` and can be customized or translated.

## Upgrading

- Existing profiles are not overwritten.
- You can safely run `/ezclean autogenprofiles` after adding new worlds.

---
For more details, see the documentation in `docs/` and the comments in `messages.yml`.
