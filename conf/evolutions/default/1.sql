# --- !Ups

CREATE TABLE "events" (
  "tx_id" SERIAL PRIMARY KEY,
  "entity_id" BIGINT,
  "event" TEXT);

CREATE SEQUENCE "events_entity_id_seq";

# --- !Downs

DROP SEQUENCE "events_entity_id_seq";

DROP TABLE "events";