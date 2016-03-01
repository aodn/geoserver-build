This directory contains the results of the geoserver netcdf gridded data support investigation and supporting files.  In particular it contains:

 * a maven pom that can be used to build geoserver with all netcdf related plugins
 * sample imagemosaic indexing files for indexing a subset of acorn_hourly_avg_rot_qc and srs_sst_l3s_1d_ngt_gridded files
 * sample slds that can be used to style acorn, SRS and CARS layers
 * a summary of the netcdf support in GeoServer that may be used to replace ncWMS functionality, limitations and issues and work required

To build geoserver with required plugins

    mvn clean install

To run geoserver

    mvn jetty:run -Duser.timezone=GMT

You can then go to http://localhost:8080/geoserver and login using username/password admin/geoserver
 
To load CARS_2009_Australia_Weekly.nc using a netcdf store

 * Download http://thredds.aodn.org.au/thredds/fileServer/climatology/CARS/2009/eMII-product/CARS2009_Australia_weekly.nc and copy to  a local directory
 * modify the dimension order to be required dimension order for the netcdf plugin using ncpdq (install first) e.g.
 
    ncpdq -a DAY_OF_YEAR,DEPTH,LONGITUDE,LATITUDE CARS2009_Australia_weekly.nc CARS2009_Australia_weekly_coards.nc

 * load styles/rainbow-celsius.sld style into geoserver (styles option)
 * create a new netcdf store 
 * name store and select output of ncpdq command above e.g. CARS2009_Australia_weekly_coards.nc
 * go to publish tab - select current_vectors_ucur_vcur as the default style
 * go to dimensions tab - enable and select list and biggest domain value for time and list and smallest domain value for depth
 * can then display layer preview/request getcapabilities to see indexed time/depth values

To use the mosaic indexing configuration files for loading acorn_hourly_avg_rot_qc and srs_sst_l3s_1d_ngt_gridded files as imagemosaics as detailed below:

 * create a geoserver_gridded database in postgres owned by a geoserver_gridded user with password geoserver_gridded
 * install postgis in database geoserver_gridded

To load a subset of SRS data as an imagemosaic 

 * copy a subset of SRS data to a local directory e.g. from http://thredds.aodn.org.au/thredds/catalog/IMOS/SRS/sst/ghrsst/L3S-1d/ngt/2016/catalog.html
 * unpack files using something like - mkdir unpacked ; for file in *.nc ; do ncpdq -P upk $file unpacked/$file ; done
 * load styles/rainbow.sld into geoserver
 * create srs_sst_l3s_1d_ngt_gridded_mosaic and srs_sst_l3s_1d_ngt_gridded_netcdf schemas owned by geoserver_gridded in geoserver_gridded database
 * modify mosaic_config_files/srs_sst_l3s_1d_ngt_gridded/indexer.xml IndexingDirectories parameter to point to the directory where the unpacked SRS files are
 * in geoserver, create a new image mosaic raster store
 * enter a name for the raster store and select the directory containing the indexing configuration (i.e. mosaic_config_files/srs_sst_l3s_1d_ngt_gridded)
 * go to the publishing tab and select rainbow as the default style
 * go to dimensions tab - enable and select list and biggest domain value for time
 * can then display layer preview/request getcapabilities to see indexed time values

To load a subset of ACORN data as an imagemosaic 

 * copy a subset of ACORN Wera data to a local directory e.g. from http://thredds.aodn.org.au/thredds/catalog/IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2014/06/06/catalog.html
 * load styles/current_vectors_ucur_vcur.sld into geoserver
 * create acorn_hourly_avg_rot_qc_mosaic and acorn_hourly_avg_rot_qc_netcdf schemas owned by geoserver_gridded in geoserver_gridded database
 * modify mosaic_config_files/acorn_hourly_avg_rot_qc/indexer.xml IndexingDirectories parameter to point to the directory where the ACORN files are
 * in geoserver, create a new image mosaic raster store
 * enter a name for the raster store and select the directory containing the indexing configuration (i.e. mosaic_config_files/acorn_hourly_avg_rot_qc)
 * go to the publishing tab and select current_vectors_ucur_vcur as the default style
 * go to dimensions tab - enable and select list and biggest domain value for time
 * can then display layer preview/request getcapabilities to see indexed time values

