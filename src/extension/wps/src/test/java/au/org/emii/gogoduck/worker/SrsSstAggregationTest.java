package au.org.emii.gogoduck.worker;

import au.org.emii.utils.GoGoDuckConfig;
import org.geoserver.catalog.Catalog;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;


public class SrsSstAggregationTest extends BaseAggregationTest {

    private static final Logger logger = LoggerFactory.getLogger(SrsSstAggregationTest.class);

    private GoGoDuck ggd = null;
    private File outputFile = null;

    private String layer = "srs_sst_l3s_1d_ngt_gridded_url";
    private String subset = "TIME,2015-09-01T15:20:00.000Z,2015-09-03T15:20:00.000Z;LATITUDE,-14.4150390625,-11.7783203125;LONGITUDE,148.8427734375,151.6552734375";
    private String expectedFilePath = "src/test/resources/aggregation-files/srs_sst_l3s_1d_ngt_gridded_url/expected-result.nc";
    private String resourceDir = "src/test/resources/aggregation-files/srs_sst_l3s_1d_ngt_gridded_url/";
    private String configFile = "src/test/resources/aggregation-files/srs_sst_l3s_1d_ngt_gridded_url/gogoduck.xml";
    private String sourceNetCdf[] = {"IMOS/SRS/SST/ghrsst/L3S-1d/ngt/2015/20150902152000-ABOM-L3S_GHRSST-SSTskin-AVHRR_D-1d_night.nc", "IMOS/SRS/SST/ghrsst/L3S-1d/ngt/2015/20150903152000-ABOM-L3S_GHRSST-SSTskin-AVHRR_D-1d_night.nc"};

    @Before
    public void setUp() throws Exception {
        IndexReader indexReader = mock(IndexReader.class);
        Catalog catalog = mock(Catalog.class);
        outputFile = new File(outputFilePath);

        URIList uriList = new URIList();
        for (String netCdf : sourceNetCdf) {
            uriList.add(new URI(netCdf));
        }

        GoGoDuckConfig goGoDuckConfig = new GoGoDuckConfig(new File(resourceDir), null);
        goGoDuckConfig = spy(goGoDuckConfig);
        doReturn(configFile).when(goGoDuckConfig).getLayerConfigPath(anyString(), anyString());
        doReturn(configFile).when(goGoDuckConfig).getConfigFilePath(anyString());

        FileMetadata fileMetadata = new FileMetadata(layer, indexReader, subset, goGoDuckConfig);
        fileMetadata = spy(fileMetadata);
        doReturn(uriList).when(fileMetadata).getUriList();

        ggd = spy(new GoGoDuck(catalog, layer, subset, outputFilePath, format, goGoDuckConfig));
        doReturn(fileMetadata).when(ggd).getFileMetadata();
        doNothing().when(ggd).createSymbolicLink(any(File.class), any(Path.class));
    }

    @Test
    public void aggregationTest() throws Exception {

        Path outputPath = ggd.run();

        NetcdfFile ncFile1 = NetcdfFile.open(outputPath.toAbsolutePath().toString());
        NetcdfFile ncFile2 = NetcdfFile.open(expectedFilePath);

        List<Dimension> dimensions1 = ncFile1.getDimensions();
        List<Dimension> dimensions2 = ncFile2.getDimensions();
        verifyDimensions(dimensions1, dimensions2);

        List<Variable> variables1 = ncFile1.getVariables();
        List<Variable> variables2 = ncFile2.getVariables();
        verifyVariables(variables1, variables2);

        List<Attribute> globalAttributes1 = ncFile1.getGlobalAttributes();
        List<Attribute> globalAttributes2 = ncFile2.getGlobalAttributes();
        verifyAttributes(globalAttributes1, globalAttributes2);
    }

    @After
    public void tearDown() {
        if (outputFile != null) {
            outputFile.delete();
        }
    }
}
