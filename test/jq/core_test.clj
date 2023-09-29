(ns jq.core-test
  {:clj-kondo/config '{:linters {:deprecated-namespace {:level :off}}}}
  (:require [clojure.test :refer [deftest is testing]]
            [jq.core :as jq])
  (:import (com.fasterxml.jackson.databind ObjectMapper)))

(defn json-string->json-node [^String json-string]
  (.readTree (ObjectMapper.) json-string))

(deftest raw-execute
  (testing "length function"
    (let [data "{\"a\":[1,2,3,4,5],\"b\":\"hello\"}"
          query ".a | length"]
      (is (= "5" (jq/execute data query)))))

  (testing "compound sum function"
    (let [data "{\"a\":[1,2,3,4,5],\"b\":\"hello\"}"
          query ".a | .[0] + .[1]"]
      (is (= "3" (jq/execute data query)))))

  (testing "mapping function onto values"
    (let [data "[1,2,3]"
          query "map(.+1)"]
      (is (= "[2,3,4]" (jq/execute data query)))))

  (testing "inline function definition"
    (let [data "[[1,2],[10,20]]"
          query "def addvalue(f): . + [f]; map(addvalue(.[0]))"]
      (is (= "[[1,2,1],[10,20,10]]" (jq/execute data query))))))

(deftest test-processor
  (testing "mapping function onto values"
    (let [data "[1,2,3]"
          query "map(.+1)"
          processor-fn (jq/processor query)]
      (is (= "[2,3,4]" (processor-fn data))))))

(deftest json-node-processor
  (testing "mapping function onto values"
    (let [data "[1,2,3]"
          query "map(.+1)"
          processor-fn (jq/json-node-processor query)]
      (is (= "[2,3,4]" (processor-fn (json-string->json-node data)))))))
