#!/usr/bin/env bb

(def pom-xml (xml/parse-str (slurp "pom.xml") :coalescing false))

(def version (->> pom-xml
                  (:content)
                  (filter (fn [node]
                            (= (:tag node)
                               :xmlns.http%3A%2F%2Fmaven.apache.org%2FPOM%2F4.0.0/version)))
                  first
                  :content
                  first))

(println version)
