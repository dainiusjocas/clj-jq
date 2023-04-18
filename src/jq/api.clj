(ns jq.api
  (:require [jq.api.api-impl :as impl])
  (:import (com.fasterxml.jackson.databind JsonNode)
           (java.util Collection)
           (net.thisptr.jackson.jq JsonQuery Scope)))

(set! *warn-on-reflection* true)

; required in common use cases, so moving out of impl space
(def json-node->string impl/json-node->string)
(def string->json-node impl/string->json-node)

; jq docs http://manpages.ubuntu.com/manpages/hirsute/man1/jq.1.html
(defn execute
  "Given a JSON data string (1) and a JQ query string (2)
  returns a JSON string result of (2) applied on (1).
  Output stream is joined with the new-line symbol.
  Accepts optional options map.
  NOTE: if your query doesn't change then use the `processor`."
  (^String [^String data ^String query]
   (execute data query {}))
  (^String [^String data ^String query opts]
   (impl/apply-json-query-on-string-data
     data
     (impl/compile-query query)
     (impl/new-scope opts))))

(defn processor
  "Given a JQ query string (1) compiles it and returns a function that given
  a JSON string (2) will return a JSON string with (1) applied on (2).
  Output stream is joined with the new-line symbol.
  Accepts optional options map."
  ([^String query] (processor query {}))
  ([^String query opts]
   (let [^JsonQuery json-query (impl/compile-query query)
         ^Scope scope (impl/new-scope opts)]
     (fn this
       (^String [^String data]
         (this data nil))
       (^String [^String data {:keys [vars]}]
         (impl/apply-json-query-on-string-data data json-query (impl/scope-with-vars scope vars)))))))

(defn flexible-processor
  "Given a JQ query string (1) compiles it and returns a function that given
  a JsonNode object or a String (2) will return
  either a JSON string or json node with (1) applied on (2).
  Accepts optional options map."
  ([^String query] (flexible-processor query {}))
  ([^String query opts]
   (let [^JsonQuery query (impl/compile-query query)
         output-format (get opts :output :string)
         ^Scope scope (impl/new-scope opts)]
     (fn this
       ([json-data]
        (this json-data nil))
       ([json-data {:keys [vars]}]
        (let [^Scope call-scope (if vars (impl/scope-with-vars scope vars) scope)]
          (cond
            ; string => string
            (and (string? json-data) (= :string output-format))
            (impl/apply-json-query-on-string-data json-data query call-scope)

            ; string => json-node
            (and (string? json-data) (not= :string output-format))
            (impl/apply-json-query-on-json-node (impl/string->json-node json-data) query call-scope)

            ; json-node => string
            (and (not (string? json-data)) (= :string output-format))
            (impl/apply-json-query-on-json-node-data json-data query call-scope)

            ; json-node => json-node
            (and (not (string? json-data)) (not= :string output-format))
            (impl/apply-json-query-on-json-node json-data query call-scope))))))))

(defn stream-processor
  "Given a JQ query string (1) compiles it and returns a function that given
  a JsonNode object will return an Collection.
  Returning an `Collection` handles that a JQ script returns 0 or more JSON entities.
  Accepts optional options map."
  ([^String query] (stream-processor query {}))
  ([^String query opts]
   (let [^JsonQuery query (impl/compile-query query)
         ^Scope scope (impl/new-scope opts)]
     (fn this
       (^Collection [^JsonNode data] (this data nil))
       (^Collection [^JsonNode data {:keys [vars]}]
        (let [^Scope call-scope (if vars (impl/scope-with-vars scope vars) scope)]
          (impl/stream-of-json-entities data query call-scope)))))))

(comment
  (jq.api/execute "{\"a\":[1,2,3,4,5],\"b\":\"hello\"}" ".")

  ((jq.api/processor ".") "{\"a\":[1,2,3,4,5],\"b\":\"hello\"}")

  ((jq.api/flexible-processor "." {:module-paths ["test/resources"]}) "{\"a\":[1,2,3,4,5],\"b\":\"hello\"}")

  ((jq.api/flexible-processor "." {:output :json-node}) "{\"a\":[1,2,3,4,5],\"b\":\"hello\"}")

  (let [data "[1,2,3]"
        expression "map(.+1)"
        processor-fn (stream-processor expression)]
    (processor-fn (string->json-node data)))

  ((jq.api/stream-processor "." {}) (jq.api/string->json-node {"a" "b"})))
