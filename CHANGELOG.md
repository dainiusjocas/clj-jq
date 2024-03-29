# Changelog

For a list of breaking changes, check [here](#breaking-changes).

## Unreleased

- Update GraalVM to 22.3.2
- Update jackson-databind to 2.15.2
- fix: only print the exception message
- add `net.thisptr/jackson-jq-extra` package with useful functions to the CLI tool

## v2.3.0

- Handle vars that are of the `JsonNode` type #53 (issue $52)

## v2.2.0

- Handle multiple JSON entities per line (issue #49) (PR #51)

## v2.1.0

- Avoid creating intermediate collections in transducers #48

## v2.0.0

- Upgrade `jackson-jq` to 1.0.0-preview.20230409 #26
- Allow variables to be passed at the expression compile time #29 (Thanks to @charles-dyfis-net)
- Nix support #34 (Thanks to @charles-dyfis-net)
- Allow JQ variables to be passed at execution time #38 (Thanks to @charles-dyfis-net)
- API to return a stream of JSON entities #41
- Transducers API #42
- CLI: pretty print output by default, flag `-c` for compact #44

## v1.2.1

- Support for loading JQ [modules](https://stedolan.github.io/jq/manual/#Modules) from the filesystem
- Fixed a bug for musl based release #24
- Updated `net.thisptr/jackson-jq` to `1.0.0-preview.20220705`
- Updated GraalVM to 22.3.0

## v1.1.3

- Bumped GraalVM to 21.3.0

## v1.1.1

- Bumped dependencies
- GraalVM native image build future-proof

## v1.1.0

## New

- introduce `jq.api` namespace
- deprecate `jq.core` namespace

## Breaking changes

## Unreleased

## v2.0.0

- `flexible-processor` with `{:output :json-node}` always returns `ArrayNode`
- Options with String->String might return new line separated outputs
