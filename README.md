Geoserver Build
===============

Configures a GeoServer war file with the following;

* The XSLT extension installed
* CSV with metadata header plugin
* Layer filter extension

Check out the [extensions' readme](https://github.com/aodn/geoserver-build/blob/master/src/extension/wps/README.md) for more information.

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

### To run

```
cd src/main
mvn jetty:run
```

GeoServer will then be available at:

```
http://localhost:8080
```
