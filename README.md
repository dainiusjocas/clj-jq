# clj-jq

A library to execute `jq` scripts on JSON data. It is a thin wrapper around [jackson-jq](https://github.com/eiiches/jackson-jq).

Available `jq` functions can be found [here](https://github.com/eiiches/jackson-jq#implementation-status-and-current-limitations).

## Alternatives

There is another `jq` library for Clojure: [clj-jq](https://github.com/BrianMWest/clj-jq). 
This library works by shelling-out to an embedded `jq`. The problem is that it has costs for
every invocation. Also, it creates difficulties to use this library with the GraalVM native-image.

## Use cases

The library intends to be used for stream processing.

### Compiling a script to execute it multiple times

```clojure
(require '[jq.core :as jq])

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

### One of script execution

```clojure
(let [data "[1,2,3]"
      query "map(.+1)"]
  (jq/execute data query))
=> "[2,3,4]"
```

## Performance

```clojure
(use 'criterium.core)
(let [jq-script (time (jq.core/processor ".a[] |= sqrt"))] 
  (quick-bench (jq-script "{\"a\":[10,2,3,4,5],\"b\":\"hello\"}")))
=>
"Elapsed time: 0.098731 msecs"
Evaluation count : 203586 in 6 samples of 33931 calls.
Execution time mean : 3.729388 µs
Execution time std-deviation : 490.874949 ns
Execution time lower quantile : 2.960691 µs ( 2.5%)
Execution time upper quantile : 4.082989 µs (97.5%)
Overhead used : 1.977144 ns
```

## Future work

- [ ] Expose interface to provide custom function

## License

Copyright &copy; 2021 [Dainius Jocas](https://www.jocas.lt).

Distributed under The Apache License, Version 2.0.
