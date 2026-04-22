# Release Process

This file describes a minimal release process for publishing EzClean.

1. Update `CHANGELOG.md` with notable changes.
2. Bump the plugin version in `pom.xml`.
3. Create a signed Git tag and push it to the repository.
4. Build the artifact and attach it to a GitHub Release.

Notes about packaging:

- The project uses a shading step which may require compatible versions of the shading plugin and ASM for newer Java versions.
- If packaging fails due to shading/classfile versions, build the release artifacts on a machine with a matching JDK and ensure plugin versions are up-to-date.
