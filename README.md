Geoserver Build
===============

Build a GeoServer war file with the following;

* The XSLT extension installed
* CSV with metadata header plugin
* Layer filter extension
* Netcdf output extension

# To build:
```
git submodule update --init
mvn clean
mvn -P wps,xslt install -DskipTests

```

# Other notes:
```
# issue with clean not propagating,
pushd src/geoserver/src
mvn clean
popd

# and/or
rm -rf $( find -type d -iname '*target*' )

# start build from intermediate module
mvn install -rf :netcdf-output

```
