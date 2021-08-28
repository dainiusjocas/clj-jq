.PHONY: test
test:
	clojure -M:test

.PHONY: lint
lint:
	clojure -M:clj-kondo

.PHONY: check-deps
check-deps:
	clojure -Sdeps '{:deps {antq/antq {:mvn/version "RELEASE"}}}' -M -m antq.core

.PHONY: uberjar
uberjar:
	clojure -T:build clean
	clojure -T:build uber

.PHONY: release
release:
	rm release.properties || true
	rm pom.xml.releaseBackup || true
	clojure -Spom
	mvn release:prepare

.PHONY: native-image
native-image: uberjar
	CLJ_JQ_STATIC=false ./script/compile

.PHONY: static-native-image
static-native-image:
	CLJ_JQ_STATIC=true ./script/compile
