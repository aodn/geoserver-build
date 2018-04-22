package au.org.emii.ncdfgenerator;

import org.apache.commons.io.FileUtils;
import org.geotools.data.DataStoreFinder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.jdbc.JDBCDataStore;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyIn;
import org.postgresql.copy.CopyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;

import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

class MockOutputterCounter implements IOutputFormatter {
    int count;

    public final void prepare(OutputStream os) {
        this.count = 0;
    }

    public final void write(String filename, InputStream is) {
        ++count;
    }

    public final void close() {
    }

    public int getCount() {
        return count;
    }
};

public class GenerationIT {

    static final String TMPDIR = "./tmp";
    private static final Logger logger = LoggerFactory.getLogger(GenerationIT.class);

    private static boolean dbIsPrepared = false;
    private Path outputDir;

    public GenerationIT() throws Exception {
        if (!dbIsPrepared) {
            logger.info("Prepare db");

            InputStream is = null;
            Connection conn = null;

            try {
                conn = getConn();

                is = getClass().getResourceAsStream("/scripts/anmn_ts.sql");
                conn.prepareStatement("drop schema if exists anmn_ts cascade").execute();
                importSQL(conn, is);

                is = getClass().getResourceAsStream("/scripts/anmn_nrs_ctd_profiles.sql");
                conn.prepareStatement("drop schema if exists anmn_nrs_ctd_profiles cascade").execute();
                importSQL(conn, is);

                is = getClass().getResourceAsStream("/scripts/soop_sst.sql");
                conn.prepareStatement("drop schema if exists soop_sst cascade").execute();
                importSQL(conn, is);
            }
            finally {
                if (is != null) {
                    is.close();
                }
                if (conn != null) {
                    conn.close();
                }
            }
            dbIsPrepared = true;
        }
    }

    private static void importSQL(Connection conn, InputStream is) throws Exception {

        // Supports Postgres data copy actions over jdbc
        final String regularStmts[] = {"SET", "CREATE", "COMMENT", "ALTER", "SELECT"};
        Statement st = conn.createStatement();
        CopyManager copyManager = ((PGConnection) conn).getCopyAPI();

        logger.info("Importing SQL");

        int ch = -1;
        while ((ch = is.read()) >= 0) {
            StringBuilder sb = new StringBuilder();

            // glob firstToken
            while (ch >= 0 && ch != ' ' && ch != '\n') {
                sb.append((char) ch);
                ch = is.read();
            }

            String firstTok = sb.toString();

            // empty line
            if (firstTok.equals("")) {
                continue;
            }
            // comment
            if (firstTok.equals("--")) {
                while (ch >= 0 && ch != '\n') {
                    ch = is.read();
                }
                continue;
            }
            // glob rest of the stmt
            while (ch >= 0 && ch != ';') {
                if (ch >= 0) {
                    sb.append((char) ch);
                }
                ch = is.read();
            }

            // sb.append((char)ch); // ;
            String sql = sb.toString();

            // statement type
            boolean regularStmt = false;
            for (String stmt : regularStmts) {
                if (stmt.equals(firstTok)) {
                    regularStmt = true;
                }
            }

            if (regularStmt) {
                logger.debug("query " + sql);
                st.execute(sql);
            }
            else if (firstTok.equals("COPY")) {
                logger.debug("Copying data");
                is.read(); // \n

                byte[] b = new byte[3];

                is.mark(3);
                is.read(b, 0, 3);

                if (!(b[0] == '\\' && b[1] == '.' && b[2] == '\n')) {
                    // non-empty copy statement - use copyManager to transfer content
                    // Note: the copyManager reads to eof and doesn't respect the \n\\. terminating sequence. so we have
                    // to scan ourselves
                    CopyIn copyIn = copyManager.copyIn(sql);
                    while (!(b[0] == '\n' && b[1] == '\\' && b[2] == '.')) {
                        copyIn.writeToCopy(b, 0, 1);
                        is.reset();
                        is.read(); // advance one byte
                        is.mark(3);
                        is.read(b, 0, 3);
                    }
                    copyIn.endCopy();
                }
            }
            else {
                throw new RuntimeException("Unknown SQL token during import '" + firstTok + "'");
            }
        }
    }

    private Connection getConn() throws Exception {
        Map<String, String> env = System.getenv();

        String opts[] = {"POSTGRES_USER", "POSTGRES_PASS", "POSTGRES_JDBC_URL", "POSTGRES_DB"};
        for (String opt : opts) {
            if (env.get(opt) == null) {
                throw new Exception("Environment var '" + opt + "' not set");
            }
        }

        Properties props = new Properties();
        props.setProperty("user", env.get("POSTGRES_USER"));
        props.setProperty("password", env.get("POSTGRES_PASS"));
        props.setProperty("database", env.get("POSTGRES_DB"));
        props.setProperty("ssl", "true");
        props.setProperty("sslfactory", "org.postgresql.ssl.NonValidatingFactory");
        props.setProperty("driver", "org.postgresql.Driver");

        String url = String.format("%s/%s", env.get("POSTGRES_JDBC_URL"), env.get("POSTGRES_DB"));

        return DriverManager.getConnection(url, props);
    }

    private NcdfEncoder getEncoder(URL config, String cqlFilter, String schema, IOutputFormatter outputGenerator) throws Exception {

        File is = new File(config.toURI());

        // decode definition
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
        Node node = document.getFirstChild();
        NcdfDefinition definition = new NcdfDefinitionXMLParser().parse(node);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("dbtype", "postgis");
        params.put("host", "localhost");
        params.put("port", 5432);
        params.put("schema", "public");
        params.put("database", System.getenv().get("POSTGRES_DB"));
        params.put("user", System.getenv().get("POSTGRES_USER"));
        params.put("passwd", System.getenv().get("POSTGRES_PASS"));

        JDBCDataStore jdbcDatastore = (JDBCDataStore) DataStoreFinder.getDataStore(params);

        NcdfEncoderBuilder encoderBuilder = new NcdfEncoderBuilder()
                .setDataStore(jdbcDatastore)
                .setTmpCreationDir(TMPDIR)
                .setDefinition(definition)
                .setFilterExpr(cqlFilter)
                .setSchema(schema)
                .setFetchSize(10);

        NcdfEncoder encoder = encoderBuilder.create();
        encoder.prepare(outputGenerator);
        return encoder;
    }

    private URL getAnmnConfig() {
        return getClass().getResource("/anmn_ts.xml");
    }

    private void consumeEncoderOutput(NcdfEncoder encoder) throws Exception {
        while (encoder.writeNext()) ;
        encoder.close();
    }

    @BeforeClass
    public static void beforeClass() {
        // called before constructor and must be static...
    }

    @Before
    public void setup() throws Exception {
        logger.info("delete and create TMPDIR");
        File file = new File(TMPDIR);
        FileUtils.deleteDirectory(file);
        file.mkdirs();
        logger.info("creating output dir");
        outputDir = Files.createTempDirectory("netcdfoutput-it-");
    }

    @After
    public void teardown() throws IOException {
        logger.info("removing output directory");
        deleteDirectory(outputDir);
    }

    @Test
    public void testNothing() throws Exception {
        // support devel testing constructor load
    }


    @Test
    public void testAnmnNrsCtdProfiles() throws Exception {
        URL config = this.getClass().getResource("/anmn_nrs_ctd_profiles.xml");
        String cql = "TIME < '2013-6-29T00:40:01Z' ";

        try (CdlOutputFormatter outputter = new CdlOutputFormatter(outputDir)) {
            NcdfEncoder encoder = getEncoder(config, cql, "anmn_nrs_ctd_profiles", outputter);
            consumeEncoderOutput(encoder);
        }

        assertDirsAreEqual(getPath("anmnnrsctdprofiles"), outputDir);
    }

    @Test
    public void testSoopSSTTrajectory() throws Exception {
        URL config = getClass().getResource("/soop_sst_trajectory.xml");
        String cql = "TIME >= '2013-06-25T09:00:00Z'AND TIME <= '2013-06-26T10:00:00Z'";

        try (CdlOutputFormatter outputter = new CdlOutputFormatter(outputDir)) {
            NcdfEncoder encoder = getEncoder(config, cql, "soop_sst_trajectory", outputter);
            consumeEncoderOutput(encoder);
        }

        assertDirsAreEqual(getPath("soopssttrajectory"), outputDir);
    }

    @Test
    public void testAnmnTs() throws Exception {
        String cql = "INTERSECTS(geom,POLYGON((113.3349609375 -33.091796875,113.3349609375 -30.982421875,117.1142578125 -30.982421875,117.1142578125 -33.091796875,113.3349609375 -33.091796875))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z' ";

        try (CdlOutputFormatter outputter = new CdlOutputFormatter(outputDir)) {
            NcdfEncoder encoder = getEncoder(getAnmnConfig(), cql, "anmn_ts", outputter);
            consumeEncoderOutput(encoder);
        }

        assertDirsAreEqual(getPath("anmnts"), outputDir);
    }

    @Test
    public void testNoCql() throws Exception {
        MockOutputterCounter outputter = new MockOutputterCounter();
        NcdfEncoder encoder = getEncoder(getAnmnConfig(), null, "anmn_ts", outputter);
        consumeEncoderOutput(encoder);
        assertEquals(11, outputter.getCount());
    }

    @Test
    public void testCqlWithValidSpatialTemporalSubset() throws Exception {
        String cql = "INTERSECTS(geom,POLYGON((113.3349609375 -33.091796875,113.3349609375 -30.982421875,117.1142578125 -30.982421875,117.1142578125 -33.091796875,113.3349609375 -33.091796875))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z'";
        MockOutputterCounter outputter = new MockOutputterCounter();
        NcdfEncoder encoder = getEncoder(getAnmnConfig(), cql, "anmn_ts", outputter);
        consumeEncoderOutput(encoder);
        assertEquals(11, outputter.getCount());
    }

    @Test
    public void testCqlWithNoDataInSpatialSubset() throws Exception {
        String cql = "INTERSECTS(geom,POLYGON((163.7841796875 -15.9970703125,163.7841796875 -3.0771484375,173.8037109375 -3.077148437499999,173.8037109375 -15.9970703125,163.7841796875 -15.9970703125))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z'";
        MockOutputterCounter outputter = new MockOutputterCounter();
        NcdfEncoder encoder = getEncoder(getAnmnConfig(), cql, "anmn_ts", outputter);
        consumeEncoderOutput(encoder);
        assertEquals(0, outputter.getCount());
    }

    @Test
    public void testCqlWithTemporalExtentOutOfAllowedRange() throws Exception {

        String cql = "INTERSECTS(geom,POLYGON((113.33 -33.09,113.33 -30.98,117.11 -30.98,117.11 -33.09,113.33 -33.09))) AND TIME >= '1949-01-01T23:00:00Z' AND TIME <= '1951-01-01T00:00:00Z'";
        MockOutputterCounter outputter = new MockOutputterCounter();
        NcdfEncoder encoder = getEncoder(getAnmnConfig(), cql, "anmn_ts", outputter);
        consumeEncoderOutput(encoder);
        assertEquals(0, outputter.getCount());
    }

    @Test(expected = CQLException.class)
    public void testCqlLongitudeOutsideAllowedRange() throws Exception {

        String cql = "INTERSECTS(geom,POLYGON((182 -33.09,113.33 -30.98,117.11 -30.98,117.11 -33.09,113.33 -33.09))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z'";

        MockOutputterCounter outputter = new MockOutputterCounter();
        NcdfEncoder encoder = getEncoder(getAnmnConfig(), cql, "anmn_ts", outputter);
        consumeEncoderOutput(encoder);
    }

    @Test(expected = CQLException.class)
    public void testCqlLatitudeOutsideAllowedRange() throws Exception {

        String cql = "INTERSECTS(geom,POLYGON((113.33 -95,113.33 -30.98,117.11 -30.98,117.11 -33.09,113.33 -33.09))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z'";

        MockOutputterCounter outputter = new MockOutputterCounter();
        NcdfEncoder encoder = getEncoder(getAnmnConfig(), cql, "anmn_ts", outputter);
        consumeEncoderOutput(encoder);
    }

    @Test
    public void testCqlWithFloatEqualityValid() throws Exception {
        String cql = "INTERSECTS(geom,POLYGON((113.3349609375 -33.091796875,113.3349609375 -30.982421875,117.1142578125 -30.982421875,117.1142578125 -33.091796875,113.3349609375 -33.091796875))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z' AND (NOMINAL_DEPTH = 125.0 OR NOMINAL_DEPTH = 150.0)";
        MockOutputterCounter outputter = new MockOutputterCounter();
        NcdfEncoder encoder = getEncoder(getAnmnConfig(), cql, "anmn_ts", outputter);
        consumeEncoderOutput(encoder);
        assertEquals(2, outputter.getCount());
    }

    @Test
    public void testCqlWithFloatInequalityValid() throws Exception {
        String cql = "INTERSECTS(geom,POLYGON((113.3349609375 -33.091796875,113.3349609375 -30.982421875,117.1142578125 -30.982421875,117.1142578125 -33.091796875,113.3349609375 -33.091796875))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z' AND NOMINAL_DEPTH <> 125.0";
        MockOutputterCounter outputter = new MockOutputterCounter();
        NcdfEncoder encoder = getEncoder(getAnmnConfig(), cql, "anmn_ts", outputter);
        consumeEncoderOutput(encoder);
        assertEquals(10, outputter.getCount());
    }

    @Test
    public void testCqlWithStringEqualityValid() throws Exception {
        // QC flag is represented as a string in the db, so must quote
        String cql = "INTERSECTS(geom,POLYGON((113.3349609375 -33.091796875,113.3349609375 -30.982421875,117.1142578125 -30.982421875,117.1142578125 -33.091796875,113.3349609375 -33.091796875))) AND TIME >= '2015-01-13T23:00:00Z' AND TIME <= '2015-04-14T00:00:00Z' AND TEMP_quality_control = '4'";
        MockOutputterCounter outputter = new MockOutputterCounter();
        NcdfEncoder encoder = getEncoder(getAnmnConfig(), cql, "anmn_ts", outputter);
        consumeEncoderOutput(encoder);
        assertEquals(10, outputter.getCount());
        // TODO, should check that we only include obs with temp_qc = 4 etc, not just that instances are constrained.
    }

    class CdlOutputFormatter implements IOutputFormatter, AutoCloseable {
        private final Path outputDir;

        public CdlOutputFormatter(Path outputDir) {
            this.outputDir = outputDir;
        }

        public final void write(String filename, InputStream is) throws IOException {
            Path netcdfOutputFile = outputDir.resolve(filename);
            Path cdlOutputFile = outputDir.resolve(filename.replaceFirst(".nc$", ".cdl"));

            try {
                Files.copy(is, netcdfOutputFile);

                try (NetcdfFile expectedFile = NetcdfFile.open(netcdfOutputFile.toString())) {
                    StringWriter outputWriter = new StringWriter();
                    NCdumpW.print(expectedFile, outputWriter, NCdumpW.WantValues.all, false, false, null, null);
                    String cdl = outputWriter.toString().replaceFirst("netcdf .*/([^/]*) ", "netcdf $1 "); // remove temporary directory details
                    Files.write(cdlOutputFile, cdl.getBytes());
                }
            } finally {
                Files.deleteIfExists(netcdfOutputFile);
            }
        }

        public final void close() throws IOException {
        }

    }

    private void deleteDirectory(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void assertDirsAreEqual(final Path expected, final Path generated)
        throws IOException {

        // Checks parameters
        assertTrue("Generated Folder doesn't exist: " + generated, Files.exists(generated));
        assertTrue("Generated is not a folder?!?!: " + generated, Files.isDirectory(generated));

        assertTrue("Expected Folder doesn't exist: " + expected, Files.exists(expected));
        assertTrue("Expected is not a folder?!?!: " + expected, Files.isDirectory(expected));

        Files.walkFileTree(expected, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                                                     BasicFileAttributes attrs)
                throws IOException {
                FileVisitResult result = super.preVisitDirectory(dir, attrs);

                // get the relative file name from path "expected"
                Path relativize = expected.relativize(dir);
                // construct the path for the counterpart file in "generated"
                File otherDir = generated.resolve(relativize).toFile();
                logger.info("=== preVisitDirectory === compare " + dir + " to " + otherDir);
                assertEquals("Folders doesn't contain same file!?!?",
                    Arrays.toString(dir.toFile().list()),
                    Arrays.toString(otherDir.list()));
                return result;
            }
            @Override
            public FileVisitResult visitFile(Path file,
                                             BasicFileAttributes attrs)
                throws IOException {
                FileVisitResult result = super.visitFile(file, attrs);

                // get the relative file name from path "expected"
                Path relativize = expected.relativize(file);
                // construct the path for the counterpart file in "generated"
                File fileInOther = generated.resolve(relativize).toFile();
                logger.info("=== comparing: " + file + " to " + fileInOther);
                String expectedContents = FileUtils.readFileToString(file.toFile());
                String generatedContents = FileUtils.readFileToString(fileInOther);
                assertEquals("("+fileInOther+")  csv standard doesn't match expected ("+file+")!", expectedContents, generatedContents);
                return result;
            }
        });
    }

    private Path getPath(String resource) throws URISyntaxException {
        return Paths.get(this.getClass().getResource(resource).toURI());
    }

}
