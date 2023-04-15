(ns jq.cli
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [jq.api :as jq])
  (:import (java.io Reader BufferedReader)))

(def cli-options
  [["-h" "--help"]])

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

(defn execute [jq-filter files _]
  (let [jq-processor (jq/flexible-processor jq-filter)]
    (if (seq files)
      (doseq [f files
              item (jq-processor (slurp f))]
        (println (jq/json-node->string item)))
      (when (.ready ^Reader *in*)
        (doseq [^String line (line-seq (BufferedReader. *in*))]
          (let [item (jq-processor line)]
            (when-not (str/blank? item)
              (println item))))))))

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
