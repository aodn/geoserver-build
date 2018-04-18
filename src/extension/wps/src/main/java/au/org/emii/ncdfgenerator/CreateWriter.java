package au.org.emii.ncdfgenerator;

import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFileWriter.Version;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

class CreateWriter implements ICreateWriter {
    // NetcdfFileWriteable is not an abstraction over a stream!. instead it insists on being a file...
    // Makes resource handling tedious to support multithreading and http concurrency.

    private final String tmpDir;

    CreateWriter(String tmpDir) {
        this.tmpDir = tmpDir;
    }

    private String getFilename() {
        long threadId = Thread.currentThread().getId();
        return tmpDir + "/tmpfile" + threadId + ".nc";
    }

    public final NetcdfFileWriter create() throws IOException {
        String filename = getFilename();
        Files.deleteIfExists(Paths.get(filename));
        return NetcdfFileWriter.createNew(Version.netcdf3, filename);
    }

    public final InputStream getStream() throws IOException {
        return new FileInputStream(getFilename());
    }
}


