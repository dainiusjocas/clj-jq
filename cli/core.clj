(ns core
  (:require [clojure.tools.cli :as cli]
            [jq.core :as jq])
  (:import (java.io Reader BufferedReader)))

(def cli-options
  [["-h" "--help"]])

(defn handle-args [args]
  (cli/parse-opts args cli-options))

(defn print-summary-msg [summary]
  (println "jackson-jq based command-line JSON processor")
  (println "Usage: clj-jq [options] jq-filter [file...]")
  (println "Supported options:")
  (println summary))

(defn execute [jq-filter files options]
  (let [jq-processor (jq/processor jq-filter)]
    (if (seq files)
      (doseq [f files]
        (println (jq-processor (slurp f))))
      (when (.ready ^Reader *in*)
        (doseq [line (line-seq (BufferedReader. *in*))]
          (println (jq-processor line)))))))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]
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
