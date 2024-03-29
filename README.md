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

Create and populate a `src/main/webapp/data` directory with custom content. Your data dir can be a symlink pointing to a
checkout of `https://github.com/aodn/geoserver-config`.

For all the Postman tests to pass you will need to update `src/main/webapp/data/ncwms.xml` and `src/main/webapp/data/workspaces/imos/JNDI_argo/argo_primary_profile_core_low_res_good_qc_data/filters.xml`.

```xml
src/main/webapp/data/ncwms.xml

<ncwms>
<wfsServer>http://localhost:8080/geoserver/ows</wfsServer>
<urlSubstitution key="^">https://thredds.aodn.org.au/thredds/wms/</urlSubstitution>
</ncwms>
```

```xml
src/main/webapp/data/workspaces/imos/JNDI_argo/argo_primary_profile_core_low_res_good_qc_data/filters.xml

<?xml version="1.0"?>
<filters>
    <filter>
        <name>data_centre_name</name>
        <type>string</type>
        <label>Data centre name</label>
        <visualised>true</visualised>
        <excludedFromDownload>true</excludedFromDownload>
    </filter>
    <filter>
        <name>platform_number</name>
        <type>string</type>
        <label>Platform Number</label>
        <visualised>true</visualised>
        <excludedFromDownload>false</excludedFromDownload>
    </filter>
    <filter>
        <name>juld</name>
        <type>datetime</type>
        <label>Time</label>
        <visualised>true</visualised>
        <excludedFromDownload>false</excludedFromDownload>
    </filter>
    <filter>
        <name>position</name>
        <type>geometrypropertytype</type>
        <label>Bounding Box</label>
        <visualised>true</visualised>
        <excludedFromDownload>false</excludedFromDownload>
    </filter>
    <filter>
        <name>profile_processing_type</name>
        <type>string</type>
        <label>Realtime/Delayed</label>
        <visualised>true</visualised>
        <excludedFromDownload>false</excludedFromDownload>
    </filter>
</filters>

```

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
