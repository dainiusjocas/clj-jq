(ns ^{:doc "Internal implementation details."
      :no-doc true}
  jq.api.api-impl
  (:import (net.thisptr.jackson.jq JsonQuery Versions Scope BuiltinFunctionLoader Output)
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

(defn apply-json-query-on-json-node
  "Given a JSON data string and a JsonQuery object applies the query
  on the JSON data string and return JsonNode."
  ^JsonNode [^JsonNode json-node ^JsonQuery json-query]
  (let [output-container (OutputContainer. nil)]
    (.apply json-query (Scope/newChildScope root-scope) json-node output-container)
    (.getValue output-container)))

(defn string->json-node ^JsonNode [^String data]
  (.readTree mapper data))

(defn json-node->string ^String [^JsonNode data]
  (.writeValueAsString mapper data))

(defn ^String apply-json-query-on-string-data
  "Reads data JSON string into a JsonNode and passes to the query executor."
  [^String data ^JsonQuery query]
  (json-node->string (apply-json-query-on-json-node (string->json-node data) query)))

(defn ^String apply-json-query-on-json-node-data
  "Reads data JSON string into a JsonNode and passes to the query executor."
  ^String [^JsonNode data ^JsonQuery query]
  (json-node->string (apply-json-query-on-json-node data query)))

(defn compile-query
  "Compiles a JQ query string into a JsonQuery object."
  ^JsonQuery [^String query]
  (JsonQuery/compile query jq-version))
