{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/release-22.11";
    flake-utils.url = "github:numtide/flake-utils";
    clj-nix.url = "github:jlesquembre/clj-nix";
    clj-nix.inputs.nixpkgs.follows = "nixpkgs";
    clj-nix.inputs.flake-utils.follows = "flake-utils";
  };
  outputs = {self, clj-nix, nixpkgs, flake-utils}: flake-utils.lib.eachDefaultSystem (system:
  let
    pkgs = import nixpkgs { inherit system; };
    cljpkgs = clj-nix.packages."${system}";
  in {
    legacyPackages = pkgs;
    packages = rec {
      default = jq-cli;
      jq-cli = let
        buildInitClasses = [
          "com.fasterxml.jackson"
          "net.thisptr.jackson.jq"
        ];
      in cljpkgs.mkGraalBin {
        cljDrv = jq-cli-java;
        extraNativeImageBuildArgs = map (class: "--initialize-at-build-time=${class}") buildInitClasses ++ [ ];
      };
      jq-cli-java = cljpkgs.mkCljBin {
        projectSrc = ./.;
        name = "lt.jocas/clj-jq";
        version = builtins.readFile ./resources/CLJ_JQ_VERSION;
        main-ns = "jq.cli";
        jdkRunner = pkgs.graalvm17-ce;
        buildCommand = "clojure -T:build uber";
      };
    };
    devShells = {
      default = pkgs.mkShell {
        buildInputs = with pkgs; [ clojure graalvm17-ce ] ++ [ cljpkgs.deps-lock ];
        shellHook = ''
          PS1='[clj-jq] '"$PS1"
        '';
      };
    };
  });
}
