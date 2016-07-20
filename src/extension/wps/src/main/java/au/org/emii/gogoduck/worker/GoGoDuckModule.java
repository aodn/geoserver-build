package au.org.emii.gogoduck.worker;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;

public class GoGoDuckModule {
    private static final Logger logger = LoggerFactory.getLogger(GoGoDuckModule.class);

    // TODO Should not be hard coded
    private static final String TIME_FIELD = "time";
    private static final String URL_FIELD = "file_url";

    private String profile = null;
    private GoGoDuckSubsetParameters subset = null;
    private UserLog userLog = null;
    private IndexReader indexReader = null;

    public GoGoDuckModule() {

    }

    public GoGoDuckModule(String profile, IndexReader indexReader, String subset, UserLog userLog) {
        init(profile, indexReader, subset, userLog);
    }

    public void init(String profile, IndexReader indexReader, String subset, UserLog userLog) {
        this.profile = profile;
        this.indexReader = indexReader;
        this.subset = new GoGoDuckSubsetParameters(subset);
        this.userLog = userLog;
    }

    public URIList getUriList() throws GoGoDuckException {
        return indexReader.getUriList(profile, TIME_FIELD, URL_FIELD, subset);
    }

    public void postProcess(File file) {
        try {
            if (GoGoDuckConfig.properties.containsValue(profile)) {
                String postProcessProperty = String.format("%s.postprocess", GoGoDuckConfig.getPropertyKeyByValue(profile));
                if (GoGoDuckConfig.properties.containsKey(postProcessProperty) && GoGoDuckConfig.properties.getProperty(postProcessProperty).equals("true")) {
                    Method method = this.getClass().getDeclaredMethod(String.format("postProcess_%s", postProcessProperty), File.class);
                    method.invoke(this, file);
                }
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

        if (GoGoDuckConfig.properties.containsValue(profile)) {
            String ncksParametersProperty = String.format("%s.ncks.parameters", GoGoDuckConfig.getPropertyKeyByValue(profile));
            String ncksParameters[] = GoGoDuckConfig.properties.getProperty(ncksParametersProperty).split(";", -1);
            for (String ncksParameter : ncksParameters) {
                ncksExtraParameters.add(ncksParameter);
            }
        }
        return ncksExtraParameters;
    }

    public final void updateMetadata(Path outputFile) {
        try {
            String location = outputFile.toAbsolutePath().toString();
            NetcdfFileWriter nc = NetcdfFileWriter.openExisting(location);
            GridDataset gridDs =  GridDataset.open (location);

            nc.setRedefineMode(true);
            for (Attribute newAttr : gridDs.getGlobalAttributes()) {
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
