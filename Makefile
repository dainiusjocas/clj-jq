.PHONY: test
test:
	clojure -M:test

.PHONY: lint
lint:
	clojure -M:clj-kondo

.PHONY: check-deps
check-deps:
	clojure -Sdeps '{:deps {antq/antq {:mvn/version "RELEASE"}}}' -M -m antq.core
