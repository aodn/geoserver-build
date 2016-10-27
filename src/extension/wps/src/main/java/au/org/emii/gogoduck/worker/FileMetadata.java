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
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private Rectangle2D.Double spatialExtent, spatialSubset;
    private Rectangle2D intersection;

    public FileMetadata(String profile, IndexReader indexReader, String subset, GoGoDuckConfig goGoDuckConfig) {
        this.profile = profile;
        this.indexReader = indexReader;
        this.subset = new GoGoDuckSubsetParameters(subset);
        this.goGoDuckConfig = goGoDuckConfig;
    }

    public URIList getUriList() throws GoGoDuckException {
        return indexReader.getUriList(profile, goGoDuckConfig.getTimeField(), goGoDuckConfig.getSizeField(), goGoDuckConfig.getFileUrlField(), subset);
    }

    public GoGoDuckConfig getGoGoDuckConfig() {
        return goGoDuckConfig;
    }

    public boolean unpackNetcdf() {
        return unpack;
    }

    public boolean isTimeUnlimited() {
        return getTime().isUnlimited();
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
                VariableDS vds = (VariableDS) var;
                if (vds.hasScaleOffset()) {
                    // File contains packed variables, unpack it
                    fileContainPackedVariables = true;
                    break;
                }
            }

            // Checking unpack config (Needed for TPAC)
            boolean unpackPackedVariables = getGoGoDuckConfig().getUnpack(profile);

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
            throw new GoGoDuckException(String.format("Unable to load file %s", file.getName()), e);
        } finally {
            close(gridDs, GridDataset.class);
        }
    }

    public void validateSpatialSubset() {
        logger.info("Validating Spatial Subset");
        double latitudeStart = Double.valueOf(subset.get("LATITUDE").start);
        double latitudeEnd = Double.valueOf(subset.get("LATITUDE").end);
        double longitudeStart = Double.valueOf(subset.get("LONGITUDE").start);
        double longitudeEnd = Double.valueOf(subset.get("LONGITUDE").end);

        spatialExtent = new Rectangle2D.Double(longitude.getMinValue(), latitude.getMinValue(), longitude.getMaxValue() - longitude.getMinValue(), latitude.getMaxValue() - latitude.getMinValue());
        spatialSubset = new Rectangle2D.Double(longitudeStart, latitudeStart, longitudeEnd - longitudeStart, latitudeEnd - latitudeStart);
        intersection = spatialExtent.createIntersection(spatialSubset);

        logger.info(String.format("Spatial Extent: Latitude %s %s Longitude %s %s", latitude.getMinValue(), latitude.getMaxValue(), longitude.getMinValue(), longitude.getMaxValue()));
        logger.info(String.format("Spatial Subset: Latitude %s %s Longitude %s %s", latitudeStart, latitudeEnd, longitudeStart, longitudeEnd));
        logger.info(String.format("Intersection  : Latitude %s %s Longitude %s %s", intersection.getMinX(), intersection.getMaxX(), intersection.getMinY(), intersection.getMaxY()));

        // Point Time Series Validation
        if (latitudeStart == latitudeEnd && longitudeStart == longitudeEnd) {
            if (latitudeStart < latitude.getMinValue() || latitudeStart > latitude.getMaxValue() || longitudeStart < longitude.getMinValue() || longitudeStart > longitude.getMaxValue())
                throw new GoGoDuckException(String.format("Your point timeseries (Latitude:%s Longitude:%s ) is out of bounds (Latitude:%s %s Longitude:%s %s)",
                        subset.get("LATITUDE").start, subset.get("LONGITUDE").start, latitude.getMinValue(), latitude.getMaxValue(), longitude.getMinValue(), longitude.getMaxValue()));

        } else if (intersection.isEmpty())
            throw new GoGoDuckException(String.format("Your spatial subset (Latitude:%s %s Longitude:%s %s) is out of bounds (Latitude:%s %s Longitude:%s %s)",
                    subset.get("LATITUDE").start, subset.get("LATITUDE").end, subset.get("LONGITUDE").start, subset.get("LONGITUDE").end, latitude.getMinValue(), latitude.getMaxValue(), longitude.getMinValue(), longitude.getMaxValue()));

        logger.info("Spatial Subset validation completed successfully");
    }

    public List<String> getExtraParameters() throws Exception {
        if (extraParameters == null) {
            extraParameters = new ArrayList<>();
            for (String ncksParameter : getGoGoDuckConfig().getVariablesToInclude(getProfile())) {
                extraParameters.add(ncksParameter);
            }
        }
        return extraParameters;
    }

    protected List<Attribute> getGlobalAttributesToUpdate(Path outputFile) throws Exception {
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
            } catch (Exception e) {
                // Don't fail because of this bullshit :)
                logger.warn("Could not find 'title' attribute in result file");
            }

            newAttributeList.add(new Attribute(getGoGoDuckConfig().getTitle(profile),
                    String.format("%s, %s, %s",
                            title,
                            subset.get("TIME").start,
                            subset.get("TIME").end)));

            newAttributeList.add(new Attribute(getGoGoDuckConfig().getLatitudeStart(profile), Double.toString(intersection.getMinY())));
            newAttributeList.add(new Attribute(getGoGoDuckConfig().getLatitudeEnd(profile), Double.toString(intersection.getMaxY())));

            newAttributeList.add(new Attribute(getGoGoDuckConfig().getLongitudeStart(profile), Double.toString(intersection.getMinX())));
            newAttributeList.add(new Attribute(getGoGoDuckConfig().getLongitudeEnd(profile), Double.toString(intersection.getMaxX())));

            List<String> timeStart = getGoGoDuckConfig().getTimeStart(profile);
            for (String timeStartEntry : timeStart) {
                newAttributeList.add(new Attribute(timeStartEntry, subset.get("TIME").start));
            }

            List<String> timeEnd = getGoGoDuckConfig().getTimeEnd(profile);
            for (String timeEndEntry : timeEnd) {
                newAttributeList.add(new Attribute(timeEndEntry, subset.get("TIME").end));
            }

            return newAttributeList;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new GoGoDuckException(String.format("Failed updating metadata for file '%s': '%s'", outputFile, e.getMessage()));
        } finally {
            close(nc, NetcdfFileWriter.class);
        }
    }

    public NcksSubsetParameters getSubsetParameters() {
        if (subsetParameters == null) {
            subsetParameters = new NcksSubsetParameters();
            subsetParameters.put(getLatitude().getFullName(), getSubset().get("LATITUDE"));
            subsetParameters.put(getLongitude().getFullName(), getSubset().get("LONGITUDE"));
            subsetParameters.addTimeSubset(getTime().getFullName(), getSubset().get("TIME"));
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

    public CoordinateAxis getLatitude() {
        return latitude;
    }

    public CoordinateAxis getLongitude() {
        return longitude;
    }

    public GoGoDuckSubsetParameters getSubset() {
        return subset;
    }

    public String getProfile() {
        return profile;
    }
}
