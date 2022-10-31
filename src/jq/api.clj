(ns jq.api
  (:require [jq.api.api-impl :as impl])
  (:import (net.thisptr.jackson.jq JsonQuery Scope)))

(set! *warn-on-reflection* true)

; jq docs http://manpages.ubuntu.com/manpages/hirsute/man1/jq.1.html
(defn execute
  "Given a JSON data string (1) and a JQ query string (2)
  returns a JSON string result of (2) applied on (1).
  Accepts optional options map.
  NOTE: if your query doesn't change the use the `processor`."
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
  Accepts optional options map."
  ([^String query] (processor query {}))
  ([^String query opts]
   (let [^JsonQuery json-query (impl/compile-query query)
         ^Scope scope (impl/new-scope opts)]
     (fn ^String [^String data]
       (impl/apply-json-query-on-string-data data json-query scope)))))

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
     (fn [json-data]
       (cond
         ; string => string
         (and (string? json-data) (= :string output-format))
         (impl/apply-json-query-on-string-data json-data query scope)

         ; string => json-node
         (and (string? json-data) (not= :string output-format))
         (impl/apply-json-query-on-json-node (impl/string->json-node json-data) query scope)

         ; json-node => string
         (and (not (string? json-data)) (= :string output-format))
         (impl/apply-json-query-on-json-node-data json-data query scope)

         ; json-node => json-node
         (and (not (string? json-data)) (not= :string output-format))
         (impl/apply-json-query-on-json-node json-data query scope))))))

(comment
  (jq.api/execute "{\"a\":[1,2,3,4,5],\"b\":\"hello\"}" ".")

  ((jq.api/processor ".") "{\"a\":[1,2,3,4,5],\"b\":\"hello\"}")

  ((jq.api/flexible-processor "." {:module-paths ["test/resources"]}) "{\"a\":[1,2,3,4,5],\"b\":\"hello\"}")

  ((jq.api/flexible-processor "." {:output :json-node}) "{\"a\":[1,2,3,4,5],\"b\":\"hello\"}"))
