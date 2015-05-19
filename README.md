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


mvn -P wps install -Dmaven.test.skip=true
```

Manual tomcat deploy (something like)
```
sudo -s
rm /var/lib/tomcat7/webapps/geoserver* -rf
ls /var/lib/tomcat7/webapps/
cp ./src/geoserver/src/web/app/target/geoserver.war  /var/lib/tomcat7/webapps/ -i
ls /var/lib/tomcat7/webapps/
/etc/init.d/tomcat7 start
/etc/init.d/tomcat7 stop
cp /home/meteo/imos/projects/ncdfgenerator/target/netcdf-output-0.0.1-SNAPSHOT.jar /var/lib/tomcat7/webapps/geoserver/WEB-INF/lib/ -i
echo > /var/log/tomcat7/catalina.out
/etc/init.d/tomcat7 start
less /var/log/tomcat7/catalina.out

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
