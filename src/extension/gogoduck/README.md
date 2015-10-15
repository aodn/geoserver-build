# GoGoDuck

GoGoDuck is a simple NetCDF file aggregator, using nco tools (ncks, ncrcat, ncatted, etc).

## Requirements

 * nco tools, a recent version! (>= 4.3.4)
 * java/maven

## Usage:

To build:
```
$ mvn clean install
```

To run:
```
$ mvn exec:java -Dexec.mainClass=au.org.emii.gogoduck.worker.Main
```

## On IMOS Data

### ACORN

Running on some ACORN data (rot qc):
```
$ mvn exec:java -Dexec.mainClass=au.org.emii.gogoduck.worker.Main -Dexec.args="-g http://geoserver-123.aodn.org.au/geoserver -p acorn_hourly_avg_rot_qc_timeseries_url -s TIME,2013-11-20T00:30:00.000Z,2013-11-20T10:30:00.000Z;LATITUDE,-33.433849,-32.150743;LONGITUDE,114.15197,115.741219 -o output.nc"
```

### GSLA

Running on GSLA data:
```
$ mvn exec:java -Dexec.mainClass=au.org.emii.gogoduck.worker.Main -Dexec.args="-g http://geoserver-123.aodn.org.au/geoserver -p gsla_nrt00_timeseries_url -s TIME,2011-10-10T00:00:00.000Z,2011-10-20T00:00:00.000Z;LATITUDE,-33.433849,-32.150743;LONGITUDE,114.15197,115.741219 -o output.nc"
```

### CARS

When running on CARS data, it handles one big NetCDF file. If it runs with no
opendap mounted, such as on your development machine - it'll be slow! However
if it runs when opendap is mounted, things will be significantly faster.

Running on CARS data:
```
$ mvn exec:java -Dexec.mainClass=au.org.emii.gogoduck.worker.Main -Dexec.args="-g http://geoserver-123.aodn.org.au/geoserver -p cars_world_monthly -s TIME,2009-01-01T00:00:00.000Z,2009-12-25T23:04:00.000Z;LATITUDE,-33.433849,-32.150743;LONGITUDE,114.15197,115.741219 -o output.nc"
```

Running on CARS data with depth (notice the floating point for the depth parameter):
```
$ mvn exec:java -Dexec.mainClass=au.org.emii.gogoduck.worker.Main -Dexec.args="-g http://geoserver-123.aodn.org.au/geoserver -p cars_world_monthly -s TIME,2009-01-01T00:00:00.000Z,2009-12-25T23:04:00.000Z;LATITUDE,-33.433849,-32.150743;LONGITUDE,114.15197,115.741219;DEPTH,0.0,100.0 -o output.nc"
```

### SRS

Running on SRS data (big files!):
```
$ mvn exec:java -Dexec.mainClass=au.org.emii.gogoduck.worker.Main -Dexec.args="-g http://geoserver-123.aodn.org.au/geoserver -p srs_sst_l3s_1d_dn_gridded_url -s TIME,2014-10-10T00:00:00.000Z,2014-10-12T00:00:00.000Z;LATITUDE,-33.433849,-32.150743;LONGITUDE,114.15197,115.741219 -o output.nc"
```

## Running as WPS

In order to run as a WPS request, you can run the following:
```
$ curl --data @doc/wps-acorn.xml --header "Expect:" --header "Content-Type: application/xml" http://localhost:8080/geoserver/wps
$ curl --data @doc/wps-gsla.xml --header "Expect:" --header "Content-Type: application/xml" http://localhost:8080/geoserver/wps
$ curl --data @doc/wps-srs.xml --header "Expect:" --header "Content-Type: application/xml" http://localhost:8080/geoserver/wps
```

## Unit Tests

Run:
```
$ mvn test
```

