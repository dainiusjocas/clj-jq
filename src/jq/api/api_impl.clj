(ns ^{:doc    "Internal implementation details."
      :no-doc true}
  jq.api.api-impl
  (:require [clojure.string :as str])
  (:import (java.util ArrayList Iterator Map$Entry)
           (net.thisptr.jackson.jq JsonQuery Versions Scope BuiltinFunctionLoader Output)
           (com.fasterxml.jackson.databind ObjectMapper JsonNode)
           (com.fasterxml.jackson.databind.node ArrayNode JsonNodeFactory ObjectNode)
           (net.thisptr.jackson.jq.module.loaders ChainedModuleLoader BuiltinModuleLoader FileSystemModuleLoader)
           (net.thisptr.jackson.jq.module ModuleLoader)
           (java.nio.file Path)
           (java.io File Reader)))

(set! *warn-on-reflection* true)

(def ^ObjectMapper mapper (ObjectMapper.))

(def jq-version Versions/JQ_1_6)

(defn ->JsonNode
  (^JsonNode [data] (->JsonNode mapper data))
  (^JsonNode [^ObjectMapper mapper data]
   (if (instance? JsonNode data)
     data
     (.valueToTree mapper data))))

(defn JsonNode->clj
  "Converts JsonNode to a Clojure value.
  An optional object mapper can be passed to handle data types such as keywords."
  ([^JsonNode json-node] (JsonNode->clj mapper json-node))
  ([^ObjectMapper mapper ^JsonNode json-node]
   (.treeToValue mapper json-node ^Class Object)))

(defn ->absolute-path
  "FileSystemModuleLoader requires absolute paths."
  ^Path [^String file-path]
  (-> file-path
      (File.)
      (.toPath)
      (.toAbsolutePath)))

(defn module-loader
  "Given a scope and a list of file paths as strings creates a ModuleLoader
  that knows how to load JQ modules."
  ^ModuleLoader [^Scope scope file-paths]
  (->> file-paths
       (mapv ->absolute-path)
       (into-array Path)
       (FileSystemModuleLoader. scope jq-version)
       (vector (BuiltinModuleLoader/getInstance))
       (into-array ModuleLoader)
       (ChainedModuleLoader.)))

(defn setup-modules!
  "Modules documentation: https://stedolan.github.io/jq/manual/#Modules"
  [^Scope scope file-paths]
  (.setModuleLoader scope (module-loader scope file-paths)))

(def ^:private ^Scope root-scope
  "Scope that contains all the available Builtin functions.
  Handy for the native image creation, because at build time it loads all the functions.
  WARNING: MUTABLE! USE ONLY TO CREATE NEW SCOPES!"
  (let [scope (Scope/newEmptyScope)]
    ; Load all the functions available for the JQ-1.6
    (.loadFunctions (BuiltinFunctionLoader/getInstance) jq-version scope)
    scope))

(defn put-vars-in-scope [^Scope scope vars]
  (if (instance? ObjectNode vars)
    (let [^Iterator iter (.fields ^ObjectNode vars)]
      (while (.hasNext iter)
        (let [^Map$Entry entry (.next iter)]
          (.setValue scope (.getKey entry) (.getValue entry)))))
    (doseq [[key value] vars]
      (.setValue scope (name key) (->JsonNode value)))))

(defn scope-with-vars ^Scope [^Scope old-scope vars]
  (if vars
    (let [scope (Scope/newChildScope old-scope)]
      (put-vars-in-scope scope vars)
      scope)
    old-scope))

(defn string->json-node
  (^JsonNode [^String data] (string->json-node mapper data))
  (^JsonNode [^ObjectMapper mapper ^String data]
   (.readTree mapper data)))

(defn rdr->json-node-iter
  "Takes in a java.io.Reader and returns an iterator of JsonNode values."
  ([^Reader rdr] (rdr->json-node-iter mapper rdr))
  ([^ObjectMapper mapper ^Reader rdr]
   (.readValues (.readerFor mapper ^Class JsonNode) rdr)))

(comment
  ; Two JSON values encoded in one string produces two values
  (= 2 (-> "\"hello\" \"world\""
           (java.io.StringReader.)
           (rdr->json-node-iter)
           (iterator-seq)
           (count))))

(defn json-node->string
  (^String [^JsonNode data] (json-node->string mapper data))
  (^String [^ObjectMapper mapper ^JsonNode data]
   (.writeValueAsString mapper data)))

; Helper interface that specifies a method to get a string value.
(definterface IContainer
  ; net.thisptr.jackson.jq/Output
  (^java.lang.Iterable getValue []))

; Container class helper that implements the net.thisptr.jackson.jq.Output
; interface that enables the class to be used as a callback for JQ and exposes the
; unsynchronized-mutable container field for the result of the JQ transformation.
(deftype JsonNodeOutputContainer [^ArrayNode container]
  Output
  (emit [_ json-node] (.add container json-node))
  IContainer
  (getValue [_] container))

(defn json-node-output-container []
  (JsonNodeOutputContainer. (.arrayNode JsonNodeFactory/instance)))

(deftype StringOutputContainer [^ArrayList container]
  Output
  (emit [_ json-node] (.add container (json-node->string json-node)))
  IContainer
  (getValue [_] container))

(defn string-output-container []
  (StringOutputContainer. (ArrayList.)))

(deftype CollectionOutputContainer [^ArrayList container]
  Output
  (emit [_ json-node] (.add container json-node))
  IContainer
  (getValue [_] container))

(defn collection-output-container []
  (CollectionOutputContainer. (ArrayList.)))

(defn apply-json-query-on-json-node
  "Given a JSON data string and a JsonQuery object applies the query
  on the JSON data string and return JsonNode; may be given a custom IContainer"
  ([^JsonNode json-node ^JsonQuery json-query ^Scope scope ^IContainer container]
   (.apply json-query (Scope/newChildScope scope) json-node container)
   (.getValue container))
  ([^JsonNode json-node ^JsonQuery json-query ^Scope scope]
   (apply-json-query-on-json-node json-node json-query scope (json-node-output-container))))

(defn stream-of-json-entities
  [^JsonNode json-node ^JsonQuery json-query ^Scope scope]
  (apply-json-query-on-json-node json-node json-query scope (collection-output-container)))

(defn apply-json-query-on-json-node-data
  "Passes a JsonNode to the query executor."
  ^String [^JsonNode data ^JsonQuery query ^Scope scope]
  (str/join "\n" (apply-json-query-on-json-node data query scope (string-output-container))))

(defn apply-json-query-on-string-data
  "Reads data JSON string into a JsonNode and passes to the query executor."
  ^String [^String data ^JsonQuery query ^Scope scope]
  (apply-json-query-on-json-node-data (string->json-node data) query scope))

(defn new-scope
  (^Scope [] (Scope/newChildScope root-scope))
  (^Scope [opts]
   (let [^Scope scope (new-scope)
         module-paths (get opts :modules)
         module-paths (if (string? module-paths) [module-paths] module-paths)
         variables (get opts :vars)]
     (when (seq module-paths)
       (setup-modules! scope module-paths))
     (put-vars-in-scope scope variables)
     scope)))

(defn compile-query
  "Compiles a JQ query string into a JsonQuery object."
  ^JsonQuery [^String query]
  (JsonQuery/compile query jq-version))

(definterface OutputWithAcc
  (^net.thisptr.jackson.jq.Output withAcc [acc]))

(deftype XfOutput [rf ^:volatile-mutable acc]
  Output
  ; Pass down the transducers chain
  (emit [_ json-node] (rf acc json-node))
  OutputWithAcc
  ; Set value and return object itself
  (withAcc [this current-acc] (set! acc current-acc) this))

(defn fast-transducer
  "Returns a transducer that avoids creating intermediate collections for JSON entities.
  Doesn't support runtime variables.
  NOTE: if used from multiple threads, the output ordering by input is not guaranteed."
  ([^String expression] (fast-transducer expression {}))
  ([^String expression opts]
   (let [^JsonQuery query (compile-query expression)
         ^Scope scope (new-scope opts)]
     (fn [rf]
       (let [^XfOutput output (XfOutput. rf nil)]
         (fn transducer
           ([] (rf))
           ([acc] (rf acc))
           ([acc ^JsonNode data]
            (.apply query scope data (.withAcc output acc))
            acc)))))))

(comment
  (time (into []
              (comp
                (map ->JsonNode)
                (fast-transducer "(. , .)")
                (map JsonNode->clj))
              [1 2 3])))
