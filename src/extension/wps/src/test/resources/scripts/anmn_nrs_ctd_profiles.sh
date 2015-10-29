#!/bin/bash -x

# report size
sudo -u postgres psql -d harvest -P pager=off -c " select * from admin.size_relation where schema = 'anmn_nrs_ctd_profiles'"

# dump schema + data
sudo -u postgres pg_dump -d harvest -n anmn_nrs_ctd_profiles  -x -O > anmn_nrs_ctd_profiles.sql


