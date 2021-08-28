(ns build
  (:require [clojure.tools.build.api :as b]
            [clojure.java.shell :as shell]))

(defn branch-name []
  (:out (shell/sh "git" "rev-parse" "--abbrev-ref" "HEAD")))

(def lib 'lt.jocas/clj-jq)
(def version (if (= "main" (branch-name))
               (format "1.0.%s" (b/git-count-revs nil))
               (format "1.0.%s-%s" (b/git-count-revs nil) "SNAPSHOT")))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"
                            :aliases [:cli]}))
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn version-file [_]
  (b/write-file {:path   "resources/CLJ_JQ_VERSION"
                 :string version}))

(defn prep [_]
  (b/write-pom {:src-pom   "./pom.xml"
                :class-dir class-dir
                :lib       lib
                :version   version
                :basis     basis
                :src-dirs  ["src"]})
  (b/copy-dir {:src-dirs   ["src" "resources"]
               :target-dir class-dir}))

(defn uber [_]
  (version-file nil)
  (b/copy-dir {:src-dirs   ["src" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis     basis
                  :src-dirs  ["src" "cli"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis     basis
           :manifest  {"Main-Class" "jq.cli"}}))

(def lib-basis (b/create-basis {:project "deps.edn"}))
(def lib-jar-file (format "target/%s.jar" (name lib)))

(defn jar [_]
  (clean nil)
  (version-file nil)
  (b/write-pom {:class-dir class-dir
                :lib       lib
                :version   version
                :basis     basis
                :src-dirs  ["src"]})
  (b/copy-dir {:src-dirs   ["src"]
               :target-dir class-dir})
  (b/write-pom {:class-dir "."
                :lib       lib
                :version   version
                :basis     basis
                :src-dirs  ["src"]})
  (b/jar {:class-dir class-dir
          :jar-file  lib-jar-file}))
