# Users schema
 
# --- !Ups

CREATE SEQUENCE user_id_seq;
CREATE TABLE users (
    id      integer NOT NULL DEFAULT nextval('user_id_seq'),
    uuid    varchar(255),
    email   varchar(255),
    openid  varchar(255),
    first   varchar(255),
    last    varchar(255)
);
 
# --- !Downs
 
DROP TABLE users;
DROP SEQUENCE user_id_seq;
