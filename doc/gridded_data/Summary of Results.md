## Support for Gridded (NetCDF) Data in GeoServer

### Background

NCWMS abomination not an OGC compliant service - quick and dirty solution for data migration - thredds already modified to source files from s3
Requires configuration and deployment of thredds with geoserver along with indexing of files by harvesters.

GeoServer has some gridded NetCDF file support including (as discovered at FOSS4G) support for serving collections of NetCDF files as a mosaic.

Can we just use that - or if not what work would be required to it to do so.

OGC Compliant service - easier for Contributors to setup and use - one less required service

### Web Coverage Service

Web Coverage Service - The OGC Web Coverage Service (WCS) supports electronic retrieval of geospatial data as
"coverages" – that is, digital geospatial information representing space/time-varying phenomena.

WFS for gridded data!

As for WFS a WCS in GeoServer can be visualised using WMS - as a raster image

### NetCDF Plugin

Adds support for sourcing coverage data from a NetCDF file containing gridded data following CF conventions (NetCDF Coverage store). Can be accessed via WCS 
or visualised using WMS

#### Example - CARS

CARS_2009_Australia_Weekly/sea_water_temperature

#### GeoServer NetCDF Store Implementation

[NetCDF versus Coverage ](http://geoserver.geo-solutions.it/multidim/en/netcdf/netcdf_basics.html#netcdf-file-structure-in-geoserver)

Anciliary files:

 * H2 or Postgres db (Postgres more useful for mosaic) for indexing dimensions (the_geom, image_index, [time], [elevation]) for each coverage (variable) in the file
 * *.idx file for slice index (imageindex, time_index, elevation_index, variable_name) - maps image_index or slice to netcdf variable dimension values
 - last mod date checked against source file date to see if indexes need to be updated  - not  sure why this isn't included in the
  indexing dimensions store - access by image index alone doesn't make sense?
 * origin.txt - contains source file path  - not used by anything

Can use an indexer file to control variables indexed, schema used and coverage name mapping. Generated if not present/specified - name of source file.xml

Can use mddatastore.properties in source file directory or netcdf_data_dir directory to specify a postgres datastore for the dimensions index (useful when mosaicing - see below)

#### Notes

 * Two dimensional non-independent latitude-longitude coordinate variables aren’t currently supported.  This is what ACORN SeaSonde datasets use. 
Would need to add support for these types of datasets.  NCWMS supports these. Refer 
[Fast regridding of large, complex geospatial datasets](http://centaur.reading.ac.uk/19928/1/19928Com_geo_Complex_Grids_final.pdf) by Jon Blower
and A. Clegg for a comparison of algorithms to do this, including the one chosen for ncWMS.  

 * Must follow COARDS convention for dimension order ie. time (where present), elevation (where present), latitude, longitude.
So I couldn't load our current CARS datasets - had to reorder dimensions using ncpdq before I could do this. 
We would either need to reprocess the CARS products to obtain the correct ordering or modify the NetCDF plugin to handle dimensions in any order

 * NetCDF input can recognise "LONGITUDE", "longitude" as a longitude but can only recognise "depth" as an elevation not "DEPTH".
This resulted in the CARS_2009_Australia_Weekly coverage having an extra DEPTH dimension rather than the more standard elevation dimension

 * Need to check if DEPTH was recognised as an elevation whether negative elevation values would apply. Examples seem to suggest they wouldn't.
NCWMS does this.  Couldn't check this as couldn't find a utility that could change the name of a coordinate variable in netcdf 4 files that worked.

 * Uses variable names by default as coverage names when indexing without indexing instructions - using standard names may be better - defaulted band name to GRAY_INDEX - not sure why  I ended up changing it

 * image_index increases for each variable - thought they were meant to be independent?
 
 * didn't pick up min/max for coverage bands automatically - a good thing?

#### Styling

Styled using an SLD.  Use [RasterSymbolizer](http://docs.geoserver.org/latest/en/user/styling/sld-reference/rastersymbolizer.html) to style raster data.

Color map
Channel Selection
CQL fo dynamic colormaps

Dynamic colormaps

WPS transformations - more on this later

 * Have to build fillers into the colormap!
 * SLD rendered arrows can move a when dragging the map amount depending on the size of the arrows.
 * Rendering arrows on a standard colormap style can affect the colour of the colormap layer.
 * Some issues clicking on layers.  It normally works and gives values from underlying coverage bands but in some areas on some layers it can give an error.


### ImageMosaic extension

Adds support for creating a coverage from a collection of compatible files supported as coverage sources by GeoServer (including gridded NetCDF files).

#### Configuration

indexer.xml - main mosaic indexing config file - schema for index file - what to index etc
datastore.properties - defines datastore where index will be written - defaults to shapefile if not present
netdf-indexer.xml - netcdf indexing file to be used when indexing netcdf granules
netcdf-datastore.properties - defines datastore for storing netcdf dimension index for all granules

#### Examples - ACORN / SRS

srs_sst_l3s_1d_ngt_gridded/sea_surface_temperature

Doesn't automatically unpack SRS like ncwms - runs OK on unpacked data. 

acorn_hourly_avg_rot_qc/sea_water_velocity

Doesn't Support SeaSonde

#### Coverage Views

Used to define a view made of different bands or channels (components of structured coverage values e.g. red, green, blue or eastward_sea_water_velocity,
 northward_sea_water_velocity) originally available inside coverages (either bands/channels of the same coverage or different coverages) of the same Coverage Store.

For a NetCDF store, variables are published as separate coverages.  A coverage view can be used to create a coverage containing a number of variables or bands
such as eastward_sea_water_velocity and northward_sea_water_velocity.  This allows rendering of a combined layer representing sea_water_velocity for example.

#### Dynamic colormaps

Community plugin - allows colormaps to be defined in terms of percentages rather than specifying values.

Requires collection of statistics min/max values etc.  Instructions use gdalinfo - spent some time on this but was unable to get this to work.

#### External mosaics

UseExistingSchema parameter - turns off automatic creation of granule catalog tables (must be present) 
and indexing of granules on creation of the store - enables manual management of granules? - some issues

Use with CanBeEmpty parameter to enable creation of empty coverages which can then be manually populated.

netcdf ancilliary files are created as required (may be because I was just inserting records in granules catalog - there is a REST i/f to add granules - but includes upload of file)

Need to disable in memory cache for GeoServer to automatically pick up changes

Need to externally manage netcdf ancilliary files too?

#### Property collectors

Allow time/elevation or custom dimensions to be sourced from the file_name (uses regular expressions) - eg. could add site_code dimension, revision_date dimension

Custom dimensions can be populated externally?

#### Implementation

The dimensions included in each file are indexed into a granule catalog which can be searched to find files satisfying filter criteria. 
Requests are delegated to data stores created to access the files and responses aggregated and returned.

#### Issues

 * Examples for using netcdf indexer show multiple variables using a "default" schema with only one definition of that schema being required.
This didn't work for me.  I had to define schemas for each variable (all with the same contents).  Would be better if I only had to define it once.

 * Separate table created for each variable in both the granule catalog and the netcdf dimensions index -> can't index more than one coverage
 with the same variables in the one schema

 * Couldn't put granule catalog and netcdf dimension indexing in same schema - same names - caused an exception
 
 * Relative path option only used in granule catalog, netcdf dimensions index still uses absolute path. 
Relative path is from directory in which indexer.xml is placed to data.

 * Granule catalog and netcdf dimensions indexes ended up having the the same information in them (couldn't set them to the same destination)

 * Easy to corrupt a datastore/layer - if misconfigure and it falls over - incomplete setup of supporting files prevent the store from 
being created again when fixing the config - have to clean it up manually

 * Automatically generated files, tables etc aren't deleted when deleting the store

 * Can add new granules by adding to table but get duplicate netcdf dimension indexing records - still appears to work though as
 coverage operation always take the first slice satisfying the criteria need to check rest harvesting service to see if this happens there as well

 * No magnitude transformation - need to add a WPS tranform to enable a magnitude color map for acorn - not one of the provided transformations (multiply is)

 * Generates spurious files in indexing config directory - not sure what they are for/whether they are required - ones a copy of indexer.xml - so probably not

### Other Features

 * WCS api
 * NetCDF output plugin
 * REST I/F

### Work required

 * Add support ACORN SeaSonde (NetCDF plugin)
 * Fix issues with CARS (change dimension order or support non-COARDS dimension order) (NetCDF plugin)
 * WPS transform - magnitude of north/east components for rendering ACORN as it is currently rendered
 * Support for S3 storage (NetCDF plugin)
 * All auxiliary files -> db?
 * Fix issues 
 * Revisit build process again
 * Community engagement if trying to contribute back to core

### Summary

Sounds good in theory but would be a substantial investment. Pros - netcdf store - relatively few classes.  Perhaps more complicated than we need - mosaic/netcdf store separation.
Need to fit into GeoServer coverage framework.
Stuff that just works in ncWMS would need to be made to work in GeoServer - e.g. depth is a negative elevation

### Risks

 * lots of issues
 * existing framework limited in what it can handle

### Options

 * invest in GeoServer mosaic/netcdf plugins
 * imos specific mosaic/netcdf plugins
 * write an leaner netcdf collection plugin
 * use ncwms2

### To Investigate

REST interface for harvesting/removing granules/creating/deleting stores
Configure cars to write dimension index to postgres so can confirm what it contains
Using origName to rename generated tables - what does this imply for coverage names/band names
Do we actually need to have EO extensions installed for what we want to do?  Not sure that we do.
WPS transformation for rendering colormap of sea water velocity magnitude




