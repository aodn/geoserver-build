package au.org.emii.gogoduck.worker;

import au.org.emii.gogoduck.exception.GoGoDuckException;
import au.org.emii.utils.GoGoDuckConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GoGoDuckModule {
    private static final Logger logger = LoggerFactory.getLogger(GoGoDuckModule.class);

    @Autowired
    ServletContext context;

    private String profile;
    private GoGoDuckSubsetParameters subset;
    private IndexReader indexReader;
    private GoGoDuckConfig goGoDuckConfig;

    private Boolean containsPackedVariable = null;
    private Boolean timeUnlimited = null;
    private List<String> ncksExtraParameters = null;
    private NcksSubsetParameters ncksSubsetParameters = null;
    private CoordinateAxis time = null, latitude = null, longitude = null;

    public GoGoDuckModule() {

    }

    public GoGoDuckModule(String profile, IndexReader indexReader, String subset, GoGoDuckConfig goGoDuckConfig) {
        this.profile = profile;
        this.indexReader = indexReader;
        this.subset = new GoGoDuckSubsetParameters(subset);
        this.goGoDuckConfig = goGoDuckConfig;
    }

    public URIList getUriList() throws GoGoDuckException {
        return indexReader.getUriList(profile, goGoDuckConfig.getTimeField(), goGoDuckConfig.getFileUrlField(), subset);
    }

    public boolean hasPackedVariables() {
        return containsPackedVariable;
    }

    public boolean isTimeUnlimited() {
        return timeUnlimited;
    }

    public void loadFileMetadata(File file) {
        boolean containsPackedVariable = false;
        GridDataset gridDs = null;

        try {
            setNcksSubsetParameters(file.getAbsoluteFile().toString());
            setNcksExtraParameters();
            this.timeUnlimited = time.isUnlimited();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GoGoDuckException(e.getMessage(), e);
        }

        try {
            if (goGoDuckConfig.getUnpackNetcdf(profile)) {
                // Checking the config (Needed for TPAC)
                this.containsPackedVariable = true;
            } else {
                String location = file.getAbsolutePath();
                Set<NetcdfDataset.Enhance> enhanceMode = new HashSet<>();
                enhanceMode.add(NetcdfDataset.Enhance.ScaleMissing);
                gridDs = GridDataset.open(location, enhanceMode);
                for (Variable var : gridDs.getNetcdfDataset().getVariables()) {
                    VariableDS vds = new VariableDS(var.getGroup(), var, true);
                    if (vds.hasScaleOffset()) {
                        containsPackedVariable = true;
                        break;
                    }
                }
                this.containsPackedVariable = containsPackedVariable;
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GoGoDuckException(e.getMessage(), e);
        } finally {
            try {
                if (gridDs != null) {
                    gridDs.close();
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
                throw new GoGoDuckException(e.getMessage(), e);
            }
        }
    }

    private void setNcksExtraParameters() throws Exception {
        ncksExtraParameters = new ArrayList<>();
        for (String ncksParameter : goGoDuckConfig.getNcksParameters(profile)) {
            ncksExtraParameters.add(ncksParameter);
        }
    }

    public List<String> getNcksExtraParameters() {
        return ncksExtraParameters;
    }

    protected List<Attribute> getGlobalAttributesToUpdate(NetcdfFile nc) throws Exception{
        List<Attribute> newAttributeList = new ArrayList<>();

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

        newAttributeList.add(new Attribute(goGoDuckConfig.getLatitudeStart(profile), subset.get("LATITUDE").start));
        newAttributeList.add(new Attribute(goGoDuckConfig.getLatitudeEnd(profile), subset.get("LATITUDE").end));

        newAttributeList.add(new Attribute(goGoDuckConfig.getLongitudeStart(profile), subset.get("LONGITUDE").start));
        newAttributeList.add(new Attribute(goGoDuckConfig.getLongitudeEnd(profile), subset.get("LONGITUDE").end));

        newAttributeList.add(new Attribute(goGoDuckConfig.getTimeStart(profile), subset.get("TIME").start));
        newAttributeList.add(new Attribute(goGoDuckConfig.getTimeEnd(profile), subset.get("TIME").end));

        return newAttributeList;
    }

    public final void updateMetadata(Path outputFile) throws Exception {
        try {
            String location = outputFile.toAbsolutePath().toString();
            NetcdfFileWriter nc = NetcdfFileWriter.openExisting(location);

            nc.setRedefineMode(true);
            for (Attribute newAttr : getGlobalAttributesToUpdate(nc.getNetcdfFile())) {
                nc.addGroupAttribute(null, newAttr);
            }
            nc.setRedefineMode(false);
            nc.close();
        } catch (IOException e) {
            throw new GoGoDuckException(String.format("Failed updating metadata for file '%s': '%s'", outputFile, e.getMessage()));
        }
    }

    public NcksSubsetParameters getNcksSubsetParameters() {
        return ncksSubsetParameters;
    }

    public NcksSubsetParameters setNcksSubsetParameters(String location) {
        GridDataset gridDs;

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
            gridDs.close();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new GoGoDuckException(e.getMessage());
        }

        if (time == null || latitude == null || longitude == null) {
            throw new GoGoDuckException(String.format("Unable to retrieve time:%s latitude:%s longitude:%s", time, latitude, longitude));
        }

        ncksSubsetParameters = new NcksSubsetParameters();
        ncksSubsetParameters.put(latitude.getFullName(), subset.get("LATITUDE"));
        ncksSubsetParameters.put(longitude.getFullName(), subset.get("LONGITUDE"));
        ncksSubsetParameters.addTimeSubset(time.getFullName(), subset.get("TIME"));
        return ncksSubsetParameters;
    }
}
