{
  description = "Business Partner Onboarding Service (Java 21 / Spring Boot 3 / Gradle)";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };

        # Single source of truth for the JDK: everything below (Gradle, JAVA_HOME, IDE) uses it.
        jdk = pkgs.jdk21;
        gradle = pkgs.gradle.override { java = jdk; };
      in
      {
        devShells.default = pkgs.mkShell {
          packages = [
            jdk
            gradle
            pkgs.git
            pkgs.curl
            pkgs.jq      # pretty-print API responses
            pkgs.httpie  # friendlier alternative to curl for trying the API
          ];

          JAVA_HOME = "${jdk.home}";
          # `./gradlew bootRun` inside this shell runs with the dev profile (application-dev.yml).
          # The Gradle test task removes this variable again, so tests never see it.
          SPRING_PROFILES_ACTIVE = "dev";

          shellHook = ''
            # Keep Gradle caches inside the project so builds do not depend on ~/.gradle state.
            export GRADLE_USER_HOME="$PWD/.gradle-home"
            echo "dev shell: $(java -version 2>&1 | head -1), Gradle ${gradle.version}, Spring profile: $SPRING_PROFILES_ACTIVE"
          '';
        };

        formatter = pkgs.nixpkgs-fmt;
      });
}
