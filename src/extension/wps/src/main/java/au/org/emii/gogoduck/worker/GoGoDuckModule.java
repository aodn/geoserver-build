package au.org.emii.gogoduck.worker;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;

public class GoGoDuckModule {
    private static final Logger logger = LoggerFactory.getLogger(GoGoDuckModule.class);

    // TODO Should not be hard coded
    private static final String TIME_FIELD = "time";
    private static final String URL_FIELD = "file_url";

    protected String profile = null;
    protected SubsetParameters subset = null;
    protected UserLog userLog = null;
    protected IndexReader indexReader = null;

    public GoGoDuckModule() {}

    public void init(String profile, IndexReader indexReader, String subset, UserLog userLog) {
        this.profile = profile;
        this.indexReader = indexReader;
        this.subset = new SubsetParameters(subset);
        this.userLog = userLog;
    }

    public URIList getUriList() throws GoGoDuckException {
        return indexReader.getUriList(profile, TIME_FIELD, URL_FIELD, subset);
    }

    public void postProcess(File file) {
        return;
    }

    public List<String> ncksExtraParameters() {
        List<String> ncksExtraParameters = new ArrayList<String>();
        return ncksExtraParameters;
    }

    protected List<Attribute> getGlobalAttributesToUpdate(NetcdfFile nc) {
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

    public SubsetParameters getSubsetParameters() {
        // Remove time parameter as we don't need to subset on it, we already
        // have only files that are in the correct time range
        SubsetParameters subsetParametersNoTime = new SubsetParameters(subset);
        subsetParametersNoTime.remove("TIME");
        return subsetParametersNoTime;
    }

    public static GoGoDuckModule newInstance(String profile, IndexReader indexReader, String subset, UserLog userLog) {
        String thisPackage = GoGoDuckModule.class.getPackage().getName();
        String classToInstantiate = String.format("GoGoDuckModule_%s", profile);

        GoGoDuckModule module = null;
        while (null == module && !classToInstantiate.isEmpty()) {
            logger.debug(String.format("Trying class '%s.%s'", thisPackage, classToInstantiate));
            try {
                Class classz = Class.forName(String.format("%s.%s", thisPackage, classToInstantiate));
                module = (GoGoDuckModule) classz.newInstance();
                module.init(profile, indexReader, subset, userLog);
                logger.info(String.format("Using class '%s.%s'", thisPackage, classToInstantiate));
                return module;
            }
            catch (Exception e) {
                logger.debug(String.format("Could not find class for '%s.%s'", thisPackage, classToInstantiate));
            }
            classToInstantiate = nextProfile(classToInstantiate);
        }

        throw new GoGoDuckException(String.format("Error initializing class for profile '%s'", profile));
    }

    /* Finds the correct profile to run for the given layer, starts with:
    GoGoDuckModule_acorn_hourly_avg_sag_nonqc_timeseries_url
    GoGoDuckModule_acorn_hourly_avg_sag_nonqc_timeseries
    GoGoDuckModule_acorn_hourly_avg_sag_nonqc
    GoGoDuckModule_acorn_hourly_avg_sag
    GoGoDuckModule_acorn_hourly_avg
    GoGoDuckModule_acorn_hourly
    GoGoDuckModule_acorn
    GoGoDuckModule */
    private static String nextProfile(String profile) {
        return profile.substring(0, profile.lastIndexOf("_"));
    }
}
