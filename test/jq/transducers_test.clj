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
                         (jq/JsonNode->clj))
                       data))))

    (testing "string input is not parsed, it is converted to TextNode as is"
      (is (= ["test"]
             (sequence (comp
                         (jq/->JsonNode)
                         (jq/search ".")
                         (jq/JsonNode->clj))
                       ["test"]))))

    (testing "JSON string parser, it is converted to TextNode as is"
      (is (= [{"foo" "bar"}]
             (sequence (comp
                         (jq/parser)
                         (jq/search ".")
                         (jq/JsonNode->clj))
                       [(json/write-value-as-string {"foo" "bar"})]))))

    (testing "string serializer"
      (is (= ["{\"a\":\"b\"}" "{\"a\":\"b\"}"]
             (sequence (comp
                         (jq/->JsonNode)
                         (jq/search "(. , .)")
                         (jq/serializer))
                       [{"a" "b"}]))))

    (testing "output catenation opt"
      (is (= expected-result
             (sequence (comp
                         (jq/->JsonNode)
                         (jq/search query {:cat false})
                         cat
                         (jq/JsonNode->clj))
                       data))))

    (testing "multiple scripts in a row"
      (is (= [3 4 5]
             (sequence (comp
                         (jq/->JsonNode)
                         (jq/search query)
                         (jq/search query)
                         (jq/JsonNode->clj))
                       data))))))

(deftest custom-mappers
  (let [data [{:a :b}]
        query "."]
    (testing "default object mapper is bad at keywords"
      (is (= [{":a" {"name"      "b"
                     "namespace" nil
                     "sym"       {"name"      "b"
                                  "namespace" nil}}}]
             (sequence (comp
                         (jq/->JsonNode)
                         (jq/search query)
                         (jq/JsonNode->clj))
                       data))))
    (testing "keyword aware mappers"
      (let [mapper json/keyword-keys-object-mapper]
        (is (= [{:a "b"}]
               (sequence (comp
                           (jq/->JsonNode mapper)
                           (jq/search query)
                           (jq/JsonNode->clj mapper))
                         data)))))

    (testing "parser and serializer"
      (is (= ["{\"foo\":\"bar\"}" "{\"foo\":\"bar\"}"]
             (sequence (comp
                         (jq/parser json/keyword-keys-object-mapper)
                         (jq/search "(. , .)")
                         (jq/serializer json/keyword-keys-object-mapper))
                       [(json/write-value-as-string {:foo "bar"})]))))))

(deftest convenience-transducer
  (let [data [1 2 3]
        query "(. , .)"]
    (is (= [1 1 2 2 3 3]
           (sequence (jq/process query) data)))))
