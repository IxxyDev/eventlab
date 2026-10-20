.PHONY: db db-stop psql api test format web

db:
	docker compose up -d --wait

db-stop:
	docker compose stop

psql:
	psql -h 127.0.0.1 -p 5433 -U eventlab -d eventlab

api:
	cd eventlab-api && ./gradlew bootRun

test:
	cd eventlab-api && ./gradlew check

format:
	cd eventlab-api && ./gradlew spotlessApply

web:
	cd eventlab && npm run dev
