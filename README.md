# Geoserver Build

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
