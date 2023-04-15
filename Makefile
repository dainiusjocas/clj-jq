.PHONY: test
test:
	clojure -M:test

.PHONY: lint
lint:
	clojure -M:clj-kondo --lint build cli src test

.PHONY: check-deps
check-deps:
	clojure -Sdeps '{:deps {com.github.liquidz/antq {:mvn/version "RELEASE"} org.slf4j/slf4j-nop {:mvn/version "RELEASE"}}}' -M -m antq.core

.PHONY: uberjar
uberjar:
	clojure -T:build clean
	clojure -T:build uber

.PHONY: deploy-to-clojars
deploy-to-clojars:
	mvn -B -DnewVersion="$$(cat resources/CLJ_JQ_VERSION)" -DgenerateBackupPoms=false versions:set
	clojure -T:build jar
	clojure -X:deploy

.PHONY: native-image
native-image: uberjar
	CLJ_JQ_STATIC=false ./script/compile

.PHONY: static-native-image
static-native-image: uberjar
	./script/setup-musl
	PATH=$$HOME/.musl/bin:$$PATH CLJ_JQ_STATIC=true CLJ_JQ_MUSL=true ./script/compile
