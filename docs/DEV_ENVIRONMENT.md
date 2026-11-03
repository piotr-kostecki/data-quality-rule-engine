# Development environment: Nix flake + direnv

The project brings its own toolchain. You do not need Java or Gradle installed globally; Nix provides
them, and direnv activates them when you `cd` into the directory.

## Files

| File | Role |
|---|---|
| `flake.nix` | Declares the dev shell: JDK 21, Gradle, git, curl, jq, httpie, `JAVA_HOME`, `GRADLE_USER_HOME`, `SPRING_PROFILES_ACTIVE=dev` |
| `flake.lock` | Pins the exact nixpkgs commit, so everyone gets bit-for-bit the same tools. Commit it. |
| `.envrc` | One line, `use flake`: tells direnv to load that shell |
| `.direnv/` | direnv/nix-direnv cache (git-ignored) |
| `.gradle-home/` | Project-local Gradle cache and wrapper distributions (git-ignored) |

## Daily use

```bash
cd <repository>           # direnv loads the shell (cached after the first time)
java -version             # 21.0.x from /nix/store, not a system-wide JDK
./gradlew test
./gradlew bootRun         # Spring profile "dev" is already active (see below)
cd ..                     # leaves the shell; your global PATH is back
```

## The `dev` Spring profile

The shell exports `SPRING_PROFILES_ACTIVE=dev`. Spring Boot reads that environment variable at startup, so
any app you run here (`./gradlew bootRun`, or `java -jar build/libs/*.jar`) loads
`onboarding-server/src/main/resources/application-dev.yml` on top of `application.yml`:

- the profile group `dev: demo` also activates `demo`, which seeds three partners;
- DEBUG logging for `com.example.onboarding`;
- indented JSON responses;
- every actuator endpoint (`/actuator/env`, `/actuator/beans`, `/actuator/mappings`, …) and full health details.

Two details:

- **Tests are isolated from it.** `onboarding-server/build.gradle` removes `SPRING_PROFILES_ACTIVE` from the test task's
  environment; otherwise demo data would leak into integration tests that expect an empty store.
- **Override per run** when you want something else:
  `SPRING_PROFILES_ACTIVE= ./gradlew bootRun` (no profile) or `./gradlew bootRun --args='--spring.profiles.active=demo'`.
  A command-line argument wins over the environment variable.

Without direnv: `nix develop` opens the same shell, and `nix develop -c ./gradlew test` runs one command
inside it (useful in CI).

## How it works

- **Nix** builds every package into an immutable, hash-addressed path such as
  `/nix/store/xr0h…-zulu-ca-jdk-21.0.11`. Different projects can use different JDKs side by side without
  conflicts.
- A **flake** is a Nix project with declared `inputs` (here nixpkgs and flake-utils) and `outputs` (here
  `devShells.<system>.default`). `flake-utils.eachDefaultSystem` produces the shell for macOS and Linux,
  on both ARM and x86.
- `flake.lock` records the commit hash of each input. Running `nix flake update` moves to newer packages;
  review the change like any dependency upgrade.
- **direnv** runs `.envrc` on `cd` and exports the resulting environment into your current shell.
  **nix-direnv** (installed on this machine) caches the evaluation, so later `cd`s are instant and the
  shell's store paths are protected from garbage collection.

## Common tasks

| Task | How |
|---|---|
| Add a tool (e.g. `docker-compose`) | add `pkgs.docker-compose` to `packages` in `flake.nix`; direnv reloads on the next prompt |
| Upgrade all tools | `nix flake update`, then `./gradlew test` |
| See where the JDK lives | `echo $JAVA_HOME` |
| Point IntelliJ at the same JDK | Project Structure → SDK → Add JDK → paste `$JAVA_HOME` |
| Force a reload | `direnv reload` |
| direnv refuses to load | `direnv allow` (needed after every `.envrc` change, as a security measure) |

## Why the Gradle wrapper is still there

`./gradlew` pins Gradle 8.14.4 for people and CI without Nix. Inside the dev shell both `gradle` (from Nix)
and `./gradlew` work. Prefer `./gradlew`, so that everyone runs exactly the same Gradle.

Note: Gradle 8.14 cannot **run** on JDK 25 or newer. The Nix shell avoids this by
providing JDK 21. Outside Nix, use a JDK 17–24 to launch Gradle.

## Editor setup and formatting

Formatting rules are kept in editor-neutral files, so every editor produces the same result:

| File | Applies to | Rules |
|---|---|---|
| `.editorconfig` | all files, in Neovim (built in), IntelliJ IDEA and VS Code | UTF-8, LF, final newline, no trailing whitespace, 4 spaces (2 for YAML/JSON/Nix/Lua), 120 columns |
| `config/eclipse-formatter.xml` | Java, through jdtls (Neovim, VS Code) or the IntelliJ "Eclipse Code Formatter" plugin | 4 spaces, 120 columns, 8-space continuation indent, keeps hand-made line breaks |

**Neovim (LazyVim):** `.lazy.lua` is the project file. lazy.nvim loads it automatically and asks once
to trust it (`:trust`). It configures jdtls:
- the formatter profile above;
- import order (other packages, `javax`, `java`, static) with no wildcard imports;
- null analysis for the JSpecify annotations;
- Gradle import with the project's JDK 21.

It also shows a ruler at column 121 for Java and Groovy, and turns off format-on-save for Markdown.
Start Neovim from inside the dev shell, so `JAVA_HOME` points at JDK 21.

**Lombok:** the application and server modules use Lombok. LazyVim's Java extra starts jdtls with the
Lombok agent automatically. In IntelliJ, enable annotation processing (the Lombok plugin is bundled).
