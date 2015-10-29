#!/bin/bash -x
rm schema.sql

# dump schema
sudo -u postgres pg_dump -d harvest -n anmn_ts -s -x -O > schema.sql || exit

# change schema name
sed -i 's/CREATE SCHEMA anmn_ts/CREATE SCHEMA anmn_ts_/' schema.sql  || exit
sed -i 's/SET search_path = anmn_ts/SET search_path = anmn_ts_/' schema.sql || exit

# reinsert new schema
sudo -u postgres psql -d harvest -c 'drop schema if exists anmn_ts_ cascade' || exit
sudo -u postgres psql -d harvest -f schema.sql || exit
rm schema.sql

S=$(cat <<EOF
set search_path=anmn_ts, public;
insert into anmn_ts_.indexed_file select * from indexed_file;
insert into anmn_ts_.timeseries select * from timeseries;
insert into anmn_ts_.measurement
select *
from measurement where "TIME" >= '2015-01-01T23:00:00Z' and "TIME" <= '2015-05-14T00:00:00Z'
;
EOF
)

# subset data
sudo -u postgres psql -d harvest -c "$S" || exit

# report size
sudo -u postgres psql -d harvest -P pager=off -c " select * from admin.size_relation where schema = 'anmn_ts_'" 

# dump schema with data
rm anmn_ts.sql
sudo -u postgres pg_dump -d harvest -n anmn_ts_ -x -O  > anmn_ts.sql || exit

# change schema name
sed -i 's/CREATE SCHEMA anmn_ts_/CREATE SCHEMA anmn_ts/' anmn_ts.sql || exit
sed -i 's/SET search_path = anmn_ts_/SET search_path = anmn_ts/' anmn_ts.sql || exit

