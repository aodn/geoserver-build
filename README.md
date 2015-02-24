Geoserver Build
===============

Configures a GeoServer war file with the following;

* The XSLT extension installed
* CSV with metadata header plugin
* Layer filter extension

To Build,
````
git submodule update --init
mvn clean install -U -Dmaven.test.skip=true 
````

