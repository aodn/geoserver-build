Geoserver Build
===============

Configures a GeoServer war file with the following extensions installed

* The [XSLT WFS Output Format](https://docs.geoserver.org/stable/en/user/extensions/xslt/index.html)
  extension installed
* The [GeoWebCache s3 blob store](https://docs.geoserver.org/stable/en/user/extensions/gwc-s3/index.html)
  extension installed
* The [Control Flow Module](https://docs.geoserver.org/stable/en/user/extensions/controlflow/index.html) 
  extension installed
* [SqlServer support](https://docs.geoserver.org/stable/en/user/data/database/sqlserver.html) used by IMAS installed

Builds the following AODN extensions and includes them:

* CSV with metadata header WFS output format
* [Layer filter configuration plugin](src/extension/layer-filters/README.md)
* [NCWMS abomination](src/extension/ncwms/README.md)

Makes the following customisations to the geoserver war

* Return 500 errors instead of 200 for API errors so squid doesn't cache API errors
* Enable CORS for GET requests so IMAS can use it for GetFeatureInfo requests

Might be good to use transformation sets as per geonetwork-build to enable CORS instead of a full replacement of web.xml
 so that we get other updates made when upgrading.

### To build

```
mvn clean install -U 
```

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
mvn jetty:run-war -Pjetty -Djetty.port=9090 -Duser.timezone=UTC
```

GeoServer will then be available at:

```
http://localhost:9090
```

### Running using Tomcat and IntelliJ

Set the maven profile to `tomcat`
Create a context file and update to use the required database (an existing populated database or create one with restored data)
Create and populate a data directory or a symlink to a checkout of `geoserver-config`
Install Tomcat 8.5
Install Amazon Corretto version 11
Update the run configuration JRE and Application Server to your Corretto and Tomcat
For console logging, create src/main/webapp/WEB-INF/classes/logging.properties:

```yaml
org.apache.catalina.core.ContainerBase.[Catalina].level=INFO
org.apache.catalina.core.ContainerBase.[Catalina].handlers=java.util.logging.ConsoleHandler
```

GeoServer will then be available at:

```
http://localhost:8080
```

Default admin user is `username=admin`, `password=geoserver`

#### Some requests to help with debugging AODN extensions

Note: for the following requests the required layers need to be present in the database and geoserver-config. 

**NCWMS**

GetMetadata: http://localhost:8080/geoserver/ncwms?layerName=acorn_hourly_avg_rot_qc_timeseries_url%2Fsea_water_velocity&SERVICE=ncwms&REQUEST=GetMetadata&item=timesteps&version=1.0.0&item=layerDetails

GetFeatureInfo: http://localhost:8080/geoserver/ncwms?LAYERS=acorn_hourly_avg_rot_qc_timeseries_url%2Fsea_water_velocity&TRANSPARENT=TRUE&VERSION=1.3.0&FORMAT=text%2Fxml&EXCEPTIONS=application%2Fvnd.ogc.se_xml&TILED=true&SERVICE=ncwms&REQUEST=GetFeatureInfo&STYLES=&QUERYABLE=true&CRS=EPSG%3A4326&NUMCOLORBANDS=253&TIME=2019-07-10T10%3A00%3A00.000Z&BBOX=-42.232422%2C49.614258%2C-10.767578%2C220.385742&INFO_FORMAT=text%2Fxml&QUERY_LAYERS=acorn_hourly_avg_rot_qc_timeseries_url%2Fsea_water_velocity&FEATURE_COUNT=1&SRS=EPSG%3A4326&WIDTH=1943&HEIGHT=358&X=742&Y=238&I=742&J=238&BUFFER=10

GetLegendGraphic: http://localhost:8080/geoserver/ncwms?PALETTE=rainbow&STYLE=vector/rainbow&LEGEND_OPTIONS=forceLabels:on&NUMCOLORBANDS=253&SERVICE=ncwms&REQUEST=GetLegendGraphic&LAYER=acorn_hourly_avg_rot_qc_timeseries_url%2Fsea_water_velocity&FORMAT=image/png&VERSION=1.3.0

GetMap: http://localhost:8080/geoserver/ncwms?LAYERS=imos%3Aargo_profile_map&TRANSPARENT=TRUE&VERSION=1.1.1&FORMAT=image%2Fpng&EXCEPTIONS=application%2Fvnd.ogc.se_xml&TILED=true&SERVICE=ncwms&REQUEST=GetMap&STYLES=&QUERYABLE=true&SRS=EPSG%3A4326&BBOX=-112.5,-67.5,-90,-45&WIDTH=256&HEIGHT=256

**Layer filters**

enabledFilters: http://localhost:8080/geoserver/ows?request=enabledFilters&service=layerFilters&version=1.0.0&workspace=imos&layer=argo_primary_profile_core_low_res_good_qc_data

uniqueValues: http://localhost:8080/geoserver/wms?request=uniqueValues&service=layerFilters&version=1.0.0&layer=imos:argo_profile_map&propertyName=platform_number

WFS: http://localhost:8080/geoserver/ows?typeName=imos:argo_primary_profile_core_low_res_good_qc_data&SERVICE=WFS&outputFormat=csv-restricted-column&REQUEST=GetFeature&VERSION=1.0.0&CQL_FILTER=platform_number%20LIKE%20%271900042%27

**CSV with metadata header**

GetFeature: http://localhost:8080/geoserver/ows?typeName=imos:argo_primary_profile_core_low_res_good_qc_data&SERVICE=WFS&outputFormat=csv-with-metadata-header&REQUEST=GetFeature&VERSION=1.0.0


