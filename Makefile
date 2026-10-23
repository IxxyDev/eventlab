.PHONY: db db-stop psql api test format web sql check-version release

db:
	docker compose up -d --wait

db-stop:
	docker compose stop

psql:
	psql -h 127.0.0.1 -p 5433 -U eventlab -d eventlab

api:
	cd eventlab-api && ./gradlew bootRun

test:
	cd eventlab-api && ./gradlew test --rerun check

format:
	cd eventlab-api && ./gradlew spotlessApply

web:
	cd eventlab && npm run dev

sql:
	cd eventlab-api && postgres-language-server check src/main/resources/sql

check-version:
	@web=$$(jq -r .version eventlab/package.json); \
	api=$$(sed -n 's/^version=//p' eventlab-api/gradle.properties); \
	echo "expected=$(VERSION) web=$$web api=$$api"; \
	test "$$web" = "$(VERSION)" && test "$$api" = "$(VERSION)"

release:
	@test -n "$(VERSION)" || (echo "Usage: make release VERSION=0.1.0" && exit 1)
	cd eventlab && npm version $(VERSION) --no-git-tag-version
	sed -i.bak 's/^version=.*/version=$(VERSION)/' eventlab-api/gradle.properties
	rm eventlab-api/gradle.properties.bak
	git commit -m "Release $(VERSION)" eventlab/package.json eventlab/package-lock.json eventlab-api/gradle.properties
	git tag -a v$(VERSION) -m "EventLab $(VERSION)"
