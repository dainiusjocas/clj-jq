(ns jq.api-test
  (:require [clojure.test :refer [deftest is testing]]
            [jq.api :as jq]
            [jq.api.api-impl :as utils])
  (:import (com.fasterxml.jackson.databind JsonNode)))

(def string-data "[1,2,3]")
(def json-node-data (utils/string->json-node string-data))
(def query "map(.+1)")
(def result-string "[2,3,4]")

(deftest variables
  (testing "passing variables in at compile time"
    (let [script "[$var1, $var2, $var3]"
          processor-fn (jq/flexible-processor script {:output :string
                                                      :vars {:var1 "hello"
                                                             "var2" "world"
                                                             :var3 123}})
          resp (processor-fn "null")]
      (is (= resp "[\"hello\",\"world\",123]"))))
  (testing "passing variables in at runtime"
    (let [script "[$cvar, $rvar]"
          processor-fn (jq/flexible-processor script {:output :string
                                                      :vars {:cvar "compile"}})
          resp (processor-fn "null" {:vars {:rvar "run"}})]
      (is (= resp "[\"compile\",\"run\"]")))))

(deftest script-from-a-file
  (let [modules-dir "test/resources"
        script-file (str modules-dir "/scripts.jq")]
    (testing "Inlined script with a slurped file"
      (let [script (str (slurp script-file) "map(increment(.))")
            processor-fn (jq/flexible-processor script {:output :string})
            resp (processor-fn string-data)]
        (is (= result-string resp))))
    (testing "include module"
      (let [script "include \"scripts\"; map(increment(.))"
            processor-fn (jq/flexible-processor script {:output  :string
                                                        :modules modules-dir})
            resp (processor-fn string-data)]
        (is (= result-string resp))))
    (testing "import module"
      (let [script "import \"scripts\" as scripts; map(scripts::increment(.))"
            processor-fn (jq/flexible-processor script {:output :string
                                                        :modules [modules-dir]})
            resp (processor-fn string-data)]
        (is (= result-string resp))))))

(deftest simple-execution
  (let [resp (jq/execute string-data query)]
    (is (string? resp))
    (is (= result-string resp)))
  (testing "loading scripts from file"
    (let [opts {:modules "test/resources"}
          query "include \"scripts\"; map(increment(.))"
          resp (jq/execute string-data query opts)]
      (is (string? resp))
      (is (= result-string resp)))))

(deftest multi-output-execution
  (testing "running a processor in multi-output mode, simple case"
    (let [data-1 "[1,2,3]"
          data-2 "[4,5,6]" ;; testing two calls ensures we aren't retaining extra state
          script ".[] | {\"out\": .}"
          processor-fn (jq/flexible-processor script {:output :string})]
      (is (= "{\"out\":1}\n{\"out\":2}\n{\"out\":3}" (processor-fn data-1)))
      (is (= "{\"out\":4}\n{\"out\":5}\n{\"out\":6}" (processor-fn data-2)))))
  (testing "checking that queries can choose not to return any output given an input"
    (let [data "[9,10,11]"
          script ".[] | select(. >= 10)"
          processor-fn (jq/flexible-processor script {:output :string})]
      (is (= "10\n11" (processor-fn data))))))

(deftest string-to-string-execution
  (testing "mapping function onto values"
    (let [processor-fn (jq/processor query)]
      (is (= result-string (processor-fn string-data)))))
  (testing "loading scripts from file"
    (let [opts {:modules "test/resources"}
          query "include \"scripts\"; map(increment(.))"
          processor-fn (jq/processor query opts)]
      (is (= result-string (processor-fn string-data))))))

(deftest flexible-processor-options
  (testing "String to String"
    (let [processor-fn (jq/flexible-processor query {:output :string})]
      (is (= result-string (processor-fn string-data)))))

  (testing "String to JsonNode"
    (let [result-string "[[2,3,4]]"
          processor-fn (jq/flexible-processor query {:output :json-node})
          resp (processor-fn string-data)]
      (is (instance? JsonNode resp))
      (is (= result-string (utils/json-node->string resp)))))

  (testing "JsonNode to JsonNode"
    (let [result-string "[[2,3,4]]"
          processor-fn (jq/flexible-processor query {:output :json-node})
          resp (processor-fn json-node-data)]
      (is (instance? JsonNode resp))
      (is (= result-string (utils/json-node->string resp)))))

  (testing "JsonNode to String"
    (let [processor-fn (jq/flexible-processor query {:output :string})
          resp (processor-fn json-node-data)]
      (is (string? resp))
      (is (= result-string resp)))))

(deftest streaming-processor
  (testing "simple case"
    (let [processor-fn (jq/stream-processor query)
          out (processor-fn json-node-data)]
      (is (= 1 (count out)))
      (is (= "[2,3,4]" (utils/json-node->string (first out))))))
  (testing "multiple output"
    (let [processor-fn (jq/stream-processor ".[] | (. , .)")
          out (processor-fn json-node-data)]
      (is (= 6 (count out)))
      (is (= ["1" "1" "2" "2" "3" "3"] (mapv utils/json-node->string out)))))
  (testing "passing variables in at runtime"
    (let [script "[$cvar, $rvar]"
          processor-fn (jq/stream-processor script {:vars {:cvar "compile"}})
          resp (-> (utils/string->json-node "null")
                   (processor-fn {:vars {:rvar "run"}}))]
      (is (= 1 (count resp)))
      (is (= "[\"compile\",\"run\"]" (-> resp first utils/json-node->string))))))
