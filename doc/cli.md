# CLI utility

`clj-jq`: `jackson-jq` based command-line JSON processor.

## Installation

For MacOS and linux use `brew`:
```shell
brew install dainiusjocas/brew/clj_jq
```

Upgrade:

```shell
brew upgrade clj-jq
```

Or download the latest binaries yourself from [Github release page](https://github.com/dainiusjocas/clj-jq/releases).

In case you're running MacOS then give run permissions for the executable binary:
```shell
sudo xattr -r -d com.apple.quarantine clj-jq
```

## Options

```text
clj-jq 1.1.0
jackson-jq based command-line JSON processor
Usage: clj-jq [options] jq-filter [file...]
Supported options:
  -h, --help
```

## Examples

```shell
echo "[1,2,3,4]" | ./clj-jq '
.
| map(.+1)
| reverse
'
#=> [5,4,3,2]
```
