# NetCDF Generator

[![Build Status](https://travis-ci.org/aodn/ncdfgenerator.png?branch=master)](https://travis-ci.org/aodn/ncdfgenerator.png)

## Requires
java 1.7

## env vars for integration tests
export POSTGRES_USER='user'
export POSTGRES_PASS='pass'
export POSTGRES_JDBC_URL='jdbc:postgresql://127.0.0.1/mytest'

## To compile and run unit + integration tests
mvn clean install


## references

cql

http://svemir.co/2012/08/16/introduction-to-cql-and-geoserver-implementation/

http://en.cppreference.com/w/c/language/operator_precedence


timeseries example,
jfca@10-nsp-mel:~$ ncdump  /mnt/opendap/1/IMOS/opendap/eMII/checker_test/ANMN/timeSeries/IMOS_ANMN-QLD_TZ_20140907T063947Z_ITFTIS_FV01_ITFTIS-1409-SBE39-94_END-20150207T065000Z_C-20150219T005030Z.nc | less

profile example,
jfca@10-nsp-mel:~$ ncdump /mnt/opendap/1/IMOS/opendap/eMII/checker_test/ANMN/profile/IMOS_ANMN-NRS_CDEKOSTUZ_20150224T023931Z_NRSROT_FV01_Profile-SBE19plus_C-20150227T052824Z.nc  | less

----

http://www.unidata.ucar.edu/software/thredds/v4.3/netcdf-java/reference/faq.html

https://www.unidata.ucar.edu/software/netcdf/docs/netcdf/Variables.html

https://www.unidata.ucar.edu/software/netcdf/docs/netcdf/Dimensions.html

http://www.unidata.ucar.edu/software/thredds/current/netcdf-java/tutorial/NetcdfFileWriteable.html

http://www.unidata.ucar.edu/software/netcdf/docs/BestPractices.html

https://www.unidata.ucar.edu/software/netcdf/docs/netcdf/CDL-Data-Types.html

