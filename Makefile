.PHONY: test
test:
	clojure -M:test

.PHONY: lint
lint:
	clojure -M:clj-kondo

.PHONY: check-deps
check-deps:
	clojure -Sdeps '{:deps {antq/antq {:mvn/version "RELEASE"}}}' -M -m antq.core

.PHONY: pom.xml
pom.xml:
	clojure -Spom

.PHONY: uberhar
uberjar: pom.xml
	clojure -X:uberjar \
	:jar target/clj-jq-uber.jar \
	:main-class jq.cli \
	:aliases '[:cli]'

.PHONY: release
release:
	rm release.properties || true
	rm pom.xml.releaseBackup || true
	clojure -Spom
	mvn release:prepare

.PHONY: native-image
native-image:
	CLJ_JQ_STATIC=false ./script/compile

.PHONY: static-native-image
static-native-image:
	CLJ_JQ_STATIC=true ./script/compile
