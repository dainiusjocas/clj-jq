(ns jq.api-test
  (:require [clojure.test :refer [deftest is testing]]
            [jq.api :as jq]
            [jq.api.api-impl :as utils])
  (:import (com.fasterxml.jackson.databind JsonNode)))

(def string-data "[1,2,3]")
(def json-node-data (utils/string->json-node string-data))
(def query "map(.+1)")
(def result-string "[2,3,4]")

(deftest simple-execution
  (let [resp (jq.api/execute string-data query)]
    (is (string? resp))
    (is (= result-string resp))))

(deftest string-to-string-execution
  (testing "mapping function onto values"
    (let [processor-fn (jq/processor query)]
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
