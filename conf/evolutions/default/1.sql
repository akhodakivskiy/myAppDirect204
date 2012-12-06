# Users schema
 
# --- !Ups

CREATE SEQUENCE account_id_seq;
CREATE TABLE accounts (
    id          integer PRIMARY KEY NOT NULL DEFAULT nextval('account_id_seq'),
    uuid        varchar(255),
    country     varchar(255),
    name        varchar(255),
    phone       varchar(255),
    website     varchar(255),
    edition     varchar(255),
    duration    varchar(255),
    active      boolean,
    partner     varchar(255),
    base_url    varchar(255)
);

CREATE SEQUENCE user_id_seq;
CREATE TABLE users (
    id          integer PRIMARY KEY NOT NULL DEFAULT nextval('user_id_seq'),
    uuid        varchar(255),
    email       varchar(255),
    openid      varchar(255),
    first       varchar(255),
    last        varchar(255),
    account_id  integer REFERENCES accounts(id) ON UPDATE CASCADE ON DELETE CASCADE
);
 
# --- !Downs
 
DROP TABLE users IF EXISTS;
DROP SEQUENCE user_id_seq IF EXISTS;

DROP TABLE accounts IF EXISTS;
DROP SEQUENCE account_id_seq IF EXISTS;
