(ns jq.core
  (:import (net.thisptr.jackson.jq JsonQuery Scope Versions Output BuiltinFunctionLoader)
           (com.fasterxml.jackson.databind ObjectMapper JsonNode)))

(set! *warn-on-reflection* true)

(def ^ObjectMapper mapper (ObjectMapper.))

(def jq-version Versions/JQ_1_6)

(def ^Scope root-scope
  "Scope that contains all the available Builtin functions."
  (let [scope (Scope/newEmptyScope)]
    ; Load all the functions available for the JQ-1.6
    (.loadFunctions (BuiltinFunctionLoader/getInstance) jq-version scope)
    scope))

; Helper interface that specifies a method to get a string value.
(definterface IGetter
  (^com.fasterxml.jackson.databind.JsonNode getValue []))

; Container class helper that implements the net.thisptr.jackson.jq.Output
; interface that enables the class to be used as a callback for JQ and exposes the
; unsynchronized-mutable container field for the result of the JQ transformation.
(deftype OutputContainer [^:unsynchronized-mutable ^JsonNode container]
  Output
  (emit [_ json-node] (set! container json-node))
  IGetter
  (getValue [_] container))

(defn ^JsonQuery compile-query
  "Compiles a JQ query string into a JsonQuery object."
  [^String query]
  (JsonQuery/compile query jq-version))

(defn query-json-node
  "Given a JSON data string and a JsonQuery object applies the query
  on the JSON data string and return the resulting JSON string."
  [^JsonNode data ^JsonQuery query]
  (let [output-container (OutputContainer. nil)]
    (.apply query (Scope/newChildScope root-scope) data output-container)
    (.writeValueAsString mapper ^JsonNode (.getValue output-container))))

(defn ^String query-data
  "Reads data JSON string into a JsonNode and passes to the query executor."
  [^String data ^JsonQuery query]
  (query-json-node (.readTree mapper data) query))

; jq docs http://manpages.ubuntu.com/manpages/hirsute/man1/jq.1.html
(defn execute
  "Given a JSON data string (1) and a JQ query string (2)
  returns a JSON string result of (2) applied on (1)."
  [^String data ^String query]
  (query-data data (compile-query query)))

(defn json-node-processor
  "Given a JQ query string (2) compiles it and returns a function that given
  a JsonNode object (2) will return a JSON string with (1) applied on (2)."
  [^String query]
  (let [^JsonQuery query (compile-query query)]
    (fn [^JsonNode data]
      (query-json-node data query))))

(defn processor
  "Given a JQ query string (2) compiles it and returns a function that given
  a JSON string (2) will return a JSON string with (1) applied on (2)."
  [^String query]
  (let [^JsonQuery query (compile-query query)]
    (fn [^String data]
      (query-data data query))))

(comment
  (time (jq.core/execute "{\"a\":[1,2,3,4,5],\"b\":\"hello\"}" "."))

  (time (jq.core/execute "{\"a\":[10,2,3,4,5],\"b\":\"hello\"}" ".a[] |= sqrt"))
  (time (jq.core/execute "{\"a\":[10,2,3,4,5],\"b\":\"hello\"}" ".a | length"))

  (time (jq.core/execute "{\"a\":[10,2,3,4,5],\"b\":\"hello\"}" ".a | .[0] + .[1]"))
  (time (jq.core/execute "[[1,2],[10,20]]"
                           "def addvalue(f): . + [f]; map(addvalue(.[0]))"))

  (time (jq.core/execute "[1,2,3]" "map(.+1)"))

  ;(quick-bench (jq.core/execute "{\"a\":[10,2,3,4,5],\"b\":\"hello\"}" ".a | join(\"-\")"))


  (dotimes [_ 100] (time (jq.core/execute "{\"a\":[1,2,3,4,5],\"b\":\"hello\"}" ".a"))))
