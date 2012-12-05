# Account marketpkace columns
 
# --- !Ups

ALTER TABLE accounts ADD COLUMN partner varchar(255);
ALTER TABLE accounts ADD COLUMN base_url varchar(255);
 
# --- !Downs
 
ALTER TABLE accounts DROP COLUMN partner;
ALTER TABLE accounts DROP COLUMN base_url;
