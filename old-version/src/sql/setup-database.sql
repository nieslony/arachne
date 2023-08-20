/*****
 Don't forget to enable password authentication on postgres server,
 1. Edit /var/lib/pgsql/data/pg_hba.conf
 2. Add the following line:
  host    DB_NAME     DB_USER     127.0.0.1/32    md5
3.  Restart postgresql-server:
  systemctl restart postgresql
 *****/

DROP DATABASE IF EXISTS DB_NAME;
DROP USER IF EXISTS DB_USER;

CREATE USER DB_USER WITH PASSWORD 'DB_PASSWORD';
CREATE DATABASE DB_NAME OWNER DB_USER;
