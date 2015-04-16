Geoserver Build
===============

Configures a GeoServer war file with the following;

* The XSLT extension installed
* CSV with metadata header plugin
* Layer filter extension

To build:

```
git submodule update --init
mvn clean install -U -Dmaven.test.skip=true 
```

To update the submodule(s), e.g.:

```
cd src/extension/geoserver-layer-filter-extension/
git pull origin master 
cd -
git status
git add src/extension/geoserver-layer-filter-extension
git commit -m "update filter plugin extension"
git push origin master
```