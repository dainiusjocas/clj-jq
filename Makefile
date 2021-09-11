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

.PHONY: deploy-to-clojars
deploy-to-clojars:
	clojure -T:build jar
	clojure -X:deploy

.PHONY: native-image
native-image: uberjar
	CLJ_JQ_STATIC=false ./script/compile

.PHONY: static-native-image
static-native-image: uberjar
	./script/setup-musl
	PATH=$$HOME/.musl/bin:$$PATH CLJ_JQ_STATIC=true CLJ_JQ_MUSL=true ./script/compile
