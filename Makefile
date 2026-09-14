.PHONY: up down restart logs ps build test clean

up:
	docker compose up -d

down:
	docker compose down

health:
	docker ps -a

restart:
	docker compose down
	docker compose up -d

logs:
	docker compose logs -f

ps:
	docker compose ps

build:
	./mvnw clean package

test:
	./mvnw test

clean:
	./mvnw clean

shell:
	docker exec -it url-shortener-postgres psql -U postgres -d url_shortener