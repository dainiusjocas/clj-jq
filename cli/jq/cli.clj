(ns jq.cli
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [jq.transducers :as jq])
  (:import (clojure.lang IReduceInit)
           (java.io Reader BufferedReader)))

(def cli-options
  [["-c" "--[no-]compact" "compact instead of pretty-printed output." :default false]
   ["-h" "--help"]])

(defn handle-args [args]
  (cli/parse-opts args cli-options))

(def version
  (str/trim
    (slurp (io/resource "CLJ_JQ_VERSION"))))

(defn print-summary-msg [summary]
  (println (format "clj-jq %s" version))
  (println "jackson-jq based command-line JSON processor")
  (println "Usage: clj-jq [options] jq-filter [file...]")
  (println "Supported options:")
  (println summary))

(defn lines-from-stdin
  "Reads lines from the stdin and pushes them further."
  []
  (let [^BufferedReader rdr (BufferedReader. *in*)]
    (reify IReduceInit
      (reduce [_ f init]
        (try
          (loop [state init]
            (if (reduced? state)
              state
              (if-let [line (.readLine rdr)]
                (recur (f state line))
                state)))
          (finally (.close rdr)))))))

(defn printer
  ([_])
  ([_ item] (println item)))

(defn execute [jq-expression files opts]
  (let [xfs [(when (seq files) (map slurp))
             (jq/parse)
             (jq/execute jq-expression)
             (if (:compact opts) (jq/serialize) (jq/pretty-print))]
        xf (apply comp (remove nil? xfs))
        values (or (seq files) (when (.ready ^Reader *in*) (lines-from-stdin)))]
    (transduce xf printer nil values)))

(defn -main [& args]
  (let [{:keys               [options arguments errors summary]
         [jq-filter & files] :arguments} (handle-args args)]
    (when (seq errors)
      (println "Errors:" errors)
      (print-summary-msg summary)
      (System/exit 1))
    (when (or (:help options) (zero? (count arguments)))
      (print-summary-msg summary)
      (if (:help options)
        (System/exit 0)
        (System/exit 1)))
    (execute jq-filter files options))
  (System/exit 0))
