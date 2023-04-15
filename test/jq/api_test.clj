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
      (is (= resp "[\"hello\",\"world\",123]")))))

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
    (let [processor-fn (jq/flexible-processor query {:output :json-node})
          resp (processor-fn string-data)]
      (is (instance? JsonNode resp))
      (is (= result-string (utils/json-node->string resp)))))

  (testing "JsonNode to JsonNode"
    (let [processor-fn (jq/flexible-processor query {:output :json-node})
          resp (processor-fn json-node-data)]
      (is (instance? JsonNode resp))
      (is (= result-string (utils/json-node->string resp)))))

  (testing "JsonNode to String"
    (let [processor-fn (jq/flexible-processor query {:output :string})
          resp (processor-fn json-node-data)]
      (is (string? resp))
      (is (= result-string resp)))))
