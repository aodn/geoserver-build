# Geoserver Build

## Configures a GeoServer war file with the following extensions installed

* The [XSLT WFS Output Format](https://docs.geoserver.org/stable/en/user/extensions/xslt/index.html)
  extension installed
* The [GeoWebCache s3 blob store](https://docs.geoserver.org/stable/en/user/extensions/gwc-s3/index.html)
  extension installed
* The [Control Flow Module](https://docs.geoserver.org/stable/en/user/extensions/controlflow/index.html) 
  extension installed
* [SqlServer support](https://docs.geoserver.org/stable/en/user/data/database/sqlserver.html) used by IMAS installed

## Builds the following AODN extensions and includes them:

* CSV with metadata header WFS output format
* [Layer filter configuration plugin](src/extension/layer-filters/README.md)
* [NCWMS](src/extension/ncwms/README.md)

## Makes the following customisations to the geoserver war

### Return 500 errors instead of 200 for API errors so squid doesn't cache API errors

The geoserver `dispatcher` bean is overriden with `src/main/java/org/geoserver/ows/DispatcherWithHttpStatus.java` class which handles all OWS service exceptions by sending an HTTP 500 response. Otherwise Geoserver will modify the response to potentially send other HTTP response codes. These can end up as 200 codes depending on the service.


Squid will cache 200 responses which may actually contain error reporting content. These will be erroneously returned as the response when there is a cache hit.

Without the customisation `http://localhost:8080/geoserver/wms?request=DescribeLayer&service=WMS&version=1.1.1&layers=imos:NON_EXISTENT_LAYER` would return:

```
Status: 200 OK
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE ServiceExceptionReport SYSTEM "http://localhost:8080/geoserver/schemas/wms/1.1.1/WMS_exception_1_1_1.dtd">
<ServiceExceptionReport version="1.1.1" >
    <ServiceException code="LayerNotDefined" locator="MapLayerInfoKvpParser">
      imos:NON_EXISTENT_LAYER: no such layer on this server
</ServiceException>
</ServiceExceptionReport>
```

With the customisation we get:

```
Status: 500 Internal Server Error
<!doctype html>
<html lang="en">
<head>
<title>HTTP Status 500 – Internal Server Error</title>
<style type="text/css">body {font-family:Tahoma,Arial,sans-serif;} h1, h2, h3, b {color:white;background-color:#525D76;} h1 {font-size:22px;} h2 {font-size:16px;} h3 {font-size:14px;} p {font-size:12px;} a {color:black;} .line {height:1px;background-color:#525D76;border:none;}</style>
</head>
<body>
<h1>HTTP Status 500 – Internal Server Error</h1><hr class="line" />
<p><b>Type</b> Status Report</p>
<p><b>Description</b> The server encountered an unexpected condition that prevented it from fulfilling the request.</p>
<hr class="line" /><h3>Apache Tomcat/8.5.73</h3>
</body>
</html>
```

### Enable CORS for GET requests so IMAS can use it for GetFeatureInfo requests

For Tomcat CORS is enabled in `src/main/webapp/WEB-INF/web.xml`.  The `cors.filter.class` is specified in `src/main/pom.xml`.

To test send an OPTIONS request:
```
OPT http://{{host}}{{site}}/wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetFeatureInfo&FORMAT=image%2Fpng&TRANSPARENT=true&QUERY_LAYERS=imos:anmn_velocity_timeseries_map&STYLES&LAYERS=imos:anmn_velocity_timeseries_map&exceptions=application/vnd.ogc.se_inimage&INFO_FORMAT=text/html&FEATURE_COUNT=50&X=50&Y=50&SRS=EPSG%3A4326&WIDTH=101&HEIGHT=101&BBOX=126.1669921875%2C-16.787109375%2C135.0439453125%2C-7.91015625
```

Response headers will include:

```json
[
  {"key":"Access-Control-Allow-Origin","value":"*"},
  {"key":"Access-Control-Allow-Methods","value":"GET"},
  {"key":"Access-Control-Allow-Headers","value":"*"}
]
```

## Licensing
This project is licensed under the terms of the GNU GPLv3 license.


## To build

```
mvn clean install -U 
```

Copy the sample context.xml file to configure the default/additional jndi resources

```
cd src/main/src/jetty
cp context-sample.xml context.xml
```

## Provision Geoserver
A [job in jenkins](https://build.aodn.org.au/job/geoserver-build_build/) create the artifact, but the real provision
is done via cloudformation where it combines the [configuration](https://github.com/aodn/geoserver-config) 
of geoserver, for example [workspaces](https://github.com/aodn/geoserver-config/tree/master/workspaces/imos)
so you can search the layer in geoserver. In short the layers are predefined by this configuration, however
the data about this layer comes from the PostGis database that set in the cloudformation.

This issue contains extra [information](https://github.com/aodn/backlog/issues/4098)

## Running using Jetty

Maven project is in src/main
```
cd ../src/main
```

GeoServer requires extra memory above the normal maven defaults, so you will need to bump up the memory when running this command. For example, run `export MAVEN_OPTS='-Xms100m -Xmx512m -XX:MaxPermSize=192m'`
prior to running this command or add this command to you startup scripts

Then to run jetty on port 9090 you can use:
```
mvn jetty:run-war -Pjetty -Djetty.port=9090 -Duser.timezone=UTC
```

GeoServer will then be available at:

```
http://localhost:9090
```

## Running using Tomcat and IntelliJ

Install Tomcat 8.5

Install Java version 11

Set the maven profile to `tomcat`.

![maven tomcat profile](https://github.com/aodn/geoserver-build/assets/40220935/68d0c69f-1ca9-44db-b59e-edf6b47121a3)

Create a `src/main/webapp/META-INF/context.xml` file using the supplied `src/main/webapp/META-INF/context-sample.xml` as 
a template and update to use the required database datastore.

Create and populate a `src/main/webapp/data` directory with custom content. For all the Postman tests to pass you will need to
update `src/main/webapp/data/ncwms.xml` and `src/main/webapp/data/workspaces/imos/JNDI_argo/argo_primary_profile_core_low_res_good_qc_data/filters.xml`

Update the supplied `Tomcat` run configuration JRE and Application Server to your Corretto and Tomcat.

For complete console logging, create `src/main/webapp/WEB-INF/classes/logging.properties`:

```yaml
org.apache.catalina.core.ContainerBase.[Catalina].level=INFO
org.apache.catalina.core.ContainerBase.[Catalina].handlers=java.util.logging.ConsoleHandler
```

Run the `Tomcat` run configuration.

GeoServer will then be available at:

```
http://localhost:8080
```

Default admin user is `username=admin`, `password=geoserver`

## Using the Postman tests

For all the Postman tests to pass you will need to
update `src/main/webapp/data/ncwms.xml` and `src/main/webapp/data/workspaces/imos/JNDI_argo/argo_primary_profile_core_low_res_good_qc_data/filters.xml`

Import the Postman tests from `src/postman/geoserver-build.postman_collection.json` into Postman and run.
