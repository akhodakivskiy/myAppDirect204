# Accounts schema
 
# --- !Ups

CREATE SEQUENCE account_id_seq;
CREATE TABLE accounts (
    id      integer NOT NULL DEFAULT nextval('account_id_seq'),
    uuid    varchar(255),
    country varchar(255),
    name    varchar(255),
    phone   varchar(255),
    website varchar(255)
);

ALTER TABLE users ADD COLUMN account_id integer;
 
# --- !Downs
 
DROP TABLE accounts;
DROP SEQUENCE account_id_seq;

ALTER TABLE users DROP COLUMN account_id;
