# Contributing to EzClean

Thanks for your interest in contributing to EzClean! We welcome bug reports, improvements, documentation updates, and tests.

## Getting started

- Requirements: JDK 25 (matches `pom.xml`), Maven 3.6+, Git.
- Recommended IDE: IntelliJ IDEA or VS Code with Java support.

## Build & test

```bash
# run tests
mvn -U -e clean test

# build packaged JAR
mvn -DskipTests=false package
```

## Workflow

1. Fork the repository.
2. Create a branch named `feature/your-feature` or `fix/issue-123`.
3. Implement your changes and include tests where appropriate.
4. Run `mvn -U -e clean test` and ensure the build passes.
5. Open a pull request describing the change and how to test it.

## Code style and quality

- Keep methods small and single-responsibility.
- Add unit tests for new logic and edge cases.
- Follow existing package structure and naming conventions.

## Reporting issues

- Use the issue tracker and provide steps to reproduce, server version, plugin version, relevant config, and logs.

## License

By contributing, you agree that your contributions will be licensed under the project's MIT license.
