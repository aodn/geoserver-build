Geoserver Build
===============

Configures a GeoServer war file with the following;

* The XSLT extension installed
* CSV with metadata header plugin
* Layer filter extension

Check out the [extensions' readme](https://github.com/aodn/geoserver-build/blob/master/src/extension/wps/README.md) for more information.

### Prerequisites

To perform aggregation of gridded netcdf files/run unit tests, this project requires the installation of the netcdf c library
version 4.3.1 or above (see [NetCDF-4 C Library Loading](https://www.unidata.ucar.edu/software/thredds/current/netcdf-java/reference/netcdf4Clibrary.html)).

### To build

```
mvn clean install -U 
```
### To setup to run

Create an empty postgres schema that geoserver can connect to as follows: 

| Parameter | Value |
| ---- | ---- |
| host | localhost |
| port | 5432 |
| database | geoserver |
| schema | geoserver |
| user | geoserver |
| password | geoserver |

the schema will be populated with default test data using liquibase

Copy the sample context.xml file to configure the default/additional jndi resources

```
cd src/main/src/jetty
cp context-sample.xml context.xml
```

### Running using Jetty

Maven project is in src/main
```
cd ../src/main
```

GeoServer requires extra memory above the normal maven defaults, so you will need to bump up the memory when running this command. For example, run `export MAVEN_OPTS='-Xms100m -Xmx512m -XX:MaxPermSize=192m'`
prior to running this command or add this command to you startup scripts

Then to run jetty on port 9090 you can use:
```
mvn jetty:run -Djetty.port=9090 -Duser.timezone=UTC
```

GeoServer will then be available at:

```
http://localhost:9090
```
