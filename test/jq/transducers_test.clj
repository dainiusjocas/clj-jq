(ns jq.transducers-test
  (:require [clojure.test :refer [deftest is testing]]
            [jq.transducers :as jq]
            [jsonista.core :as json]))

(deftest transducers-interface
  (let [data [1 2 3]
        expression ".+1"
        expected-result [2 3 4]]
    (testing "mapper"
      (is (= expected-result
             (sequence (comp
                         (jq/->JsonNode)
                         (jq/execute expression)
                         (jq/JsonNode->value))
                       data))))

    (testing "string input is not parsed, it is converted to TextNode as is"
      (is (= ["test"]
             (sequence (comp
                         (jq/->JsonNode)
                         (jq/execute ".")
                         (jq/JsonNode->value))
                       ["test"]))))

    (testing "JSON string parser, it is converted to TextNode as is"
      (is (= [{"foo" "bar"}]
             (sequence (comp
                         (jq/parse)
                         (jq/execute ".")
                         (jq/JsonNode->value))
                       [(json/write-value-as-string {"foo" "bar"})]))))

    (testing "string serializer"
      (is (= ["{\"a\":\"b\"}" "{\"a\":\"b\"}"]
             (sequence (comp
                         (jq/->JsonNode)
                         (jq/execute "(. , .)")
                         (jq/serialize))
                       [{"a" "b"}]))))

    (testing "output catenation opt"
      (is (= (sequence (comp
                         (jq/->JsonNode)
                         (jq/execute expression)
                         (jq/JsonNode->value))
                       data)
             (sequence (comp
                         (jq/->JsonNode)
                         (jq/execute expression {:cat false})
                         cat
                         (jq/JsonNode->value))
                       data))))

    (testing "multiple scripts in a row"
      (is (= [3 4 5]
             (sequence (comp
                         (jq/->JsonNode)
                         (jq/execute expression)
                         (jq/execute expression)
                         (jq/JsonNode->value))
                       data))))))

(deftest custom-mappers
  (let [data [{:a :b}]
        expression "."]
    (testing "default object mapper is bad at keywords"
      (is (= [{":a" {"name"      "b"
                     "namespace" nil
                     "sym"       {"name"      "b"
                                  "namespace" nil}}}]
             (sequence (comp
                         (jq/->JsonNode)
                         (jq/execute expression)
                         (jq/JsonNode->value))
                       data))))
    (testing "keyword aware mappers"
      (let [mapper json/keyword-keys-object-mapper]
        (is (= [{:a "b"}]
               (sequence (comp
                           (jq/->JsonNode mapper)
                           (jq/execute expression)
                           (jq/JsonNode->value mapper))
                         data)))))

    (testing "parser and serializer"
      (is (= ["{\"foo\":\"bar\"}" "{\"foo\":\"bar\"}"]
             (sequence (comp
                         (jq/parse json/keyword-keys-object-mapper)
                         (jq/execute "(. , .)")
                         (jq/serialize json/keyword-keys-object-mapper))
                       [(json/write-value-as-string {:foo "bar"})]))))

    (testing "pretty serializer"
      (is (= ["{\n  \"foo\" : \"bar\"\n}"]
             (sequence (comp
                         (jq/->JsonNode)
                         (jq/execute ".")
                         (jq/pretty-print))
                       [{"foo" "bar"}]))))))

(deftest convenience-transducer
  (let [data [1 2 3]
        expression "(. , .)"]
    (is (= [1 1 2 2 3 3]
           (sequence (jq/process expression) data)))))
