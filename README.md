# db-mover

Easily move from one PostgreSQL server to a new PostgreSQL 9.5+ server with zero or limited downtime.

## Usage

*Important: You must move to PostgreSQL 9.5+*

- Setup your tables under `db-mover.core/start-movers`
  - Use `db-mover.move/move-at` and `db-mover.move/move-multi-at`
- Set database configurations
  - `from` is the database that is creating data
  - `to` is the database that is receiving data
- Profit

### `unique-fields`?
List of fields that comprise the table's unique condition. db-mover cannot
currently move tables that do not have unique entries.

### Field Types
  - `:integer` = Seconds epoch (string or int)
  - `:timestamp` = SQL Timestamp

## Target Database Setup

1. Create the new database
- Move db roles (i.e. user accounts) *Remember on RDS you cannot use pg_dumpall*
  - In `psql` use `\du` to list roles and `\z` to list [permissions](https://www.postgresql.org/docs/9.5/static/sql-grant.html)
  - `CREATE ROLE [DB_USER] password '[USER_PASSWORD]' login;`
  <br />`GRANT USAGE ON SCHEMA "public" TO "[DB_USER]";`
  <br />`GRANT SELECT,INSERT,UPDATE,DELETE ON ALL TABLES IN SCHEMA "public" TO "[DB_USER]";`
- Move schema (perhaps like):
  - `pg_dump -h [FROM_HOST] -U [USER] -d [DB] -s -Fc > out.sql`
  - `pg_restore -v -h [TO_HOST] -U [USER] -d [DB] out.sql`
- Move data on frozen/static tables
  - `pg_dump -h [FROM_HOST] -U [USER] -d [DB] --data-only -b -t [table1] -t [table2] -Fc > table-data.sql`
  - `pg_restore -v -h [TO_HOST] -U [USER] -d [DB] table-data.sql`

## Run

- Get [Clojure](https://clojure.org/)
- Check your environment in env.clj
- lein run

## Check status
Hitting the root with http will return json with current status. For example:
http://localhost:8080

## Build Docker

- `make build`

## What about...

### Long running transactions?
Currently we always backup 30 seconds. Last known value - 30 seconds is the starting point.

### Other tables?
You get to move those manually.

### Sync'ing changes the other direction?
Nope.
