(ns jq.transducers-test
  (:require [clojure.test :refer [deftest is testing]]
            [jq.transducers :as jq]
            [jsonista.core :as json]))

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

(deftest custom-mappers
  (let [data [{:a :b}]
        query "."]
    (testing "default object mappers"
      (is (= [{":a" {"name"      "b"
                     "namespace" nil
                     "sym"       {"name"      "b"
                                  "namespace" nil}}}]
             (sequence (comp
                         (jq/->JsonNode)
                         (jq/search query)
                         cat
                         (jq/JsonNode->clj))
                       data))))
    (testing "keyword aware mappers"
      (is (= [{:a "b"}]
             (sequence (comp
                         (jq/->JsonNode json/keyword-keys-object-mapper)
                         (jq/search query)
                         cat
                         (jq/JsonNode->clj json/keyword-keys-object-mapper))
                       data))))))
