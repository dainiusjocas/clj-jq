(ns jq.transducers-test
  (:require [clojure.test :refer [deftest is testing]]
            [jq.transducers :as jq]))

(deftest transducers-interface
  (let [data [1 2 3]
        query ".+1"
        expected-result [2 3 4]]
    (testing "mapper"
      (is (= expected-result
             (sequence (comp
                         (jq/->JsonNode)
                         (jq/search query)
                         cat
                         (jq/JsonNode->clj))
                       data))))))
