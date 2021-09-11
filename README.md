[![Clojars Project](https://img.shields.io/clojars/v/lt.jocas/clj-jq.svg)](https://clojars.org/lt.jocas/clj-jq)
[![cljdoc badge](https://cljdoc.org/badge/lt.jocas/clj-jq)](https://cljdoc.org/d/lt.jocas/clj-jq/CURRENT)
[![Tests](https://github.com/dainiusjocas/clj-jq/actions/workflows/test.yml/badge.svg)](https://github.com/dainiusjocas/clj-jq/actions/workflows/test.yml)

# clj-jq

A library to execute `jq` scripts on JSON data.
It is a thin wrapper around [jackson-jq](https://github.com/eiiches/jackson-jq).

Available `jq` functions can be found [here](https://github.com/eiiches/jackson-jq#implementation-status-and-current-limitations).

## Alternatives

There is another `jq` library for Clojure: [clj-jq](https://github.com/BrianMWest/clj-jq). 
This library works by shelling-out to an embedded `jq` instance.
The problem with this approach is that it has fixed costs for every invocation. 
Also, it creates difficulties to use this library with the GraalVM native-image.

## Use cases

The library intends to be used for stream processing.

### Compiling a script to execute it multiple times

```clojure
(require '[jq.api :as jq])

(let [data "[1,2,3]"
      query "map(.+1)"
      processor-fn (jq/processor query)]
  (processor-fn data))
=> "[2,3,4]"
```

Or inline:

```clojure
((jq/processor "map(.+1)") "[1,2,3]")
=> "[2,3,4]"
```

### One-off script execution

```clojure
(let [data "[1,2,3]"
      query "map(.+1)"]
  (jq/execute data query))
=> "[2,3,4]"
```

## How to join multiple scripts together

Joining jq scripts is as simple as "piping" output of one script to another:
join jq script strings with `|` character.

```clojure
(let [data "[1,2,3]"
      script-inc "map(.+1)"
      script-reverse "reverse"
      query (clojure.string/join " | " [script-inc script-reverse])]
  (jq/execute data query))
=> "[4,3,2]"
```

## Performance

```clojure
(use 'criterium.core)
(let [jq-script (time (jq/processor ".a[] |= sqrt"))]
  (quick-bench (jq-script "{\"a\":[10,2,3,4,5],\"b\":\"hello\"}")))
=>
"Elapsed time: 0.063264 msecs"
Evaluation count : 198870 in 6 samples of 33145 calls.
Execution time mean : 3.687955 µs
Execution time std-deviation : 668.209042 ns
Execution time lower quantile : 3.041275 µs ( 2.5%)
Execution time upper quantile : 4.280444 µs (97.5%)
Overhead used : 1.766661 ns
```

## Future work

- [ ] Expose interface to provide custom function

## License

Copyright &copy; 2021 [Dainius Jocas](https://www.jocas.lt).

Distributed under The Apache License, Version 2.0.
