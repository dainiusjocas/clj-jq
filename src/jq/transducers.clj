(ns jq.transducers
  (:require [jq.api :as api]
            [jq.api.api-impl :as impl]))

(defn ->JsonNode []
  (map impl/->JsonNode))

(defn JsonNode->clj []
  (map impl/JsonNode->clj))

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
