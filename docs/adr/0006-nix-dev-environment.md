# ADR-0006: Reproducible toolchain with a Nix flake and direnv

- **Status:** accepted
- **Date:** 2026-10-25

## Context

"Works on my machine" problems usually come from tool versions: a machine may have JDK 25 installed,
which Gradle 8.14 cannot run on, while the project targets JDK 21. Reviewers and teammates should get the
same toolchain with one command.

## Decision

- `flake.nix` defines a dev shell with JDK 21 (Zulu), Gradle 8.14 built against that JDK, git, curl, jq
  and httpie. `flake.lock` pins the exact nixpkgs revision.
- `.envrc` (`use flake`) makes direnv load the shell automatically on `cd`.
- `GRADLE_USER_HOME` points at `.gradle-home/` inside the project, so builds do not depend on
  `~/.gradle` state.
- `SPRING_PROFILES_ACTIVE=dev` makes local runs use developer-friendly settings (`application-dev.yml`,
  which includes the `demo` seed data). The Gradle test task strips this variable, so the test suite behaves
  the same inside and outside the shell.
- The Gradle wrapper and foojay toolchain resolver stay in place, so the project also builds without Nix.

## Consequences

- ➕ Identical toolchain on every machine and in CI (`nix develop -c ./gradlew test`).
- ➕ Nothing installed globally; leaving the directory unloads everything.
- ➖ Requires Nix; the first load downloads several hundred MB.
- ➖ Two ways to get Gradle (Nix and the wrapper). The wrapper is the source of truth for the build;
  Nix provides the JDK.
