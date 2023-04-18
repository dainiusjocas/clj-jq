(ns jq.transducers
  (:require [jq.api :as api]
            [jq.api.api-impl :as impl])
  (:import (com.fasterxml.jackson.databind ObjectMapper)))

(defn ->JsonNode
  "Returns a transducer that given a Java object maps it to a JsonNode.
  Accepts an optional Jackson ObjectMapper."
  ([] (map impl/->JsonNode))
  ([^ObjectMapper mapper]
   (map (partial impl/->JsonNode mapper))))

(defn JsonNode->clj
  "Returns a transducer that give a JsonNode maps it to a Java Object.
  Accepts an optional Jackson ObjectMapper"
  ([] (map impl/JsonNode->clj))
  ([^ObjectMapper mapper]
   (map (partial impl/JsonNode->clj mapper))))

(defn search
  "Returns a transducer that accepts JsonNode on which
  the query will be applied.
  Optional opts are supported that are passed for the `jq.api/stream-processor`.
  Specific opts for the transducer:
    :cat - whether to catenate output, default true"
  ([^String query] (search query {}))
  ([^String query opts]
   (let [xf (map (api/stream-processor query opts))]
     (if (false? (:cat opts))
       xf
       (comp xf cat)))))

(defn process
  "Convenience function.
  Returns a transducer that:
  - maps a Java Object to a JsonNode
  - maps a JQ query on the JsonNode
  - catenates the output
  - maps a JsonNode to a JavaObject.
  Accept opts that will be passed to the `jq.api/stream-processor`
  Accepts a Jackson ObjectMapper that will be used for both"
  ([^String query] (process query {}))
  ([^String query opts] (process query opts impl/mapper))
  ([^String query opts ^ObjectMapper mapper]
   (comp
     (->JsonNode mapper)
     (search query (assoc opts :cat true))
     (JsonNode->clj mapper))))

(comment
  ; Duplicates input
  (into []
        (comp
          (->JsonNode)
          (search "(. , .)")
          (JsonNode->clj))
        [1 2 3])

  (into [] (process "(. , .)") [1 2 3]))
