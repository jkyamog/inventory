# --- !Ups

CREATE TABLE "events" (
  "tx_id" SERIAL PRIMARY KEY,
  "entity_id" UUID,
  "event" TEXT);

CREATE TABLE "items" (
  "id" UUID PRIMARY KEY,
  "name" TEXT,
  "quantity" INT,
  "archived" BOOLEAN
)

# --- !Downs

DROP TABLE "items";

DROP TABLE "events";