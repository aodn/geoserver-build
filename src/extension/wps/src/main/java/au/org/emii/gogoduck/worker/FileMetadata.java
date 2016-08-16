package au.org.emii.gogoduck.worker;

import au.org.emii.gogoduck.exception.GoGoDuckException;
import au.org.emii.utils.GoGoDuckConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;

public class FileMetadata {
    private static final Logger logger = LoggerFactory.getLogger(FileMetadata.class);

    @Autowired
    ServletContext context;

    private String profile;
    private GoGoDuckSubsetParameters subset;
    private IndexReader indexReader;
    private GoGoDuckConfig goGoDuckConfig;

    private Boolean unpack = null;
    private List<String> extraParameters = null;
    private NcksSubsetParameters subsetParameters = null;
    private CoordinateAxis time = null, latitude = null, longitude = null;

    public FileMetadata(String profile, IndexReader indexReader, String subset, GoGoDuckConfig goGoDuckConfig) {
        this.profile = profile;
        this.indexReader = indexReader;
        this.subset = new GoGoDuckSubsetParameters(subset);
        this.goGoDuckConfig = goGoDuckConfig;
    }

    public URIList getUriList() throws GoGoDuckException {
        return indexReader.getUriList(profile, goGoDuckConfig.getTimeField(), goGoDuckConfig.getFileUrlField(), subset);
    }

    public boolean unpackNetcdf() {
        return unpack;
    }

    public boolean isTimeUnlimited() {
        return time.isUnlimited();
    }

    public void load(File file) {
        boolean fileContainPackedVariables = false;
        GridDataset gridDs = null;
        logger.info(String.format("Loading file %s metadata", file.getAbsolutePath()));
        try {
            setTimeLatLon(file.getAbsoluteFile().toString());
            String location = file.getAbsolutePath();
            Set<NetcdfDataset.Enhance> enhanceMode = new HashSet<>();
            enhanceMode.add(NetcdfDataset.Enhance.ScaleMissing);
            gridDs = GridDataset.open(location, enhanceMode);
            for (Variable var : gridDs.getNetcdfDataset().getVariables()) {
                VariableDS vds = new VariableDS(var.getGroup(), var, true);
                if (vds.hasScaleOffset()) {
                    // File contains packed variables, unpack it
                    fileContainPackedVariables = true;
                    break;
                }
            }

            // Checking unpack config (Needed for TPAC)
            boolean unpackPackedVariables = goGoDuckConfig.getUnpack(profile);

            if (fileContainPackedVariables && unpackPackedVariables) {
                this.unpack = true;
            } else {
                this.unpack = false;
            }

            logger.info(String.format("File %s contain packed variables %s", file.getAbsolutePath(), fileContainPackedVariables));
            logger.info(String.format("Unpack variables config set to %s", unpackPackedVariables));
            logger.info(String.format("Unpacking file %s %s", file.getAbsolutePath(), this.unpack.booleanValue()));
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GoGoDuckException(e.getMessage(), e);
        } finally {
            close(gridDs, GridDataset.class);
        }
    }

    public List<String> getExtraParameters() throws Exception {
        if (extraParameters == null) {
            extraParameters = new ArrayList<>();
            for (String ncksParameter : goGoDuckConfig.getVariablesToInclude(profile)) {
                extraParameters.add(ncksParameter);
            }
        }
        return extraParameters;
    }

    protected List<Attribute> getGlobalAttributesToUpdate(Path outputFile) throws Exception{
        List<Attribute> newAttributeList = new ArrayList<>();
        String title = profile;
        NetcdfFileWriter nc = null;

        try {
            String location = outputFile.toAbsolutePath().toString();
            nc = NetcdfFileWriter.openExisting(location);

            try {
                title = nc.getNetcdfFile().findGlobalAttribute("title").getStringValue();

                // Remove time slice from title ('something_a, something_b, 2013-11-20T03:30:00Z' -> 'something_a, something_b')
                title = title.substring(0, title.lastIndexOf(","));
            }
            catch (Exception e) {
                // Don't fail because of this bullshit :)
                logger.warn("Could not find 'title' attribute in result file");
            }

            newAttributeList.add(new Attribute(goGoDuckConfig.getTitle(profile),
                    String.format("%s, %s, %s",
                            title,
                            subset.get("TIME").start,
                            subset.get("TIME").end)));

            newAttributeList.add(new Attribute(goGoDuckConfig.getLatitudeStart(profile), subset.get("LATITUDE").start));
            newAttributeList.add(new Attribute(goGoDuckConfig.getLatitudeEnd(profile), subset.get("LATITUDE").end));

            newAttributeList.add(new Attribute(goGoDuckConfig.getLongitudeStart(profile), subset.get("LONGITUDE").start));
            newAttributeList.add(new Attribute(goGoDuckConfig.getLongitudeEnd(profile), subset.get("LONGITUDE").end));

            List<String> timeStart = goGoDuckConfig.getTimeStart(profile);
            for (String timeStartEntry : timeStart) {
                newAttributeList.add(new Attribute(timeStartEntry, subset.get("TIME").start));
            }

            List<String> timeEnd = goGoDuckConfig.getTimeEnd(profile);
            for (String timeEndEntry : timeEnd) {
                newAttributeList.add(new Attribute(timeEndEntry, subset.get("TIME").end));
            }

        } catch (IOException e) {
            throw new GoGoDuckException(String.format("Failed updating metadata for file '%s': '%s'", outputFile, e.getMessage()));
        } finally {
            close(nc, NetcdfFileWriter.class);
            return newAttributeList;
        }
    }

    public NcksSubsetParameters getSubsetParameters() {
        if (subsetParameters == null) {
            subsetParameters = new NcksSubsetParameters();
            subsetParameters.put(latitude.getFullName(), subset.get("LATITUDE"));
            subsetParameters.put(longitude.getFullName(), subset.get("LONGITUDE"));
            subsetParameters.addTimeSubset(time.getFullName(), subset.get("TIME"));
        }
        return subsetParameters;
    }

    private void setTimeLatLon(String location) {
        GridDataset gridDs = null;

        try {
            gridDs = GridDataset.open(location);
            List grids = gridDs.getGrids();

            for (Object grid1 : grids) {
                GeoGrid grid = (GeoGrid) grid1;
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
            logger.error(e.getMessage(), e);
            throw new GoGoDuckException(e.getMessage());
        } finally {
            close(gridDs, GridDataset.class);
        }

        if (time == null || latitude == null || longitude == null) {
            throw new GoGoDuckException(String.format("Unable to retrieve time:%s latitude:%s longitude:%s", time, latitude, longitude));
        }
    }

    private void close(Object object, Class<?> cls) {
        try {
            Class noParams[] = {};
            if (object != null) {
                Method method = cls.getDeclaredMethod("close", noParams);
                method.invoke(object, null);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GoGoDuckException(e.getMessage(), e);
        }
    }

    public CoordinateAxis getTime() {
        return time;
    }
}
