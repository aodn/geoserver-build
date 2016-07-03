# Abomination ncwms

This plugin mimicks ncwms operation in geoserver by proxying requests to a
thredds server using WFS as an index for `time` <-> `url`.

## How does it work?

ncwms provides wms over netcdf files. In addition it also provides the
following functions:
 * Get all dates with data
 * For a specific date - returns the times with data

ncwms knows that because it indexes the files periodically.

The abomination ncwms can know all of that because we have `_url` layers which
map between time and filename. So we use this reference layer to find the
information we need.

### Mimicking date/time queries

To mimick the request of get all dates with data we simply ask for all features
sorted by date for the given layer (`_url`) and then we just uniqify the dates.

To mimick the request of getting all times for a specific date, we craft a WFS
CQL query and ask just for a specific day. The CQL query will look similar to:
```
"TIME >= that_day AND TIME < next_day",
```

### Mimicking WMS

That's the easy part. Given we know what netcdf file we need to serve wms for,
we can just proxy it to a thredds server that serves this file.

All incoming WMS requests should have a `TIME=` string. Otherwise we use the
latest time for the layer. We can obtain it by sorting by time in a descending
order and just take the first feature, that would be the latest `TIME`.

Thredds has full ncwms capabilities per file, for instance:
http://thredds.aodn.org.au/thredds/wms/IMOS/ACORN/gridded_1h-avg-current-map_non-QC/CBG/2015/12/10/IMOS_ACORN_V_20151210T003000Z_CBG_FV00_1-hour-avg.nc?service=WMS&version=1.3.0&request=GetCapabilities

So the wms endpoint for the file `IMOS/ACORN/gridded_1h-avg-current-map_non-QC/CBG/2015/12/10/IMOS_ACORN_V_20151210T003000Z_CBG_FV00_1-hour-avg.nc` is at:
http://thredds.aodn.org.au/thredds/wms/IMOS/ACORN/gridded_1h-avg-current-map_non-QC/CBG/2015/12/10/IMOS_ACORN_V_20151210T003000Z_CBG_FV00_1-hour-avg.nc

The abomination ncwms plugin will just proxy all requests to the wms endpoint of
the corresponding file.

### Caveats

You must pass `SERVICE=ncwms` instead of `SERVICE=wms`, otherwise the key/value
pair parsing will be delegated to the original geoserver wms plugin and we
don't want that because it'll parse them in a wrong way.

## Metadata configuration

Usually the metadata configuration for ncwms would look like:
```
<gmd:onLine>
    <gmd:CI_OnlineResource>
        <gmd:linkage>
            <gmd:URL>http://ncwms.aodn.org.au/ncwms/wms</gmd:URL>
        </gmd:linkage>
        <gmd:protocol>
            <gco:CharacterString>OGC:WMS-1.1.1-http-get-map</gco:CharacterString>
        </gmd:protocol>
        <gmd:name>
            <gco:CharacterString>ACORN_ROT_QT/sea_water_velocity</gco:CharacterString>
        </gmd:name>
        <gmd:description>
            <gco:CharacterString>Rottnest shelf - sea water velocity (QC data)</gco:CharacterString>
        </gmd:description>
    </gmd:CI_OnlineResource>
</gmd:onLine>
```

We will need to configure both the `linkage` and `name` parameters. For
`linkage` we will need to point to the abominationncwms endpoint on geoserver.
For example:
http://geoserver-123.aodn.org.au/geoserver/ncwms

The `name` element is built as follows:
```
WORKSPACE:URL_LAYER_NAME#TIME_FIELD,URL_FIELD/NETCDF_VARIABLE
```

A more concrete example for `name` is:
```
imos:acorn_hourly_avg_rot_qc_timeseries_url#time,file_url/sea_water_velocity
```
We can omit `WORKSPACE` if it's the default one. We can also omit
`TIME_FIELD`/`URL_FIELD` if they are the default. The default for `TIME_FIELD`
is `time` and the default for `URL_FIELD` is `file_url`. Then it becomes:
```
acorn_hourly_avg_rot_qc_timeseries_url/sea_water_velocity
```

And last, a real example of an online resource:
```
<gmd:onLine>
    <gmd:CI_OnlineResource>
        <gmd:linkage>
            <gmd:URL>http://geoserver-123.aodn.org.au/geoserver/ncwms</gmd:URL>
        </gmd:linkage>
        <gmd:protocol>
            <gco:CharacterString>IMOS:NCWMS--proto</gco:CharacterString>
        </gmd:protocol>
        <gmd:name>
            <gco:CharacterString>acorn_hourly_avg_rot_qc_timeseries_url/sea_water_velocity</gco:CharacterString>
        </gmd:name>
        <gmd:description>
            <gco:CharacterString>Rottnest shelf - sea water velocity (QC data)</gco:CharacterString>
        </gmd:description>
    </gmd:CI_OnlineResource>
</gmd:onLine>
```
