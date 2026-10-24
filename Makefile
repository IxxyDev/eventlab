.PHONY: db db-stop psql api migrate test format web web-test e2e-db web-e2e sql check-version release tag

db:
	docker compose up -d --wait

db-stop:
	docker compose stop

psql:
	psql -h 127.0.0.1 -p 5433 -U eventlab -d eventlab

api:
	cd eventlab-api && ./gradlew bootRun

migrate:
	cd eventlab-api && ./gradlew bootRun --args='--spring.main.web-application-type=none'

test:
	cd eventlab-api && ./gradlew test --rerun check

format:
	cd eventlab-api && ./gradlew spotlessApply

web:
	cd eventlab && npm run dev

web-test:
	cd eventlab && npm test

e2e-db:
	@psql postgresql://eventlab:eventlab@127.0.0.1:5433/postgres -tAc \
		"SELECT 1 FROM pg_database WHERE datname = 'eventlab_e2e'" | grep -q 1 \
		|| psql postgresql://eventlab:eventlab@127.0.0.1:5433/postgres \
		-c "CREATE DATABASE eventlab_e2e"

web-e2e: e2e-db
	cd eventlab && npm run test:e2e

sql:
	cd eventlab-api && postgres-language-server check src/main/resources/sql

check-version:
	@web=$$(jq -r .version eventlab/package.json); \
	api=$$(sed -n 's/^version=//p' eventlab-api/gradle.properties); \
	echo "expected=$(VERSION) web=$$web api=$$api"; \
	test "$$web" = "$(VERSION)" && test "$$api" = "$(VERSION)"

release:
	@test -n "$(VERSION)" || (echo "Usage: make release VERSION=0.1.0" && exit 1)
	git fetch origin
	git switch -c release/$(VERSION) origin/main
	cd eventlab && npm version $(VERSION) --no-git-tag-version
	sed -i.bak 's/^version=.*/version=$(VERSION)/' eventlab-api/gradle.properties
	rm eventlab-api/gradle.properties.bak
	git commit -m "Release $(VERSION)" eventlab/package.json eventlab/package-lock.json eventlab-api/gradle.properties
	git push -u origin release/$(VERSION)
	gh pr create --base main --title "Release $(VERSION)" --body "After merging run: make tag VERSION=$(VERSION)"

tag:
	@test -n "$(VERSION)" || (echo "Usage: make tag VERSION=0.1.0" && exit 1)
	git switch main
	git pull --ff-only origin main
	$(MAKE) check-version VERSION=$(VERSION)
	git tag -a v$(VERSION) -m "EventLab $(VERSION)"
	git push origin v$(VERSION)
