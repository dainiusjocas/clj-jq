(ns ^{:doc    "Internal implementation details."
      :no-doc true}
  jq.api.api-impl
  (:require [clojure.string :as str])
  (:import (java.util ArrayList)
           (net.thisptr.jackson.jq JsonQuery Versions Scope BuiltinFunctionLoader Output)
           (com.fasterxml.jackson.databind ObjectMapper JsonNode)
           (com.fasterxml.jackson.databind.node JsonNodeFactory)
           (net.thisptr.jackson.jq.module.loaders ChainedModuleLoader BuiltinModuleLoader FileSystemModuleLoader)
           (net.thisptr.jackson.jq.module ModuleLoader)
           (java.nio.file Path)
           (java.io File)))

(set! *warn-on-reflection* true)

(def ^ObjectMapper mapper (ObjectMapper.))

(def jq-version Versions/JQ_1_6)

(defn ->JsonNode ^JsonNode [data]
  (if (instance? JsonNode data)
    data
    (.valueToTree mapper data)))

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

; Helper interface that specifies a method to get a string value.
(definterface IContainer
  ; net.thisptr.jackson.jq/Output
  (^java.lang.Iterable getValue []))

; Container class helper that implements the net.thisptr.jackson.jq.Output
; interface that enables the class to be used as a callback for JQ and exposes the
; unsynchronized-mutable container field for the result of the JQ transformation.
(deftype SingleOutputContainer [^:unsynchronized-mutable ^JsonNode container]
  Output
  (emit [_ json-node] (set! container json-node))
  IContainer
  (getValue [_] container))

(deftype MultiOutputContainer [^ArrayList container]
  Output
  (emit [_ json-node] (.add container json-node))
  IContainer
  (getValue [_]
    (let [jsonArray (.arrayNode JsonNodeFactory/instance (.size container))]
      (doseq [^JsonNode item container]
        (.add jsonArray item))
      (.clear container)
      jsonArray)))

(defn NewMultiOutputContainer []
  (MultiOutputContainer. (ArrayList.)))

(defn apply-json-query-on-json-node
  "Given a JSON data string and a JsonQuery object applies the query
  on the JSON data string and return JsonNode; may be given a custom IContainer"
  (^JsonNode [^JsonNode json-node ^JsonQuery json-query ^Scope scope ^IContainer output-container]
    (let [^IContainer output-container (or output-container (NewMultiOutputContainer))]
      (.apply json-query (Scope/newChildScope scope) json-node output-container)
      (.getValue output-container)))
  (^JsonNode [^JsonNode json-node ^JsonQuery json-query ^Scope scope]
    (apply-json-query-on-json-node json-node json-query scope nil)))

(defn string->json-node ^JsonNode [^String data]
  (.readTree mapper data))

(defn json-node->string ^String [^JsonNode data]
  (.writeValueAsString mapper data))

(defn apply-json-query-on-string-data
  "Reads data JSON string into a JsonNode and passes to the query executor."
  ^String [^String data ^JsonQuery query ^Scope scope]
  (let [stream (-> (string->json-node data)
                   (apply-json-query-on-json-node query scope (NewMultiOutputContainer)))]
    (str/join "\n" (map json-node->string stream))))

(defn apply-json-query-on-json-node-data
  "Passes data in JsonNode to the query executor."
  ^String [^JsonNode data ^JsonQuery query ^Scope scope ^IContainer output-container]
  (let [stream (apply-json-query-on-json-node data query scope output-container)]
    (str/join "\n" (map json-node->string stream))))

(defn new-scope
  (^Scope [] (Scope/newChildScope root-scope))
  (^Scope [opts]
   (let [^Scope scope (new-scope)
         module-paths (get opts :modules)
         module-paths (if (string? module-paths) [module-paths] module-paths)
         variables (get opts :vars)]
     (when (seq module-paths)
       (setup-modules! scope module-paths))
     (when variables
       (doseq [[key value] variables]
        (.setValue scope (name key) (->JsonNode value))))
     scope)))

(defn compile-query
  "Compiles a JQ query string into a JsonQuery object."
  ^JsonQuery [^String query]
  (JsonQuery/compile query jq-version))
