# GoGoDuck

GoGoDuck is a simple NetCDF file aggregator

## Requirements

 * netcdf c library and dependencies (4.3.1 or above)
 * java/maven

## Usage:

To run:
```
$ mvn clean install -DskipITs
```

Submitting a request using Geoserver:
```
1. Navigate to http://localhost:8080/web/?wicket:bookmarkablePage=:org.geoserver.wps.web.WPSRequestBuilder after [running using jetty] (https://github.com/aodn/geoserver-build#running-using-jetty)
2. Choose process: gs:GoGoDuck
3. Choose format: application/x-netcdf
```

### Running as a WPS service. (simulating a portal)
   
   In order to run as a WPS request, you can run the following:
   
   ```
   $ curl --data @wps-gogoduck.xml --header "Expect:" --header "Content-Type: application/xml" http://localhost:8080/wps
   ```

## Unit Tests

Run:
```
$ mvn test
```

## Configuring GoGoDuck downloads via the Portal

### Metadata Record

A typical Geonetwork record to enable a GoGoDuck download via the Portal will include the following online resource:

```
<gmd:onLine>
  <gmd:CI_OnlineResource>
    <gmd:linkage>
      <gmd:URL> [GEOSERVER_OWS_URL] </gmd:URL>
    </gmd:linkage>
    <gmd:protocol>
      <gco:CharacterString>OGC:WPS--gogoduck</gco:CharacterString>
    </gmd:protocol>
    <gmd:name>
      <gco:CharacterString> [LAYER_NAME] </gco:CharacterString>
    </gmd:name>
    <gmd:description>
      <gco:CharacterString>The GoGoDuck subsets and aggregates gridded data.  Data is returned as a NetCDF file or CSV</gco:CharacterString>
    </gmd:description>
  </gmd:CI_OnlineResource>
</gmd:onLine>
```

A more concrete example would look like:

```
<gmd:onLine>
  <gmd:CI_OnlineResource>
    <gmd:linkage>
      <gmd:URL>http://geoserver-123.aodn.org.au/geoserver/ows</gmd:URL>
    </gmd:linkage>
    <gmd:protocol>
      <gco:CharacterString>OGC:WPS--gogoduck</gco:CharacterString>
    </gmd:protocol>
    <gmd:name>
      <gco:CharacterString>acorn_hourly_avg_rot_nonqc_timeseries_url</gco:CharacterString>
    </gmd:name>
    <gmd:description>
      <gco:CharacterString>The GoGoDuck subsets and aggregates gridded data.  Data is returned as a NetCDF file or CSV</gco:CharacterString>
    </gmd:description>
  </gmd:CI_OnlineResource>
</gmd:onLine>
```

### Configuring Gridded NetCDF Aggregation settings

General Gridded NetCDF Aggregator settings are configured in wps/gogoduck.xml in the geoserver data directory for example as follows:

```
<?xml version="1.0"?>
<gogoduck>
  <!-- index reader settings -->
  <urlSubstitution key="^">http://data.aodn.org.au/</urlSubstitution>
  <fileUrlField>file_url</fileUrlField>
  <timeField>time</timeField>
  <sizeField>size</sizeField>
  <fileLimit>100000</fileLimit>
  <fileSizeLimit>20000000000</fileSizeLimit>
  <!-- download settings -->
  <storageLimit>200000000</storageLimit>
  <connectTimeOut>10000</connectTimeOut>
  <readTimeOut>5000</readTimeOut>
  <threadCount>8</threadCount>
  <!-- aggregation templates -->
  <templates>
    <template match="imos:srs.*">srs</template>
    <template match=".*">default</template>
  </templates>
</gogoduck>
```

### Index reader settings

These settings are used when looking up files matching the aggregation criteria in the index as follows:

| Element | Description |
| --- | --- |
| urlSubstitution | this element specifies a substitution to be applied to url's matching the key - values matching the key are replaced with the text of the element |
| fileUrlField | the index column used to source the url field from the index |
| time | the index column used to source the time step included in the file |
| sizeField | the index column used to source the file size |
| fileLimit | the maximum number of files which can be selected for aggregation |
| fileSizeLimit | the maximum total size of files which can be downloaded for aggregation |

### Download settings

These settings are used when downloading files for aggregation as follows:

| Element | Description |
| --- | --- |
| storageLimit | use up to this amount of local storage space to buffer files for aggregation |
| connectTimeOut | the time to wait for a download connection to be made |
| readTimeOut | the time to wait for download data to be sent |
| threadCount | the number of threads to be used to download files |

### Controlling aggregation output

The default behaviour of the NetCDF aggregator is to copy the global attributes and subsetted dimension and variable definitions
from the first file aggregated to the output file prior to aggregating data.   This behaviour can be modified 
using the templates section of the gogoduck configuration file to specify what aggregation override configuration
should be applied to selected layers.

To determine the aggregation configuration to be used for a given layer - each template element in the templates element is 
tested for a match against the regular expression specified in the match attribute.  The name of the first matching template
is used to source aggregation overrides by looking for a file with this name.  If no match
is found no overrides will be applied.

An example aggregation overrides (template) file is as follows:

```
<?xml version="1.0" encoding="UTF-8"?>
<template>
  <attributes>
    <remove name="start_time"/>
    <remove name="stop_time"/>
    <attribute name="time_coverage_start" type="String" value="${TIME_START}"/>
    <attribute name="time_coverage_end" type="String" value="${TIME_END}"/>
    <attribute name="title" match=".*" type="String" value="${0}, ${TIME_START}, ${TIME_END}"/>
    <attribute name="southernmost_latitude" type="Double" value="${LAT_MIN}"/>
    <attribute name="northernmost_latitude" type="Double" value="${LAT_MAX}"/>
    <attribute name="westernmost_longitude" type="Double" value="${LON_MIN}"/>
    <attribute name="easternmost_longitude" type="Double" value="${LON_MAX}"/>
  </attributes>
  <variables>
    <variable name="time"/>
    <variable name="lat"/>
    <variable name="lon"/>
    <variable name="dt_analysis" type="Float"/>
    <variable name="l2p_flags"/>
    <variable name="quality_level"/>
    <variable name="satellite_zenith_angle" type="Float"/>

    <variable name="sea_surface_temperature" type="Float">
      <attribute name="_FillValue" type="Double" value="9.96920996838687e+36"/>
      <attribute name="valid_min" type="Double" value="0.0"/>
      <attribute name="valid_max" type="Double" value="350.0"/>
    </variable>

    <variable name="sses_bias" type="Float">
      <attribute name="valid_range" type="Double">
        <value>0.0</value>
        <value>350.0</value>
      </attribute>
    </variable>
    
    <variable name="sses_count" type="Float"/>
    <variable name="sses_standard_deviation" type="Float"/>

    <variable name="sst_dtime" type="Float"/>
  </variables>
</template>
```

##### Remove element

A remove element is used to remove a global attribute when copying attributes from the first aggregation file

##### Attribute element

Attribute elements specify an attribute to be added or whose value should be replaced when copying 
attributes from the first aggregation file.

| Attribute | Description |
| --- | --- |
| name | the name of the attribute to add or replace |
| type | the type of the value to be added or used in the replacement |
| match | a java regular expression to be used to capture portions of an existing attributes value (refer [java Pattern class](http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html) and in particular (Groups and Capturing)[http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#cg])|
| value | The value to use for the attribute after substitution value - ${substitution value} - replacements have been made.  See below for valid substitution values |

##### Substitution values

The following substitution values can be used in an attribute value attribute:

| Substitution value | Description |
| --- | --- |
| TIME_START | The requested start time (UTC) for the aggregated dataset in ISO8601 format |
| TIME_END | The requested end time (UTC) of the aggregated dataset in ISO8601 format |
| LAT_MIN | The minimum latitude value of the aggregated dataset |
| LAT_MAX | The maximum latitude value of the aggregated dataset |
| LON_MIN | The minimum longitude value of the aggregated dataset |
| LON_MAX | The maximum longitude value of the aggregated dataset |
| [0-9] | The value of a captured group specified using the match attribute to select portions of the previous value of the attribute |

#### Variables element

The variables element contains the list of variables to be included in the aggregation along with any modified 
type, filler value, valid min, valid max, valid range or missing values (other variable attribute modifications are not supported)

