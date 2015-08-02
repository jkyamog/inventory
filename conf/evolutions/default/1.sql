# --- !Ups

CREATE TABLE "events" (
  "tx_id" SERIAL PRIMARY KEY,
  "entity_id" UUID,
  "event" TEXT);

CREATE TABLE "products" (
  "id" UUID PRIMARY KEY,
  "name" TEXT,
  "quantity" INT
)

# --- !Downs

DROP TABLE "products";

DROP TABLE "events";