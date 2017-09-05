# NetCDF Generator

## Requires
* java 1.7

## setup env vars for integration tests

    $ sudo -u postgres psql -d postgres
    # drop database mytest;
    # create user mytest password 'mytest';
    # create database mytest owner mytest;
    # \c mytest
    # create extension postgis schema public;

    export POSTGRES_USER='mytest'
    export POSTGRES_PASS='mytest'
    export POSTGRES_JDBC_URL='jdbc:postgresql://127.0.0.1/mytest'

## To compile and run unit + integration tests
mvn clean install

## Specific integration test - useful for developing
mvn -Dit.test=GenerationIT#testAnmnTs verify

## References

**CQL**

* http://svemir.co/2012/08/16/introduction-to-cql-and-geoserver-implementation/
* http://en.cppreference.com/w/c/language/operator_precedence


**timeseries example**

```
jfca@10-nsp-mel:~$ ncdump /mnt/opendap/1/IMOS/opendap/eMII/checker_test/ANMN/timeSeries/IMOS_ANMN-QLD_TZ_20140907T063947Z_ITFTIS_FV01_ITFTIS-1409-SBE39-94_END-20150207T065000Z_C-20150219T005030Z.nc | less
```

**profile example**

```
jfca@10-nsp-mel:~$ ncdump /mnt/opendap/1/IMOS/opendap/eMII/checker_test/ANMN/profile/IMOS_ANMN-NRS_CDEKOSTUZ_20150224T023931Z_NRSROT_FV01_Profile-SBE19plus_C-20150227T052824Z.nc  | less
```

# Running from the command line

```
$ mvn exec:java -Dexec.mainClass=au.org.emii.ncdfgenerator.Main -Dexec.args=""
```

e.g.: assuming the following environment variables are set appropriately:

* `JODAAC_DB_USERNAME`
* `JODAAC_DB_PASSWORD`
* `JODAAC_DB_NAME`
* `JODAAC_DB_HOST`


**anmn_ts**

```
$ mvn exec:java -Dexec.mainClass=au.org.emii.ncdfgenerator.Main \
    -Dexec.args=" \
      -u $JODAAC_DB_USERNAME -p $JODAAC_DB_PASSWORD -d $JODAAC_DB_NAME -h $JODAAC_DB_HOST \
      -c \"INTERSECTS(geom,POLYGON((113.33 -33.09,113.33 -30.98,117.11 -30.98,117.11 -33.09,113.33 -33.09))) \
           AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-01-15T00:00:00Z'\" \
      -P anmn_ts \
      -s anmn_ts"
```

**soop\_sst_trajectory**

```
$ mvn exec:java -Dexec.mainClass=au.org.emii.ncdfgenerator.Main \
    -Dexec.args=" \
        -u $JODAAC_DB_USERNAME -p $JODAAC_DB_PASSWORD -d $JODAAC_DB_NAME -h $JODAAC_DB_HOST \
        -c \"INTERSECTS(instance_geom,POLYGON((159 -43,159 -39,165 -39,165 -43,159 -43)))\" \
        -P soop_sst_trajectory \
        -s soop_sst"
```

# Running using WPS

In order to run as a WPS request, you can run the following:

```
$ curl --data @wps-netcdf-output.xml --header "Expect:" --header "Content-Type: application/xml" http://localhost:8080/wps
```

----

http://www.unidata.ucar.edu/software/thredds/v4.3/netcdf-java/reference/faq.html

https://www.unidata.ucar.edu/software/netcdf/docs/netcdf/Variables.html

https://www.unidata.ucar.edu/software/netcdf/docs/netcdf/Dimensions.html

http://www.unidata.ucar.edu/software/thredds/current/netcdf-java/tutorial/NetcdfFileWriteable.html

http://www.unidata.ucar.edu/software/netcdf/docs/BestPractices.html

https://www.unidata.ucar.edu/software/netcdf/docs/netcdf/CDL-Data-Types.html
