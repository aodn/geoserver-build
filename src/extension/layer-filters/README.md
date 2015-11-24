GeoServer Layer Filter Extension
================================
This extension provides the ability to configure layers to present properties by which they
can be subset.

## Installation

You can download the latest binary [here](https://jenkins.aodn.org.au/)
and unzip the archive to the `lib` directory of your geoserver web application. Restart GeoServer and you're away.

## Building

You will need [Maven](http://maven.apache.org/) to build the extension.
Clone this repo and run `$ mvn package`

## How It Works

There are three components to the extension.

* A page to configure properties by which a layer can be subset
* An OWS service that returns the configured properties as XML
* A WFS service that excludes columns that have been configured as not available for download

### Layer Property Configuration

Once the extension is installed, log in as an admin user and toward the bottom of the main menu
on the left there should an item **WMS Layer Filters**.

From the list select the layer you wish to configure filters for. Note that you can filter the results
using the search bar on the top right of the list of layers.

#### Configuration Properties

##### Enabled

For a filter to be returned it must first be enabled. Any information against filters that do not have _Enabled_
checked is discarded.

##### Property

The name of the layer property

##### Type

The type of the property as translated by GeoServer. For use with AODN portals you may want to modify `Geometry` types
to `BoundingBox` and `Timestamp` to `Date` or `DateTime`

##### Filter Label

The human presentable name of the filter

##### Not Visualised

These are filters that do not change the appearance of a WMS layer in an AODN portal but are included in the downloaded
data

##### Exclude From Download

Properties that can be filtered on but are not included in the set of downloaded data. This is useful for boolean properties.
*n.b. For a property to be excluded from download it must also be enabled*

### The OWS Service XML

The service can be accessed at `http://<host>/geoserver/ows?request=enabledFilters&service=layerFilters&version=1.0.0&workspace=imos&layer=argo_profile_download_data`
The service expects a workspace and layer name. If either is missing behaviour is undefined.

#### Example XML Response

```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<filters>
    <filter>
        <name>deployment_name</name>
        <type>String</type>
        <label>Did</label>
        <visualised>true</visualised>
        <values>
            <value>MadmaxTest2011</value>
            <value>PerthCanyonA20140213</value>
            <value>PerthCanyonB20140213</value>
            <value>TS11</value>
            <value>TalismanSaberA20130706</value>
            <value>TalismanSaberB20130706</value>
        </values>
    </filter>
    <filter>
        <name>platform_type</name>
        <type>String</type>
        <label>This</label>
        <visualised>true</visualised>
        <values>
            <value>slocum glider</value>
        </values>
    </filter>
    <filter>
        <name>platform_code</name>
        <type>String</type>
        <label>Work?</label>
        <visualised>true</visualised>
        <values>
            <value>SL085</value>
            <value>SL090</value>
        </values>
    </filter>
    <filter>
        <name>colour</name>
        <type>String</type>
        <label>Colour</label>
        <visualised>false</visualised>
        <values>
            <value>#040727</value>
            <value>#6189CC</value>
            <value>#772348</value>
            <value>#788884</value>
            <value>#AAF0AC</value>
            <value>#B355E0</value>
        </values>
    </filter>
    <filter>
        <name>geom</name>
        <type>BoundingBox</type>
        <label>Bounding Box</label>
        <visualised>true</visualised>
        <values/>
    </filter>
</filters>
```

### The WFS Service

The service is requested using the `csv-restricted-column` format, for example `http://<host>/geoserver/ows?typeName=argo_profile_download_data&SERVICE=WFS&outputFormat=csv-restricted-column&REQUEST=GetFeature&VERSION=1.0.0&CQL_FILTER=platform_number%20LIKE%20%271900042%27`

The response is identical to the CSV output format WFS response but excludes columns configured as not available for download.

## Useful Documentation

[GeoApi Java Docs](http://www.geoapi.org/2.2/javadoc/index.html)

[Geo Tools Java Docs](http://docs.geotools.org/stable/javadocs/)
