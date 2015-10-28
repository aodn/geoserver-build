Geoserver Build
===============

Configures a GeoServer war file with the following;

* The XSLT extension installed
* CSV with metadata header plugin
* Layer filter extension

To build:

```
mvn clean install -U 
```

To run:

```
cd src/main
mvn jetty:run
```

GeoServer will then be available at:

```
http://localhost:8080
```
