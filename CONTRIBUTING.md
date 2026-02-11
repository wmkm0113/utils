# Contributing to utils-jdk11

First of all, thank you for considering contributing to utils-jdk11 🎉  
Any kind of contribution — bug reports, feature requests, documentation improvements, or code — is welcome.

This document explains how to contribute effectively and safely.

# 📌 Project Scope & Philosophy

utils-jdk11 is a general-purpose Java 11 utility library intended to provide:

+ Stable, reusable APIs
+ Clear behavior and safe defaults
+ Minimal external dependencies
+ Long-term backward compatibility

Please keep these principles in mind when contributing.

# 🧭 What We Accept
## ✅ Good Contributions

+ Bug fixes with clear reproduction steps
+ Performance improvements
+ New utility methods with clear, general-purpose value
+ Documentation and Javadoc improvements
+ Unit tests

## ❌ What We Avoid

+ Highly project-specific logic
+ Breaking API changes without discussion
+ Duplicate utilities already well-covered by JDK or widely adopted libraries
+ Silent behavior changes without documentation

If you’re unsure whether something fits, open an issue first.

# 🐛 Reporting Bugs

**Before opening a bug report:** 

+ Check existing issues to avoid duplicates
+ Confirm the issue occurs on the latest version 

**When reporting a bug, please include:** 

+ Library version
+ Java version (JDK 11+)
+ Minimal reproducible example
+ Expected vs actual behavior
+ Stack trace (if applicable)

# 📐 Code Style & Guidelines
**General Rules** 

+ Follow standard Java naming conventions
+ **Prefer clarity to cleverness**
+ Avoid unnecessary abstraction
+ Keep methods small and focused

**API Design Rules**

+ Public APIs should be stable and backward-compatible
+ Avoid exposing internal implementation details
+ Do not return null unless clearly documented
+ Prefer Optional<T> for optional results
+ Document exceptions explicitly

**Javadoc**

All public classes and methods **must include Javadoc**, describing:

+ Purpose
+ Parameters
+ Return values
+ Exceptions
+ Thread-safety (if relevant)

# 🧪 Tests

+ New features must include tests
+ Bug fixes should include a regression test
+ Tests should be deterministic and fast

If a change cannot be reasonably tested, explain why in the PR.

# 🔀 Pull Request Process

1. Fork the repository
2. Create a feature or fix a branch:
    ```
    git checkout -b feature/my-feature
    ```
3. Make your changes
4. Ensure:
   + Code builds successfully
   + Tests pass
   + Javadoc is updated
5. Submit a pull request with:
   + Clear description of changes
   + Motivation and context
   + Any breaking changes are clearly noted

Small, focused PRs are preferred over large ones.

# 🔒 Backward Compatibility Policy

Because utils-jdk11 is published to Maven Central:

+ **Breaking changes require a major version bump**
+ Behavioral changes must be documented
+ Deprecated APIs should be marked with @Deprecated before removal

# 📄 License

By contributing to this project, you agree that your contributions will be licensed under the Apache License 2.0, consistent with the rest of the project.

# 🙏 Thank You

Your contributions help make utils-jdk11 better and more reliable for everyone.
We appreciate your time and effort!