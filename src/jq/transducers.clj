(ns jq.transducers
  (:require [jq.api :as api]
            [jq.api.api-impl :as impl])
  (:import (com.fasterxml.jackson.databind ObjectMapper)))

(defn ->JsonNode
  ([] (map impl/->JsonNode))
  ([^ObjectMapper mapper]
   (map (partial impl/->JsonNode mapper))))

(defn JsonNode->clj
  ([] (map impl/JsonNode->clj))
  ([^ObjectMapper mapper]
   (map (partial impl/JsonNode->clj mapper))))

(defn search [^String query]
  (map (api/stream-processor query)))

(comment
  (into []
        (comp
          (->JsonNode)
          (search "(. , .)")
          cat
          (JsonNode->clj))
        [1 2 3]))
