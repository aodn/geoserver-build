#!/bin/bash -x
rm schema.sql

# dump schema
sudo -u postgres pg_dump -d harvest -n soop_sst -s -x -O > schema.sql || exit

# change schema name
sed -i 's/CREATE SCHEMA soop_sst/CREATE SCHEMA soop_sst_/' schema.sql  || exit
sed -i 's/SET search_path = soop_sst/SET search_path = soop_sst_/' schema.sql || exit

# reinsert new schema
sudo -u postgres psql -d harvest -c 'drop schema if exists soop_sst_ cascade' || exit
sudo -u postgres psql -d harvest -f schema.sql || exit
rm schema.sql


S=$(cat <<EOF
set search_path=soop_sst, public;
insert into soop_sst_.indexed_file select * from indexed_file;
insert into soop_sst_.measurements
select * from measurements where "TIME" >= '2013-6-23T00:35:01Z' AND "TIME" <= '2013-7-03T00:40:01Z'
;
EOF
)


# subset data
sudo -u postgres psql -d harvest -c "$S" || exit

# report size
sudo -u postgres psql -d harvest -P pager=off -c " select * from admin.size_relation where schema = 'soop_sst_'"
# dump schema with data
rm soop_sst.sql
sudo -u postgres pg_dump -d harvest -n soop_sst_ -x -O  > soop_sst.sql || exit

# change schema name
sed -i 's/CREATE SCHEMA soop_sst_/CREATE SCHEMA soop_sst/' soop_sst.sql || exit
sed -i 's/SET search_path = soop_sst_/SET search_path = soop_sst/' soop_sst.sql || exit

