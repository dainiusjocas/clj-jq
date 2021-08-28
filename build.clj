(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'lt.jocas/clj-jq)
(def version (format "1.0.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"
                            :aliases [:cli]}))
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

(defn clean [_]
      (b/delete {:path "target"}))

(defn version-file [_]
      (b/write-file {:path    "resources/CLJ_JQ_VERSION"
                     :string  version}))

(defn prep [_]
      (b/write-pom {:src-pom "./pom.xml"
                    :class-dir class-dir
                    :lib lib
                    :version version
                    :basis basis
                    :src-dirs ["src"]})
      (b/copy-dir {:src-dirs ["src" "resources"]
                   :target-dir class-dir}))

(defn uber [_]
      (version-file nil)
      (b/copy-dir {:src-dirs ["src" "resources"]
                   :target-dir class-dir})
      (b/compile-clj {:basis basis
                      :src-dirs ["src" "cli"]
                      :class-dir class-dir})
      (b/uber {:class-dir class-dir
               :uber-file uber-file
               :basis     basis
               :manifest  {"Main-Class" "jq.cli"}}))
