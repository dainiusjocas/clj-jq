(ns ^{:doc    "Internal implementation details."
      :no-doc true}
  jq.api.api-impl
  (:require [clojure.string :as str])
  (:import (java.util ArrayList)
           (net.thisptr.jackson.jq JsonQuery Versions Scope BuiltinFunctionLoader Output)
           (com.fasterxml.jackson.databind ObjectMapper JsonNode)
           (com.fasterxml.jackson.databind.node ArrayNode JsonNodeFactory)
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

(defn scope-with-vars ^Scope [^Scope old-scope vars]
  (if vars
    (let [scope (Scope/newChildScope old-scope)]
      (doseq [[key value] vars]
        (.setValue scope (name key) (->JsonNode value)))
      scope)
    old-scope))

(defn string->json-node ^JsonNode [^String data]
  (.readTree mapper data))

(defn json-node->string ^String [^JsonNode data]
  (.writeValueAsString mapper data))

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

(defn apply-json-query-on-json-node
  "Given a JSON data string and a JsonQuery object applies the query
  on the JSON data string and return JsonNode; may be given a custom IContainer"
  ([^JsonNode json-node ^JsonQuery json-query ^Scope scope ^IContainer container]
   (.apply json-query (Scope/newChildScope scope) json-node container)
   (.getValue container))
  ([^JsonNode json-node ^JsonQuery json-query ^Scope scope]
   (apply-json-query-on-json-node json-node json-query scope (json-node-output-container))))

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
     (when variables
       (doseq [[key value] variables]
        (.setValue scope (name key) (->JsonNode value))))
     scope)))

(defn compile-query
  "Compiles a JQ query string into a JsonQuery object."
  ^JsonQuery [^String query]
  (JsonQuery/compile query jq-version))
