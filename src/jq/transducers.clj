(ns jq.transducers
  (:require [jq.api :as api]
            [jq.api.api-impl :as impl])
  (:import (com.fasterxml.jackson.databind ObjectMapper SerializationFeature)
           (java.io Reader)
           (java.util Iterator)))

(defn ->JsonNode
  "Returns a transducer that given a Java object maps it to a JsonNode.
  Accepts an optional Jackson ObjectMapper."
  ([] (map impl/->JsonNode))
  ([^ObjectMapper mapper]
   (map (partial impl/->JsonNode mapper))))

(defn JsonNode->value
  "Returns a transducer that give a JsonNode maps it to a Java Object.
  Accepts an optional Jackson ObjectMapper."
  ([] (map impl/JsonNode->clj))
  ([^ObjectMapper mapper]
   (map (partial impl/JsonNode->clj mapper))))

(defn parse
  "Returns a transducer that given a JSON String parses it into a JsonNode.
  Accepts an optional Jackson ObjectMapper."
  ([] (map impl/string->json-node))
  ([^ObjectMapper mapper]
   (map (partial impl/string->json-node mapper))))

(defn rdr->json-nodes
  "Returns a transducer that given a java.io.Reader parses it into a stream of JsonNode.
  Accepts an optional Jackson ObjectMapper."
  ([] (rdr->json-nodes impl/mapper))
  ([^ObjectMapper mapper]
   (fn [rf]
     (fn transducer
       ([] (rf))
       ([acc] (rf acc))
       ([acc ^Reader rdr]
        (loop [^Iterator iter (impl/rdr->json-node-iter mapper rdr)]
          (if (.hasNext iter)
            (do
              (rf acc (.next iter))
              (recur iter))
            acc)))))))

(comment
  (sequence (rdr->json-nodes) [(java.io.StringReader. "\"hello\" \"world\"")]))

(defn serialize
  "Returns a transducer that given a JsonNode serializes it to a String.
  Accepts an optional Jackson ObjectMapper."
  ([] (map impl/json-node->string))
  ([^ObjectMapper mapper]
   (map (partial impl/json-node->string mapper))))

;; Pretty printer serializer
(defn pretty-print
  "Same as the `serializer` but the output string is indented.
  ObjectMapper is copied to prevent side effects in case mapper is shared."
  ([] (pretty-print impl/mapper))
  ([^ObjectMapper mapper]
   (map (partial impl/json-node->string
                 (.enable (.copy mapper) SerializationFeature/INDENT_OUTPUT)))))

(defn execute
  "Returns a transducer that accepts JsonNode on which
  the expression will be applied.
  Optional opts are supported that are passed for the `jq.api/stream-processor`.
  Specific opts for the transducer:
    :cat - whether to catenate output, default true"
  ([^String expression] (execute expression {}))
  ([^String expression opts]
   (if (false? (:cat opts))
     (map (api/stream-processor expression opts))
     (impl/fast-transducer expression opts))))

(defn process
  "Returns a convenience transducer that:
  - maps a Java Object to a JsonNode;
  - maps a JQ expression on the JsonNode;
  - catenates the output;
  - maps a JsonNode to a JavaObject.
  Accept opts that will be passed to the `jq.api/stream-processor`
  Accepts a Jackson ObjectMapper that will be used for both"
  ([^String expression] (process expression {}))
  ([^String expression opts] (process expression opts impl/mapper))
  ([^String expression opts ^ObjectMapper mapper]
   (comp
     (->JsonNode mapper)
     (execute expression (assoc opts :cat true))
     (JsonNode->value mapper))))

(comment
  ; Duplicates input
  (into []
        (comp
          (->JsonNode)
          (execute "(. , .)")
          (JsonNode->value))
        [1 2 3])

  (into [] (process "(. , .)") [1 2 3]))
