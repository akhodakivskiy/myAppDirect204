# Account edition/duration/active columns
 
# --- !Ups

ALTER TABLE accounts ADD COLUMN edition varchar(255);
ALTER TABLE accounts ADD COLUMN duration varchar(255);
ALTER TABLE accounts ADD COLUMN active boolean;
 
# --- !Downs
 
ALTER TABLE accounts DROP COLUMN edition;
ALTER TABLE accounts DROP COLUMN duration;
ALTER TABLE accounts DROP COLUMN active;
