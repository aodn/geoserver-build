package au.org.emii.gogoduck.worker;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;

public class GoGoDuckModule {
    private static final Logger logger = LoggerFactory.getLogger(GoGoDuckModule.class);
    private static final String PROPERTIES_FILE = "config.properties";

    // TODO Should not be hard coded
    private static final String TIME_FIELD = "time";
    private static final String URL_FIELD = "file_url";

    private String profile = null;
    private GoGoDuckSubsetParameters subset = null;
    private UserLog userLog = null;
    private IndexReader indexReader = null;

    private InputStream input = null;
    private Properties properties = new Properties();

    public GoGoDuckModule(String profile, IndexReader indexReader, String subset, UserLog userLog) {
        this.profile = profile;
        this.indexReader = indexReader;
        this.subset = new GoGoDuckSubsetParameters(subset);
        this.userLog = userLog;

        try {
            input = FeatureSourceIndexReader.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE);

            if(input==null){
                throw new GoGoDuckException(String.format("Sorry, unable to find %s", PROPERTIES_FILE));
            }
            // load a properties file
            properties.load(input);
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally{
            if(input!=null){
                try {
                    input.close();
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        }
    }

    public URIList getUriList() throws GoGoDuckException {
        return indexReader.getUriList(profile, TIME_FIELD, URL_FIELD, subset, properties);
    }

    public void postProcess(File file) {
        try {
            String postProcessProperty = String.format("%s.postprocess", profile);
            if (properties.containsKey(postProcessProperty) && properties.getProperty(postProcessProperty).equals("true")) {
                Method method = this.getClass().getDeclaredMethod(String.format("postProcess_%s", postProcessProperty), File.class);
                method.invoke(this, file);
            }
        } catch (Exception e) {
            throw new GoGoDuckException(String.format("Could not post process file '%s'", file.toPath()));
        }
    }

    public void postProcess_srs_oc(File file) {
        postProcess_srs(file);
    }

    public void postProcess_srs(File file) {
        try {
            File tmpFile = File.createTempFile("ncpdq", ".nc");

            List<String> command = new ArrayList<String>();
            command.add(GoGoDuckConfig.ncpdqPath);
            command.add("-O");
            command.add("-U");
            command.add(file.getAbsolutePath());
            command.add(tmpFile.getAbsolutePath());

            logger.info(String.format("Unpacking file (ncpdq) '%s' to '%s'", file.toPath(), tmpFile.toPath()));
            GoGoDuck.execute(command);

            Files.delete(file.toPath());
            Files.move(tmpFile.toPath(), file.toPath());
        } catch (Exception e) {
            throw new GoGoDuckException(String.format("Could not run ncpdq on file '%s'", file.toPath()));
        }
    }

    public List<String> ncksExtraParameters() {
        List<String> ncksExtraParameters = new ArrayList<String>();
        String ncksParametersProperty = String.format("%s.ncks.parameters", profile);
        if (properties.containsKey(ncksParametersProperty)) {
            String ncksParameters[] = properties.getProperty(ncksParametersProperty).split(";", -1);
            for (String ncksParameter : ncksParameters) {
                ncksExtraParameters.add(ncksParameter);
            }
        }
        return ncksExtraParameters;
    }

    public List<Attribute> getGlobalAttributesToUpdate(NetcdfFile nc) {

        try {
            String attributeProperty = String.format("%s.attribute.update", profile);
            if (properties.containsKey(attributeProperty) && properties.getProperty(attributeProperty).equals("true")) {
                Method method = this.getClass().getDeclaredMethod(String.format("getGlobalAttributesToUpdate_%s", attributeProperty), NetcdfFile.class);
                return (List<Attribute>) method.invoke(this, nc);
            }
        } catch (Exception e) {
            throw new GoGoDuckException(String.format("Could not update global attribute for '%s'", profile));
        }

        List<Attribute> newAttributeList = new ArrayList<Attribute>();

        String title = profile;
        try {
            title = nc.findGlobalAttribute("title").getStringValue();

            // Remove time slice from title ('something_a, something_b, 2013-11-20T03:30:00Z' -> 'something_a, something_b')
            title = title.substring(0, title.lastIndexOf(","));
        }
        catch (Exception e) {
            // Don't fail because of this bullshit :)
            logger.warn("Could not find 'title' attribute in result file");
        }

        newAttributeList.add(new Attribute("title",
                String.format("%s, %s, %s",
                        title,
                        subset.get("TIME").start,
                        subset.get("TIME").end)));

        newAttributeList.add(new Attribute("geospatial_lat_min", subset.get("LATITUDE").start));
        newAttributeList.add(new Attribute("geospatial_lat_max", subset.get("LATITUDE").end));

        newAttributeList.add(new Attribute("geospatial_lon_min", subset.get("LONGITUDE").start));
        newAttributeList.add(new Attribute("geospatial_lon_max", subset.get("LONGITUDE").end));

        newAttributeList.add(new Attribute("time_coverage_start", subset.get("TIME").start));
        newAttributeList.add(new Attribute("time_coverage_end", subset.get("TIME").end));

        return newAttributeList;
    }

    public List<Attribute> getGlobalAttributesToUpdate_srs_oc(NetcdfFile nc) {
        return getGlobalAttributesToUpdate_srs(nc);
    }

    public List<Attribute> getGlobalAttributesToUpdate_srs(NetcdfFile nc) {
        List<Attribute> newAttributeList = new ArrayList<Attribute>();

        try {
            String title = title = nc.findGlobalAttribute("title").getStringValue();
            newAttributeList.add(new Attribute("title",
                    String.format("%s, %s, %s",
                            title,
                            subset.get("TIME").start,
                            subset.get("TIME").end)));
        }
        catch (Exception e) {
            // Don't fail because of this bullshit :)
            logger.warn("Could not find 'title' attribute in result file");
        }

        newAttributeList.add(new Attribute("southernmost_latitude", subset.get("LATITUDE").start));
        newAttributeList.add(new Attribute("northernmost_latitude", subset.get("LATITUDE").end));

        newAttributeList.add(new Attribute("westernmost_longitude", subset.get("LONGITUDE").start));
        newAttributeList.add(new Attribute("easternmost_longitude", subset.get("LONGITUDE").end));

        newAttributeList.add(new Attribute("start_time", subset.get("TIME").start));
        newAttributeList.add(new Attribute("stop_time", subset.get("TIME").end));

        return newAttributeList;
    }

    public final void updateMetadata(Path outputFile) {
        try {
            NetcdfFileWriter nc = NetcdfFileWriter.openExisting(outputFile.toAbsolutePath().toString());

            nc.setRedefineMode(true);
            for (Attribute newAttr : getGlobalAttributesToUpdate(nc.getNetcdfFile())) {
                nc.addGroupAttribute(null, newAttr);
            }
            nc.setRedefineMode(false);
            nc.close();
        }
        catch (IOException e) {
            throw new GoGoDuckException(String.format("Failed updating metadata for file '%s': '%s'", outputFile, e.getMessage()));
        }
    }

    public NcksSubsetParameters getNcksSubsetParameters(String location) {
        CoordinateAxis time = null, latitude = null, longitude = null;
        GridDataset gridDs = null;

        try {
            gridDs = GridDataset.open (location);
            List grids = gridDs.getGrids();

            for (int i = 0; i < grids.size(); i++) {
                GeoGrid grid = (GeoGrid) grids.get(i);
                if (time == null) {
                    time = grid.getCoordinateSystem().getTimeAxis();
                }

                if (latitude == null) {
                    latitude = grid.getCoordinateSystem().getYHorizAxis();
                }

                if (longitude == null) {
                    longitude = grid.getCoordinateSystem().getXHorizAxis();
                }

                if (time != null && latitude != null && longitude != null) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new GoGoDuckException(e.getMessage());
        }

        if (time == null || latitude == null || longitude == null) {
            throw new GoGoDuckException(String.format("Unable to retrieve time:%s latitude:%s longitude:%s", time, latitude, longitude));
        }

        NcksSubsetParameters ncksSubsetParameters = new NcksSubsetParameters();
        ncksSubsetParameters.put(latitude.getFullName(), subset.get("LATITUDE"));
        ncksSubsetParameters.put(longitude.getFullName(), subset.get("LONGITUDE"));
        ncksSubsetParameters.addTimeSubset(time.getFullName(), subset.get("TIME"));
        return ncksSubsetParameters;
    }
}
