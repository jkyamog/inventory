# --- !Ups

CREATE TABLE "events" (
  "tx_id" SERIAL PRIMARY KEY,
  "entity_id" BIGINT,
  "event" TEXT);

CREATE SEQUENCE "events_entity_id_seq";

CREATE TABLE "products" (
  "id" BIGINT PRIMARY KEY,
  "name" TEXT,
  "quantity" INT
)

# --- !Downs

DROP TABLE "products";

DROP SEQUENCE "events_entity_id_seq";

DROP TABLE "events";