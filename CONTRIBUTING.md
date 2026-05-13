# Contributing to Azertio

Thank you for your interest in contributing to Azertio!

## How to contribute

### Reporting bugs

Open an issue using the **Bug Report** template. Include:
- A clear description of the problem
- Steps to reproduce
- Expected vs actual behavior
- Azertio version and Java version

### Suggesting features

Open an issue using the **Feature Request** template. Describe the use case and why it would benefit the project.

### Submitting changes

1. Fork the repository and create your branch from `develop`:
   ```
   git checkout -b feature/my-feature develop
   ```
2. Make your changes, following the code style below.
3. Add or update tests as needed.
4. Run the full test suite to ensure nothing is broken:
   ```
    ./mvnw clean verify
    ./mvnw clean verify -P plugins
   ```
   The Maven wrapper enables reactor parallelism by default. If you need to debug a flaky build in serial mode, use `./mvnw -T 1 clean verify`.
5. Open a Pull Request targeting the `develop` branch.

## Code style

- Java 21, formatted with standard conventions (no tabs, 4-space indent).
- All public API must have Javadoc.
- Tests use JUnit 5 and AssertJ.

## Branch strategy

| Branch    | Purpose                              |
|-----------|--------------------------------------|
| `main`    | Stable releases only (protected)     |
| `develop` | Integration branch for ongoing work  |
| `feature/*` | New features, branched from `develop` |
| `fix/*`   | Bug fixes, branched from `develop`   |

## License

By contributing you agree that your contributions will be licensed under the [MIT License](LICENSE).
