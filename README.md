[![Clojars Project](https://img.shields.io/clojars/v/lt.jocas/clj-jq.svg)](https://clojars.org/lt.jocas/clj-jq)
[![cljdoc badge](https://cljdoc.org/badge/lt.jocas/clj-jq)](https://cljdoc.org/d/lt.jocas/clj-jq/CURRENT)
[![Tests](https://github.com/dainiusjocas/clj-jq/actions/workflows/test.yml/badge.svg)](https://github.com/dainiusjocas/clj-jq/actions/workflows/test.yml)

# clj-jq

A library to execute [`jq`](https://stedolan.github.io/jq/) expressions on JSON data within a Clojure application.
It is a thin wrapper around [jackson-jq](https://github.com/eiiches/jackson-jq):
a pure Java `jq` Implementation for Jackson JSON Processor.

Available `jq` functions can be found [here](https://github.com/eiiches/jackson-jq#implementation-status-and-current-limitations).

This library is compatible with the GraalVM `native-image`.

## Alternatives

There is another `jq` library for Clojure: [clj-jq](https://github.com/BrianMWest/clj-jq). 
This library works by shelling-out to an embedded `jq` instance.
The problem with this approach is that it has fixed costs for every invocation. 
Also, it creates difficulties to use this library with the GraalVM native-image.

## Use cases

The library is intended to be used for stream processing.

```clojure
(require '[jq.api :as jq])

(let [data "[1,2,3]"
      expression "map(.+1)"
      processor-fn (jq/stream-processor expression)]
  (processor-fn (jq/string->json-node data)))
=> [#object[com.fasterxml.jackson.databind.node.ArrayNode 0x8b78e9e "[2,3,4]"]]
```

NOTE: the input is of the `JsonNode` type while the output ßis a `Collection` of `JsonNode`s.

### Loading JQ modules from the filesystem

File with contents:
```shell
; Prepare a module files
cat /path/to/script.jq
=> def increment(number): number + 1;
```

```clojure
(let [data "[1,2,3]"
      expression "include \"scripts\"; map(increment(.))"
      processor-fn (jq/processor expression {:modules ["/path/to"]})]
  (processor-fn data))
=> "[2,3,4]"
```

Multiple modules can be provided.

### Variables for the expressions

The library supports both: compile and runtime variables.

```clojure
(let [expression "[$cvar, $rvar]"
      processor-fn (jq/stream-processor expression {:vars {:cvar "compile"}})]
  (processor-fn (jq/string->json-node "null") {:vars {:rvar "run"}}))
=> [#object[com.fasterxml.jackson.databind.node.ArrayNode 0x154d64f4 "[\"compile\",\"run\"]"]]
```

### Inlined example

```clojure
((jq/processor "map(.+1)") "[1,2,3]")
=> "[2,3,4]"
```

### One-off script execution

```clojure
(let [data "[1,2,3]"
      expression "map(.+1)"]
  (jq/execute data expression))
=> "[2,3,4]"
```

## How to join multiple scripts together

Joining `jq` scripts is as simple as "piping" output of one script to another:
join `jq` script strings with `|` character.

```clojure
(let [data "[1,2,3]"
      script-inc "map(.+1)"
      script-reverse "reverse"
      expression (clojure.string/join " | " [script-inc script-reverse])]
  (jq/execute data expression))
=> "[4,3,2]"
```

## Transducer API

[Transducers](https://clojure.org/reference/transducers) are composable algorithmic transformations.
They fit really well with the JQ model: expression produces a sequence of 0 or more JSON [entities](https://github.com/pkoppstein/jq/wiki/A-Stream-oriented-Introduction-to-jq#json-entities-and-json-streams).

```clojure
(require '[jq.transducers :as jq])

; Duplicate every value
(into []
      (comp
        (jq/->JsonNode)
        (jq/execute "(. , .)")
        (jq/JsonNode->value))
      [1 2 3])
=> [1 1 2 2 3 3]

; Several JQ expressions in a row
; NOTE: It is efficient because no serializations/deserializations are done between executions of expressions 
(into []
      (comp
        (jq/->JsonNode)
        (jq/execute ". + 1")
        (jq/execute ". + 1")
        (jq/JsonNode->value))
      [1 2 3])
=> [3 4 5]

; JSON string to JSON string
(sequence (comp
            (jq/parse)
            (jq/execute ".foo |= 2")
            (jq/serialize))
          ["{\"foo\":1}"])
=> '("{\"foo\":2}")

; A convenient "common-use-case" transducer
(into [] (jq/process "(. , .)") [1 2 3])
=>  [1 1 2 2 3 3]
```

A custom Jackson `ObjectMapper` can be provided for most transducers.
E.g. you could take an `ObjectMapper` from the [`jsonista`](https://github.com/metosin/jsonista) library and supply it to transducers.

For available transducers check [here](src/jq/transducers.clj).

## Performance

On MacBook pro M1 Max laptop: 
```clojure
(use 'criterium.core)
(let [jq-script (time (jq/processor ".a[] |= sqrt"))]
  (quick-bench (jq-script "{\"a\":[10,2,3,4,5],\"b\":\"hello\"}")))
=>
"Elapsed time: 0.257709 msecs"
Evaluation count : 334260 in 6 samples of 55710 calls.
Execution time mean : 1.812567 µs
Execution time std-deviation : 24.996676 ns
Execution time lower quantile : 1.788598 µs ( 2.5%)
Execution time upper quantile : 1.849297 µs (97.5%)
Overhead used : 1.840854 ns
```

## Future work

- [ ] Expose interface to provide custom function, like [here](https://github.com/quarkiverse/quarkus-jackson-jq)

## License

Copyright &copy; 2022 [Dainius Jocas](https://www.jocas.lt).

Distributed under The Apache License, Version 2.0.
