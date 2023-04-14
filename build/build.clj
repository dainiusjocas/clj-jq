(ns build
  (:require [clojure.tools.build.api :as b]
            [clojure.java.shell :as shell]
            [clojure.string :as str]))

(defn branch-name []
  (:out (shell/sh "git" "branch" "--show-current")))

(defn latest-tag []
  (:out (shell/sh "git" "describe" "--tags" "--abbrev=0")))

(defn current-version []
  (let [branch-name-str (str/trim (branch-name))
        tag (str/trim (subs (latest-tag) 1))]
    (if (or (= "main" branch-name-str)
            (str/blank? branch-name-str))
      (format "%s" tag)
      (format "%s-%s" tag "SNAPSHOT"))))

(def lib 'lt.jocas/clj-jq)
(def version (or (System/getenv "version") (current-version)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"
                            :aliases [:cli]}))
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn version-file [_]
  #_(b/write-file {:path   "resources/CLJ_JQ_VERSION"
                 :string version})
  (spit "resources/CLJ_JQ_VERSION" version))

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
  (b/jar {:class-dir class-dir
          :jar-file  lib-jar-file}))

(defn install [_]
  (jar [])
  (b/install {:class-dir class-dir
              :lib       lib
              :version   version
              :jar-file  lib-jar-file
              :basis     basis}))
